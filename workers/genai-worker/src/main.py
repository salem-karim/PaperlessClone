"""Main entry point for GenAI worker"""

import logging
import sys

from paperless_shared.minio_client import MinioClient
from paperless_shared.rabbitmq_client import RabbitMQClient

from .config import GenAIConfig
from .genAI_service import GenAIService
from .message_handler import GenAIMessageHandler

# Configure logging
logging.basicConfig(
    level=getattr(logging, GenAIConfig.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)

logger = logging.getLogger(__name__)


def main():
    """Initialize and start the GenAI worker"""
    logger.info("Starting GenAI Worker Service")

    rabbitmq_client = None

    try:
        # Load configuration
        logger.info("Loading configuration...")
        config = GenAIConfig()

        # Initialize services
        logger.info("Initializing MinIO client...")
        minio_client = MinioClient(config)

        logger.info("Initializing GenAI service...")
        genai_service = GenAIService()

        logger.info("Initializing RabbitMQ client...")
        rabbitmq_client = RabbitMQClient(config)

        logger.info("Initializing message handler...")
        message_handler = GenAIMessageHandler(
            rabbitmq_client=rabbitmq_client,
            minio_client=minio_client,
            genai_service=genai_service,
            config=config,
        )

        # Connect to RabbitMQ
        logger.info("Connecting to RabbitMQ...")
        rabbitmq_client.connect()

        # Start consuming messages
        logger.info(f"Starting message consumption on queue: {config.RABBITMQ_QUEUE}...")
        rabbitmq_client.start_consuming(message_handler.handle_message)

    except KeyboardInterrupt:
        logger.info("Shutting down GenAI worker (KeyboardInterrupt)")
        if rabbitmq_client:
            rabbitmq_client.stop()
    except Exception as e:
        logger.error(f"Fatal error in GenAI worker: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
