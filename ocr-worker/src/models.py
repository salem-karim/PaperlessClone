"""Data models and DTOs"""

from typing import TypedDict, Optional


class OcrRequestDto(TypedDict, total=False):
    """Incoming OCR request from RabbitMQ"""
    document_id: str
    title: str
    original_filename: str
    content_type: str
    file_size: int
    file_bucket: str
    file_object_key: str


class OcrResponseDto(TypedDict, total=False):
    """Response sent back to RabbitMQ"""
    document_id: str
    status: str  # "completed" or "failed"
    worker: str  # "ocr"
    
    # Option 1: Small text (< 100KB)
    ocr_text: Optional[str]
    
    # Option 2: Large text stored in MinIO
    ocr_text_object_key: Optional[str]  # Just the key, bucket is implied
    
    error: Optional[str]
