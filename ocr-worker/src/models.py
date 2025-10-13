"""Data models and DTOs"""

from typing import TypedDict, Optional


class DocumentDto(TypedDict, total=False):
    """Incoming document message structure"""

    id: str
    title: str
    file_path: str


class OcrResponse(TypedDict, total=False):
    """Response message structure"""

    document_id: str
    status: str  # "completed" or "failed"
    worker: str  # "ocr"
    ocr_text: Optional[str]
    ocr_text_path: Optional[str]
    error: Optional[str]
