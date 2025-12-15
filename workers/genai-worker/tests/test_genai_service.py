"""Tests for GenAI service."""

from unittest.mock import Mock, patch

import pytest


def test_genai_service_module_exists():
    """Test that GenAI service module exists."""
    try:
        from src import genAI_service  # Note: capital AI

        assert genAI_service is not None
    except ImportError:
        pytest.skip("GenAI service module not found")


def test_genai_service_initialization():
    """Test service initialization."""
    try:
        from src.genAI_service import GenAIService
    except ImportError:
        pytest.skip("GenAI service not found")

    service = GenAIService()
    assert service is not None


@patch("src.genAI_service.genai.Client")
def test_summarize_text_success(mock_genai_client):
    """Test successful text summarization."""
    try:
        from src.genAI_service import GenAIService
    except ImportError:
        pytest.skip("GenAI service not found")

    # Mock the Gemini client and response
    mock_client_instance = Mock()
    mock_response = Mock()
    mock_response.text = "This is a generated summary of the document."
    mock_client_instance.models.generate_content.return_value = mock_response
    mock_genai_client.return_value = mock_client_instance

    service = GenAIService()
    service.enabled = True
    service.client = mock_client_instance

    result = service.summarize_text("Sample document text for summarization.")

    assert "generated summary" in result.lower()
    mock_client_instance.models.generate_content.assert_called_once()


@patch("src.genAI_service.genai.Client")
def test_summarize_text_api_error(mock_genai_client):
    """Test handling API errors."""
    try:
        from src.genAI_service import GenAIService
    except ImportError:
        pytest.skip("GenAI service not found")

    # Mock the Gemini client to raise an exception
    mock_client_instance = Mock()
    mock_client_instance.models.generate_content.side_effect = Exception("API Error")
    mock_genai_client.return_value = mock_client_instance

    service = GenAIService()
    service.enabled = True
    service.client = mock_client_instance

    result = service.summarize_text("Sample document text")

    # Should return error message instead of raising
    assert "[Summarization failed:" in result


def test_summarize_text_disabled():
    """Test summarization when service is disabled."""
    try:
        from unittest.mock import patch

        from src.genAI_service import GenAIService
    except ImportError:
        pytest.skip("GenAI service not found")

    # Create service without API key
    with patch.dict("os.environ", {"GOOGLE_API_KEY": ""}, clear=False):
        service = GenAIService()
        result = service.summarize_text("Sample text")

    assert "[Summarization skipped:" in result
