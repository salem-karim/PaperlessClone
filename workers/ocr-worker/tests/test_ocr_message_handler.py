"""Tests for OCR message handler."""

from unittest.mock import Mock, patch


@patch("src.ocr_service.pytesseract.get_tesseract_version")
def test_message_handler_module_exists(mock_tesseract_version):
    """Test that message handler module exists."""
    mock_tesseract_version.return_value = "5.0.0"
    from src import message_handler

    assert message_handler is not None


@patch("src.ocr_service.pytesseract.get_tesseract_version")
def test_message_handler_initialization(mock_tesseract_version):
    """Test message handler initialization."""
    mock_tesseract_version.return_value = "5.0.0"

    from src.config import OcrConfig
    from src.message_handler import OcrMessageHandler
    from src.ocr_service import OcrService

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    ocr_service = OcrService()
    config = OcrConfig()

    handler = OcrMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        ocr_service=ocr_service,
        config=config,
    )

    assert handler is not None


def test_process_message():
    """Test processing a message."""
    from src.config import OcrConfig
    from src.message_handler import OcrMessageHandler
    from src.ocr_service import OcrService

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    minio_client.download_file.return_value = b"fake pdf content"
    minio_client.ocr_results_bucket = "ocr-text"

    ocr_service = Mock(spec=OcrService)
    ocr_service.process_document.return_value = "Extracted text from document"

    config = OcrConfig()
    config.OCR_TEXT_SIZE_THRESHOLD = 1024 * 1024  # 1MB

    handler = OcrMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        ocr_service=ocr_service,
        config=config,
    )

    # Process message
    request = {
        "document_id": "123",
        "file_bucket": "documents",
        "file_object_key": "test.pdf",
        "original_filename": "test.pdf",
        "content_type": "application/pdf",
    }

    response = handler._process_message(request)

    # Verify
    assert response["document_id"] == "123"
    assert response["status"] == "completed"
    assert response["worker"] == "ocr"
    assert "ocr_text" in response or "ocr_text_object_key" in response
    minio_client.download_file.assert_called_once()
    ocr_service.process_document.assert_called_once()


def test_process_message_missing_fields():
    """Test processing a message with missing required fields."""
    from src.config import OcrConfig
    from src.message_handler import OcrMessageHandler
    from src.ocr_service import OcrService

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    ocr_service = Mock(spec=OcrService)
    config = OcrConfig()

    handler = OcrMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        ocr_service=ocr_service,
        config=config,
    )

    # Process message with missing fields
    request = {"document_id": "123"}

    response = handler._process_message(request)

    # Verify error response
    assert response["document_id"] == "123"
    assert response["status"] == "failed"
    assert "error" in response


def test_process_message_ocr_failure():
    """Test handling OCR processing failure."""
    from src.config import OcrConfig
    from src.message_handler import OcrMessageHandler
    from src.ocr_service import OcrService

    # Mock dependencies
    rabbitmq_client = Mock()
    minio_client = Mock()
    minio_client.download_file.return_value = b"fake pdf content"

    ocr_service = Mock(spec=OcrService)
    ocr_service.process_document.side_effect = Exception("OCR processing failed")

    config = OcrConfig()

    handler = OcrMessageHandler(
        rabbitmq_client=rabbitmq_client,
        minio_client=minio_client,
        ocr_service=ocr_service,
        config=config,
    )

    # Process message
    request = {
        "document_id": "123",
        "file_bucket": "documents",
        "file_object_key": "test.pdf",
        "original_filename": "test.pdf",
        "content_type": "application/pdf",
    }

    response = handler._process_message(request)

    # Verify error response
    assert response["document_id"] == "123"
    assert response["status"] == "failed"
    assert "error" in response
