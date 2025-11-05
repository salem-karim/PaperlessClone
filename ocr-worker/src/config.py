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
    RESPONSE_QUEUE = os.getenv("RABBITMQ_RESPONSE_QUEUE", f"{QUEUE}.response")
    ROUTING_KEY_REQUEST = os.getenv(
        "RABBITMQ_ROUTING_KEY_REQUEST", "documents.ocr.request"
    )
    ROUTING_KEY_RESPONSE = os.getenv(
        "RABBITMQ_ROUTING_KEY_RESPONSE", "documents.ocr.response"
    )

    # MinIO
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

    # OCR Settings
    OCR_TEXT_SIZE_THRESHOLD = int(
        os.getenv("OCR_TEXT_SIZE_THRESHOLD", str(100 * 1024))
    )  # 100 KB

    # Logging
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

    # Google Gemini
    GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
