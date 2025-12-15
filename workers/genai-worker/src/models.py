"""GenAI Worker Data Models and DTOs"""

from paperless_shared.models import AbstractRequestDto, AbstractResponseDto


class GenAIRequestDto(AbstractRequestDto, total=False):
    """Incoming GenAI request from RabbitMQ (from OCR worker response)

    Inherits document_id from AbstractRequestDto.
    """

    # OCR text either inline or reference
    ocr_text: str | None  # Small text sent inline
    ocr_text_object_key: str | None  # Large text stored in MinIO


class GenAIResponseDto(AbstractResponseDto, total=False):
    """Response sent back to RabbitMQ after GenAI processing

    Inherits document_id, status, worker, error from AbstractResponseDto.
    """

    summary_text: str | None
