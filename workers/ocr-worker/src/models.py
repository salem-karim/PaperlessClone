"""OCR Worker Data Models and DTOs"""

from typing import Optional
from paperless_shared.models import AbstractRequestDto, AbstractResponseDto


class OcrRequestDto(AbstractRequestDto, total=False):
    """Incoming OCR request from RabbitMQ

    Inherits document_id from AbstractRequestDto.
    """

    title: str
    original_filename: str
    content_type: str
    file_size: int
    file_bucket: str
    file_object_key: str


class OcrResponseDto(AbstractResponseDto, total=False):
    """Response sent back to RabbitMQ after OCR processing

    Inherits document_id, status, worker, error from AbstractResponseDto.
    """

    # For small text (< 100KB)
    ocr_text: Optional[str]

    # For large text stored in MinIO
    ocr_text_object_key: Optional[str]  # Just the key, bucket is implied
