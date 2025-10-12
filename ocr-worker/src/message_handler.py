"""Message handling logic"""

import json
import logging
from typing import Union, Optional, Any
from pika.adapters.blocking_connection import BlockingChannel

from .ocr_service import OcrService
from .rabbitmq_client import RabbitMQClient

logger = logging.getLogger(__name__)


class MessageHandler:
    """Handles incoming messages and coordinates processing"""

    def __init__(self, rabbitmq_client: RabbitMQClient, ocr_service: OcrService):
        self.rabbitmq_client = rabbitmq_client
        self.ocr_service = ocr_service

    def handle_message(
        self, channel: BlockingChannel, method: Any, properties: Any, body: bytes
    ) -> None:
        """
        Process incoming OCR requests

        This is the callback function for RabbitMQ message consumption
        """
        logger.info(f"Received message with routing key: {method.routing_key}")

        # Log message metadata if available
        if properties.headers:
            logger.info(f"Message headers: {properties.headers}")
        if properties.message_id:
            logger.info(f"Message ID: {properties.message_id}")

        try:
            # Parse message
            message = self._parse_message(body)
            logger.info(f"Message content: {message}")

            # Validate and process
            if isinstance(message, dict):
                self._process_document(message)

                # Acknowledge successful processing
                channel.basic_ack(delivery_tag=method.delivery_tag)
                logger.info("Message acknowledged")
            else:
                logger.warning("Message is not a valid DocumentDto")
                channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

        except json.JSONDecodeError:
            logger.error("Failed to parse JSON message")
            # Reject and don't requeue malformed messages
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        except Exception as e:
            logger.error(f"Error processing message: {e}", exc_info=True)
            # Reject and requeue - let another worker try
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

    def _parse_message(self, body: bytes) -> Union[dict, str]:
        """Parse message body to dict or string"""
        try:
            return json.loads(body)
        except json.JSONDecodeError:
            return body.decode()

    def _process_document(self, document: dict) -> None:
        """Process the document and send response"""
        doc_id = document.get("id", "unknown")
        
        # TODO: Get file_path from MinIO using document ID
        # For now, use a placeholder or skip actual OCR processing
        file_path = document.get("file_path")
        
        if not doc_id:
            logger.error("Missing required field: id")
            self._send_response(doc_id, "failed", error="Missing required field: id")
            return

        # TODO: Remove this check once MinIO integration is complete
        if not file_path:
            logger.warning(f"No file_path provided for document {doc_id}. Skipping OCR processing for now.")
            # Send a mock successful response for demo purposes
            self._send_response(
                doc_id, 
                "completed", 
                ocr_text=f"Mock OCR text for document: {document.get('title', 'Unknown Title')}"
            )
            return

        try:
            # Process with OCR service
            ocr_text = self.ocr_service.process_document(doc_id, file_path)

            # Send success response
            self._send_response(doc_id, "completed", ocr_text=ocr_text)
            logger.info(f"Completed processing document {doc_id}")

        except Exception as e:
            # Send failure response
            self._send_response(doc_id, "failed", error=str(e))
            logger.error(f"Failed processing document {doc_id}: {e}")
            raise

    def _send_response(
        self,
        document_id: str,
        status: str,
        ocr_text: Optional[str] = None,
        ocr_text_path: Optional[str] = None,
        error: Optional[str] = None,
    ) -> None:
        """Send processing result back to REST API"""
        response: dict = {
            "document_id": document_id,
            "status": status,
            "worker": "ocr",
        }

        if ocr_text is not None:
            response["ocr_text"] = ocr_text
        if ocr_text_path is not None:
            response["ocr_text_path"] = ocr_text_path
        if error is not None:
            response["error"] = error

        self.rabbitmq_client.publish_response(response)
