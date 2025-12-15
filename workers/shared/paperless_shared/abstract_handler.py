"""Abstract handler for processing messages"""

import json
import logging
from abc import ABC, abstractmethod
from typing import Any, Generic, TypeVar

from pika.adapters.blocking_connection import BlockingChannel

from .models import AbstractRequestDto, AbstractResponseDto

TRequest = TypeVar("TRequest", bound=AbstractRequestDto)
TResponse = TypeVar("TResponse", bound=AbstractResponseDto)

logger = logging.getLogger(__name__)


class AbstractMessageHandler(ABC, Generic[TRequest, TResponse]):
    """Abstract base class for message handlers"""

    def __init__(self, rabbitmq_client: Any, minio_client: Any, config: Any):
        """Initialize handler with clients and config"""
        self.rabbitmq = rabbitmq_client
        self.minio = minio_client
        self.config = config

    @abstractmethod
    def _process_message(self, request: TRequest) -> TResponse | None:
        """
        Process the message and return a response.
        Must be implemented by subclasses.
        """
        pass

    @abstractmethod
    def _publish_response(self, response: TResponse) -> None:
        """
        Publish the response.
        Must be implemented by subclasses.
        """
        pass

    def _create_error_response(self, document_id: str, error_message: str) -> TResponse:
        """Create an error response"""
        return {  # type: ignore[return-value]
            "document_id": document_id,
            "status": "failed",
            "worker": self.config.WORKER_NAME,
            "error": error_message,
        }

    def handle_message(
        self,
        ch: BlockingChannel,
        method: Any,
        properties: Any,
        body: bytes,
    ) -> None:
        """
        Callback for RabbitMQ message consumption.

        Args:
            ch: Channel
            method: Method
            properties: Message properties
            body: Message body
        """
        try:
            # Parse message
            message = json.loads(body.decode())
            logger.info(f"Received message: {message}")

            # Process message
            response = self._process_message(message)  # type: ignore[arg-type]

            if response:
                # Publish response
                self._publish_response(response)

            # Acknowledge message
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"Message processed successfully: {message.get('document_id')}")

        except Exception as e:
            logger.error(f"Error processing message: {e}", exc_info=True)
            # Reject and requeue the message
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
