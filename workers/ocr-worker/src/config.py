"""OCR Worker specific configuration"""

import os
from paperless_shared.config import SharedConfig


class OcrConfig(SharedConfig):
    """OCR worker configuration - inherits shared config and adds OCR-specific settings"""

    # OCR Processing Settings
    OCR_TEXT_SIZE_THRESHOLD = int(
        os.getenv("OCR_TEXT_SIZE_THRESHOLD", str(1 * 1024 * 1024))
    )  # 1MB - threshold for storing text inline vs MinIO

    # Tesseract OCR Settings
    TESSERACT_LANGUAGE = os.getenv("TESSERACT_LANGUAGE", "eng")
    TESSERACT_DPI = int(os.getenv("TESSERACT_DPI", "300"))

    # PDF Processing
    PDF_PARALLEL_THRESHOLD_PAGES = int(os.getenv("PDF_PARALLEL_THRESHOLD_PAGES", "5"))
    PDF_PARALLEL_THRESHOLD_BYTES = int(
        os.getenv("PDF_PARALLEL_THRESHOLD_BYTES", str(1 * 1024 * 1024))  # 1MB
    )

    WORKER_NAME = "ocr"
