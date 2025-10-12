"""Configuration module"""

import os


class Config:
    """Application configuration"""

    # RabbitMQ
    RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
    RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
    RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
    RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")

    # RabbitMQ Routing
    EXCHANGE = os.getenv("RABBITMQ_EXCHANGE", "documents.operations")
    QUEUE = os.getenv("RABBITMQ_QUEUE", "documents.processing")
    ROUTING_KEY_LISTEN = os.getenv(
        "RABBITMQ_ROUTING_KEY_LISTEN", "documents.ocr.request"
    )

    ROUTING_KEY_RESPONSE = os.getenv(
        "RABBITMQ_ROUTING_KEY_RESPONSE", "documents.ocr.response"
    )

    # Logging
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
