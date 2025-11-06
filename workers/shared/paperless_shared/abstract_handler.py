"""Abstract base handler for message processing"""

import json
import logging
from abc import ABC, abstractmethod
from typing import Any, Dict, Optional

from pika.adapters.blocking_connection import BlockingChannel

logger = logging.getLogger(__name__)


class AbstractMessageHandler(ABC):
    """
    Base class for worker message handlers.
    
    Subclasses must implement:
    - _process_message: Process the incoming message and return a response
    """

    def __init__(self, rabbitmq_client, minio_client, config):
        self.rabbitmq = rabbitmq_client
        self.minio = minio_client
        self.config = config

    def handle_message(
        self, channel: BlockingChannel, method: Any, properties: Any, body: bytes
    ) -> None:
        """
        Generic message handling with error handling and acknowledgment.
        Calls worker-specific _process_message for actual processing.
        """
        logger.info(f"Received message with routing key: {method.routing_key}")

        try:
            # Parse JSON message
            message = json.loads(body)
            
            if not isinstance(message, dict):
                logger.warning("Message is not a valid dict")
                channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
                return

            logger.info(f"Processing message for document: {message.get('document_id', 'unknown')}")
            
            # Call worker-specific processing
            response = self._process_message(message)
            
            # Publish response if one was returned
            if response:
                self.rabbitmq.publish_response(response)
                logger.info(f"Published response for document {response.get('document_id')}")
            
            # Acknowledge message
            channel.basic_ack(delivery_tag=method.delivery_tag)
            logger.info("Message acknowledged")

        except json.JSONDecodeError:
            logger.error("Failed to parse JSON message")
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        except Exception as e:
            logger.error(f"Error processing message: {e}", exc_info=True)
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    @abstractmethod
    def _process_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """
        Process the message and return a response dict (or None).
        
        Args:
            message: Parsed message body as dict
            
        Returns:
            Response dict to publish, or None if no response needed
            
        Raises:
            Exception: Any processing errors (will be caught and logged)
        """
        raise NotImplementedError("Subclasses must implement _process_message")

    def _create_error_response(
        self, document_id: str, error_message: str
    ) -> Dict[str, Any]:
        """Helper to create a standard error response"""
        return {
            "document_id": document_id,
            "status": "failed",
            "worker": self.config.WORKER_NAME,
            "error": error_message,
        }
