import json
import os
import sys
from typing import TypedDict, Union, Optional
from logging import Logger, basicConfig, INFO, StreamHandler, getLogger
import pika
from pika.adapters.blocking_connection import BlockingChannel, BlockingConnection

rabbitmq_host: str = os.getenv("RABBITMQ_HOST") or "localhost"

# Configure Logging
basicConfig(
    level=INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[StreamHandler(sys.stdout)],  # Explicitly use stdout
)
logger: Logger = getLogger(__name__)


class DocumentDto(TypedDict, total=False):
    id: str
    title: str


MessageBody = Union[DocumentDto, str]

EXCHANGE = "documents.operations"
QUEUE = "documents.processing"
ROUTING_KEY = "documents.*"


def processing(channel: BlockingChannel, method, properties, body: bytes) -> None:
    """Process incoming messages"""
    logger.info(f"Received message with routing key: {method.routing_key}")
    try:
        msg: MessageBody = json.loads(body)
    except json.JSONDecodeError:
        msg = body.decode()
        logger.warning("Failed to parse JSON, treating as string")

    logger.info(f"Message content: {msg}")

    # Acknowledge the message
    channel.basic_ack(delivery_tag=method.delivery_tag)
    logger.info("Message acknowledged")


def main() -> None:
    """Main consumer logic"""
    logger.info("Starting OCR Worker...")
    logger.info(f"Connecting to RabbitMQ at {rabbitmq_host}...")

    # Initialize as None to satisfy type checker
    connection: Optional[BlockingConnection] = None
    channel: Optional[BlockingChannel] = None

    try:
        # Use the environment variable, not hardcoded "localhost"
        connection = pika.BlockingConnection(pika.ConnectionParameters(rabbitmq_host))
        channel = connection.channel()
        logger.info("Connected to RabbitMQ successfully")

        # Declare exchange and queue
        channel.exchange_declare(exchange=EXCHANGE, exchange_type="topic", durable=True)
        channel.queue_declare(queue=QUEUE, durable=True)
        channel.queue_bind(exchange=EXCHANGE, queue=QUEUE, routing_key=ROUTING_KEY)
        logger.info(f"Declared exchange '{EXCHANGE}' and queue '{QUEUE}'")

        # Start consuming
        channel.basic_consume(queue=QUEUE, on_message_callback=processing)
        logger.info(f"Waiting for messages on queue '{QUEUE}'. Press CTRL+C to exit")

        channel.start_consuming()

    except KeyboardInterrupt:
        logger.info("Received shutdown signal, stopping...")
        if channel is not None:
            channel.stop_consuming()
        if connection is not None:
            connection.close()
        logger.info("Shutdown complete")
    except Exception as e:
        logger.error(f"Fatal error: {e}", exc_info=True)
        sys.exit(1)
    finally:
        # Cleanup in finally block to ensure it always runs
        if channel is not None:
            try:
                channel.stop_consuming()
            except Exception:
                pass  # Already stopped or never started
        if connection is not None and not connection.is_closed:
            connection.close()


if __name__ == "__main__":
    main()
