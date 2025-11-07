"""Abstract Model Classes for the DTOs"""

from typing import TypedDict, Optional


class AbstractRequestDto(TypedDict, total=False):
    """Base class for all request DTOs"""
    document_id: str


class AbstractResponseDto(TypedDict, total=False):
    """Base class for all response DTOs"""
    document_id: str
    status: str  # "completed" or "failed"
    worker: str  # Worker name (e.g., "ocr", "genai")
    error: Optional[str]  # Error message if status is "failed"
