"""RabbitMQ client for connection and messaging"""

import json
from logging import getLogger
from typing import Callable, Optional

import pika
from pika.adapters.blocking_connection import BlockingChannel, BlockingConnection

from .config import Config

logger = getLogger(__name__)


class RabbitMQClient:
    """Handles RabbitMQ connection and messaging"""

    def __init__(self):
        self.connection: Optional[BlockingConnection] = None
        self.channel: Optional[BlockingChannel] = None

    def connect(self) -> None:
        """Establish connection to RabbitMQ"""
        logger.info(f"Connecting to RabbitMQ at {Config.RABBITMQ_HOST}...")

        credentials = pika.PlainCredentials(
            Config.RABBITMQ_USER, Config.RABBITMQ_PASSWORD
        )

        parameters = pika.ConnectionParameters(
            host=Config.RABBITMQ_HOST,
            port=Config.RABBITMQ_PORT,
            credentials=credentials,
        )

        self.connection = pika.BlockingConnection(parameters)
        self.channel = self.connection.channel()
        logger.info("Connected to RabbitMQ successfully")

        # Declare exchange
        self.channel.exchange_declare(
            exchange=Config.EXCHANGE, exchange_type="topic", durable=True
        )

        # Declare Request Queue
        self.channel.queue_declare(queue=Config.QUEUE, durable=True)
        self.channel.queue_bind(
            exchange=Config.EXCHANGE,
            queue=Config.QUEUE,
            routing_key=Config.ROUTING_KEY_REQUEST,
        )

        # Declare Response Queue
        self.channel.queue_declare(queue=Config.RESPONSE_QUEUE, durable=True)
        self.channel.queue_bind(
            exchange=Config.EXCHANGE,
            queue=Config.RESPONSE_QUEUE,
            routing_key=Config.ROUTING_KEY_RESPONSE,
        )

        logger.info(
            f"Declared exchange '{Config.EXCHANGE}' and queues: '{Config.QUEUE}', '{Config.RESPONSE_QUEUE}'"
        )

    def start_consuming(self, callback: Callable) -> None:
        """Start consuming messages with the provided callback"""
        if self.channel is None:
            logger.error("Not connected. Call connect() first.")
            raise RuntimeError("Not connected. Call connect() first.")

        # Configure QoS - process one message at a time
        self.channel.basic_qos(prefetch_count=1)

        # Start consuming
        self.channel.basic_consume(queue=Config.QUEUE, on_message_callback=callback)

        logger.info(
            f"Waiting for messages on queue '{Config.QUEUE}'. Press CTRL+C to exit"
        )

        try:
            self.channel.start_consuming()
        except KeyboardInterrupt:
            logger.info("Received shutdown signal, stopping...")
            self.stop()

    def publish_response(self, response: dict) -> None:
        """Publish a response message"""
        if self.channel is None:
            logger.error("Cannot publish: not connected")
            return

        self.channel.basic_publish(
            exchange=Config.EXCHANGE,
            routing_key=Config.ROUTING_KEY_RESPONSE,
            body=json.dumps(response),
            properties=pika.BasicProperties(
                delivery_mode=2, content_type="application/json"  # Persistent
            ),
        )
        logger.info(f"Sent response for document {response.get('document_id')}")

    def stop(self) -> None:
        """Stop consuming and close connections"""
        if self.channel is not None:
            try:
                self.channel.stop_consuming()
            except Exception:
                pass

        if self.connection is not None and not self.connection.is_closed:
            self.connection.close()

        logger.info("Connection closed")
