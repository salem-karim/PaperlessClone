"""Shared configuration for all workers"""

import os


class SharedConfig:
    """Common infrastructure configuration shared by all workers"""

    # RabbitMQ Connection
    RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
    RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
    RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
    RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")
    RABBITMQ_EXCHANGE = os.getenv("RABBITMQ_EXCHANGE", "documents.operations")

    # RabbitMQ Queues (worker-specific via env vars)
    RABBITMQ_QUEUE = os.getenv("RABBITMQ_QUEUE", "documents.processing")
    RABBITMQ_RESPONSE_QUEUE = os.getenv(
        "RABBITMQ_RESPONSE_QUEUE", f"{RABBITMQ_QUEUE}.response"
    )
    RABBITMQ_ROUTING_KEY_REQUEST = os.getenv(
        "RABBITMQ_ROUTING_KEY_REQUEST", "documents.request"
    )
    RABBITMQ_ROUTING_KEY_RESPONSE = os.getenv(
        "RABBITMQ_ROUTING_KEY_RESPONSE", "documents.response"
    )

    # MinIO Connection
    MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "localhost:9000")
    MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY", "minioadmin")
    MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY", "minioadmin")
    MINIO_SECURE = os.getenv("MINIO_SECURE", "false").lower() == "true"

    # MinIO Buckets
    MINIO_DOCUMENTS_BUCKET = os.getenv(
        "MINIO_DOCUMENTS_BUCKET", "paperless-documents"
    )
    MINIO_OCR_TEXT_BUCKET = os.getenv(
        "MINIO_OCR_TEXT_BUCKET", "paperless-ocr-text"
    )

    # Logging
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
