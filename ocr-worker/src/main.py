"""Main entry point for OCR worker"""

import logging
import sys

from .config import Config
from .rabbitmq_client import RabbitMQClient
from .ocr_service import OcrService
from .minio_client import MinioClient
from .message_handler import MessageHandler

# Configure logging
logging.basicConfig(
    level=getattr(logging, Config.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)

logger = logging.getLogger(__name__)


def main():
    """Initialize and start the OCR worker"""
    logger.info("Starting OCR Worker Service")

    try:
        # Initialize services
        logger.info("Initializing MinIO client...")
        minio_client = MinioClient()

        logger.info("Initializing OCR service...")
        ocr_service = OcrService()

        logger.info("Initializing RabbitMQ client...")
        rabbitmq_client = RabbitMQClient()

        logger.info("Initializing message handler...")
        message_handler = MessageHandler(rabbitmq_client, ocr_service, minio_client)

        # Connect to RabbitMQ
        logger.info("Connecting to RabbitMQ...")
        rabbitmq_client.connect()

        # Start consuming messages
        logger.info("Starting message consumption...")
        rabbitmq_client.start_consuming(message_handler.handle_message)

    except KeyboardInterrupt:
        logger.info("Shutting down OCR worker (KeyboardInterrupt)")
        if rabbitmq_client:
            rabbitmq_client.close()
    except Exception as e:
        logger.error(f"Fatal error in OCR worker: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
