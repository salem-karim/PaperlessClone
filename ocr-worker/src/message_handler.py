"""Message handling logic"""

import json
import logging
from typing import Any

from pika.adapters.blocking_connection import BlockingChannel

from .config import Config
from .ocr_service import OcrService
from .rabbitmq_client import RabbitMQClient
from .minio_client import MinioClient
from .models import OcrRequestDto, OcrResponseDto
from .genAi_service import GenAIService

logger = logging.getLogger(__name__)

# Size threshold: Send text directly if under 100KB, otherwise store in MinIO
OCR_TEXT_SIZE_THRESHOLD = Config.OCR_TEXT_SIZE_THRESHOLD


class MessageHandler:
    """Handles incoming messages and coordinates processing"""

    def __init__(
        self,
        rabbitmq_client: RabbitMQClient,
        ocr_service: OcrService,
        minio_client: MinioClient,
        genAi_service: GenAIService,
    ):
        self.rabbitmq_client = rabbitmq_client
        self.ocr_service = ocr_service
        self.minio_client = minio_client
        self.genAi_service = genAi_service

    def handle_message(
        self, channel: BlockingChannel, method: Any, properties: Any, body: bytes
    ) -> None:
        logger.info(f"Received message with routing key: {method.routing_key}")

        try:
            message = self._parse_message(body)

            if isinstance(message, dict):
                request: OcrRequestDto = message  # type: ignore[assignment]
                logger.info(
                    f"Received OCR request for document: {request.get('document_id')}"
                )
                self._process_document(request)
                channel.basic_ack(delivery_tag=method.delivery_tag)
                logger.info("Message acknowledged")
            else:
                logger.warning("Message is not a valid OcrRequestDto")
                channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

        except json.JSONDecodeError:
            logger.error("Failed to parse JSON message")
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        except Exception as e:
            logger.error(f"Error processing message: {e}", exc_info=True)
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    def _parse_message(self, body: bytes) -> OcrRequestDto | str:
        """Parse message body to dict or string"""
        try:
            return json.loads(body)
        except json.JSONDecodeError:
            return body.decode()

    def _process_document(self, request: OcrRequestDto) -> None:
        """Process the document and send response"""
        doc_id = request.get("document_id", "unknown")
        file_bucket = request.get("file_bucket")
        file_object_key = request.get("file_object_key")
        original_filename = request.get("original_filename", "unknown")
        content_type = request.get("content_type", "application/octet-stream")

        # Validate required fields
        if not doc_id:
            error_msg = "Missing required field: document_id"
            logger.error(error_msg)
            self._send_response(doc_id, "failed", error=error_msg)
            return

        if not file_bucket or not file_object_key:
            error_msg = "Missing required fields: file_bucket or file_object_key"
            logger.error(error_msg)
            self._send_response(doc_id, "failed", error=error_msg)
            return

        try:
            # Download file from MinIO
            logger.info(
                f"Downloading document {doc_id} from MinIO: "
                f"{file_bucket}/{file_object_key}"
            )
            file_data = self.minio_client.download_file(file_bucket, file_object_key)

            # Extract text using OCR
            logger.info(f"Starting OCR processing for document {doc_id}")
            ocr_text = self.ocr_service.process_document(
                file_data, content_type, original_filename
            )

            if not ocr_text or ocr_text.strip() == "":
                logger.warning(f"No text extracted from document {doc_id}")
                ocr_text = "[No text could be extracted from this document]"

            logger.info(f"Generating summary for document {doc_id} using Google Gemini")
            summary_text = self.genai_service.summarize_text(ocr_text)

            # Determine if text should be stored inline or in MinIO
            ocr_text_size = len(ocr_text.encode("utf-8"))

            if ocr_text_size < OCR_TEXT_SIZE_THRESHOLD:
                # Small text: send directly in message
                logger.info(
                    f"OCR text size ({ocr_text_size} bytes) below threshold "
                    f"({OCR_TEXT_SIZE_THRESHOLD} bytes). Sending inline."
                )
                self._send_response(doc_id, "completed", ocr_text=ocr_text, summary_text=summary_text)
            else:
                # Large text: store in MinIO and send reference
                logger.info(
                    f"OCR text size ({ocr_text_size} bytes) above threshold "
                    f"({OCR_TEXT_SIZE_THRESHOLD} bytes). Storing in MinIO."
                )

                ocr_bucket = self.minio_client.ocr_results_bucket
                ocr_object_key = f"ocr/{doc_id}.txt"

                self.minio_client.upload_text(ocr_bucket, ocr_object_key, ocr_text)

                self._send_response(
                    doc_id, "completed", ocr_text_object_key=ocr_object_key, summary_text=summary_text
                )

            logger.info(f"Completed OCR processing for document {doc_id}")

        except ValueError as e:
            # Unsupported file type
            error_msg = f"Unsupported file type: {str(e)}"
            logger.error(f"Failed processing document {doc_id}: {error_msg}")
            self._send_response(doc_id, "failed", error=error_msg)

        except Exception as e:
            # Generic error
            error_msg = f"OCR processing failed: {str(e)}"
            logger.error(f"Failed processing document {doc_id}: {error_msg}")
            self._send_response(doc_id, "failed", error=error_msg)
            raise

    def _send_response(
        self,
        document_id: str,
        status: str,
        ocr_text: str | None = None,
        ocr_text_object_key: str | None = None,
        summary_text: str | None = None,
        error: str | None = None,
    ) -> None:
        """Send processing result back to REST API"""
        response: OcrResponseDto = {
            "document_id": document_id,
            "status": status,
            "worker": "ocr",
        }

        if ocr_text is not None:
            response["ocr_text"] = ocr_text
        if ocr_text_object_key is not None:
            response["ocr_text_object_key"] = ocr_text_object_key
        if summary_text is not None:
            response["summary_text"] = summary_text
        if error is not None:
            response["error"] = error

        self.rabbitmq_client.publish_response(response)
        logger.info(
            f"Sent OCR response for document {document_id} with status: {status}"
        )
