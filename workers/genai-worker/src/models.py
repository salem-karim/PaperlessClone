"""GenAI Worker Data Models and DTOs"""

from typing import TypedDict, Optional


class GenAIRequestDto(TypedDict, total=False):
    """Incoming GenAI request from RabbitMQ (from OCR worker response)"""
    document_id: str
    
    # OCR text either inline or reference
    ocr_text: Optional[str]  # Small text sent inline
    ocr_text_object_key: Optional[str]  # Large text stored in MinIO


class GenAIResponseDto(TypedDict, total=False):
    """Response sent back to RabbitMQ after GenAI processing"""
    document_id: str
    status: str  # "completed" or "failed"
    worker: str  # "genai"
    
    summary_text: Optional[str]
    
    error: Optional[str]
