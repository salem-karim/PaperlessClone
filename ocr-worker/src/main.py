"""Main entry point for OCR Worker"""

import sys
import logging
from logging import StreamHandler

from .config import Config
from .rabbitmq_client import RabbitMQClient
from .ocr_service import OcrService
from .message_handler import MessageHandler


def setup_logging() -> None:
    """Configure application logging"""
    logging.basicConfig(
        level=getattr(logging, Config.LOG_LEVEL),
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[StreamHandler(sys.stdout)],
    )


def main() -> None:
    """Main entry point"""
    setup_logging()
    logger = logging.getLogger(__name__)

    logger.info("Starting OCR Worker...")

    try:
        # Initialize components
        rabbitmq_client = RabbitMQClient()
        ocr_service = OcrService()
        message_handler = MessageHandler(rabbitmq_client, ocr_service)

        # Connect to RabbitMQ
        rabbitmq_client.connect()

        # Start consuming messages
        rabbitmq_client.start_consuming(message_handler.handle_message)

    except Exception as e:
        logger.error(f"Fatal error: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
