"""Tests for GenAI message handler."""

from unittest.mock import Mock


def test_message_handler_module_exists():
    """Test that message handler module exists."""
    from src import message_handler

    assert message_handler is not None


def test_message_handler_initialization():
    """Test message handler initialization."""
    from src.config import GenAIConfig
    from src.genAI_service import GenAIService
    from src.message_handler import GenAIMessageHandler

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    genai_service = GenAIService()
    config = GenAIConfig()

    handler = GenAIMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        genai_service=genai_service,
        config=config,
    )

    assert handler is not None


def test_process_message_with_inline_ocr_text():
    """Test processing a message with inline OCR text."""
    from src.config import GenAIConfig
    from src.genAI_service import GenAIService
    from src.message_handler import GenAIMessageHandler

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()

    genai_service = Mock(spec=GenAIService)
    genai_service.summarize_text.return_value = "This is a summary of the document."

    config = GenAIConfig()

    handler = GenAIMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        genai_service=genai_service,
        config=config,
    )

    # Process message with inline OCR text
    request = {
        "document_id": "123",
        "ocr_text": "This is the extracted OCR text from the document.",
    }

    response = handler._process_message(request)

    # Verify
    assert response["document_id"] == "123"
    assert response["status"] == "completed"
    assert response["worker"] == "genai"
    assert "summary_text" in response
    genai_service.summarize_text.assert_called_once()


def test_process_message_with_minio_ocr_text():
    """Test processing a message with OCR text from MinIO."""
    from src.config import GenAIConfig
    from src.genAI_service import GenAIService
    from src.message_handler import GenAIMessageHandler

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    minio_client.download_file.return_value = b"OCR text from MinIO"
    minio_client.ocr_results_bucket = "ocr-text"

    genai_service = Mock(spec=GenAIService)
    genai_service.summarize_text.return_value = "This is a summary."

    config = GenAIConfig()

    handler = GenAIMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        genai_service=genai_service,
        config=config,
    )

    # Process message with MinIO reference
    request = {
        "document_id": "123",
        "ocr_text_object_key": "ocr/123.txt",
    }

    response = handler._process_message(request)

    # Verify
    assert response["document_id"] == "123"
    assert response["status"] == "completed"
    assert response["worker"] == "genai"
    minio_client.download_file.assert_called_once()
    genai_service.summarize_text.assert_called_once()


def test_process_message_missing_ocr_text():
    """Test processing a message without OCR text."""
    from src.config import GenAIConfig
    from src.genAI_service import GenAIService
    from src.message_handler import GenAIMessageHandler

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    genai_service = Mock(spec=GenAIService)
    config = GenAIConfig()

    handler = GenAIMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        genai_service=genai_service,
        config=config,
    )

    # Process message without OCR text
    request = {"document_id": "123"}

    response = handler._process_message(request)

    # Verify error response
    assert response["document_id"] == "123"
    assert response["status"] == "failed"
    assert "error" in response


def test_process_message_genai_failure():
    """Test handling GenAI processing failure."""
    from src.config import GenAIConfig
    from src.genAI_service import GenAIService
    from src.message_handler import GenAIMessageHandler

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()

    genai_service = Mock(spec=GenAIService)
    genai_service.summarize_text.side_effect = Exception("API Error")

    config = GenAIConfig()

    handler = GenAIMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        genai_service=genai_service,
        config=config,
    )

    # Process message
    request = {
        "document_id": "123",
        "ocr_text": "Sample text",
    }

    response = handler._process_message(request)

    # Verify error response
    assert response["document_id"] == "123"
    assert response["status"] == "failed"
    assert "error" in response
