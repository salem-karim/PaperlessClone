"""Main entry point for OCR worker"""

import logging
import sys

from paperless_shared.rabbitmq_client import RabbitMQClient
from paperless_shared.minio_client import MinioClient

from .config import OcrConfig
from .ocr_service import OcrService
from .message_handler import OcrMessageHandler

# Configure logging
logging.basicConfig(
    level=getattr(logging, OcrConfig.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)

logger = logging.getLogger(__name__)


def main():
    """Initialize and start the OCR worker"""
    logger.info("Starting OCR Worker Service")

    rabbitmq_client = None

    try:
        # Load configuration
        logger.info("Loading configuration...")
        config = OcrConfig()

        # Initialize services
        logger.info("Initializing MinIO client...")
        minio_client = MinioClient(config)

        logger.info("Initializing OCR service...")
        ocr_service = OcrService()

        logger.info("Initializing RabbitMQ client...")
        rabbitmq_client = RabbitMQClient(config)

        logger.info("Initializing message handler...")
        message_handler = OcrMessageHandler(
            rabbitmq_client=rabbitmq_client,
            minio_client=minio_client,
            ocr_service=ocr_service,
            config=config
        )

        # Connect to RabbitMQ
        logger.info("Connecting to RabbitMQ...")
        rabbitmq_client.connect()

        # Start consuming messages
        logger.info(f"Starting message consumption on queue: {config.RABBITMQ_QUEUE}...")
        rabbitmq_client.start_consuming(message_handler.handle_message)

    except KeyboardInterrupt:
        logger.info("Shutting down OCR worker (KeyboardInterrupt)")
        if rabbitmq_client:
            rabbitmq_client.stop()
    except Exception as e:
        logger.error(f"Fatal error in OCR worker: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
