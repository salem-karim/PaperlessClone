"""Tests for OCR service."""

from unittest.mock import MagicMock, patch

import pytest


def test_ocr_service_module_exists():
    """Test that OCR service module exists."""
    from src import ocr_service

    assert ocr_service is not None


@patch("src.ocr_service.pytesseract.get_tesseract_version")
def test_ocr_service_initialization(mock_tesseract_version):
    """Test OCR service can be initialized."""
    from src.ocr_service import OcrService

    # Mock tesseract being available
    mock_tesseract_version.return_value = "5.0.0"

    service = OcrService()
    assert service is not None


@patch("src.ocr_service.pytesseract.get_tesseract_version")
@patch("src.ocr_service.pytesseract.image_to_string")
@patch("src.ocr_service.Image.open")
def test_process_image(mock_image_open, mock_image_to_string, mock_tesseract_version):
    """Test processing an image."""
    from src.ocr_service import OcrService

    # Mock tesseract
    mock_tesseract_version.return_value = "5.0.0"

    # Mock image
    mock_image = MagicMock()
    mock_image_open.return_value = mock_image

    # Mock OCR result
    mock_image_to_string.return_value = "Sample extracted text"

    # Create service and process
    service = OcrService()
    result = service.process_document(b"fake image data", "image/png", "test.png")

    assert "Sample extracted text" in result
    mock_image_to_string.assert_called_once()


@patch("src.ocr_service.pytesseract.get_tesseract_version")
@patch("src.ocr_service.ProcessPoolExecutor")
@patch("pypdf.PdfReader")  # Patch where it's imported from, not where it's used
def test_process_pdf(mock_pdf_reader, mock_executor, mock_tesseract_version):
    """Test processing a PDF."""
    from src.ocr_service import OcrService

    # Mock tesseract
    mock_tesseract_version.return_value = "5.0.0"

    # Mock PDF reader
    mock_reader = MagicMock()
    mock_reader.pages = [MagicMock(), MagicMock()]  # 2 pages
    mock_pdf_reader.return_value = mock_reader

    # Mock executor to run functions sequentially
    mock_executor_instance = MagicMock()
    mock_executor_instance.__enter__.return_value = mock_executor_instance
    mock_executor_instance.map.side_effect = lambda func, args: [func(arg) for arg in args]
    mock_executor.return_value = mock_executor_instance

    # Mock the static methods that will be called
    with patch.object(OcrService, "_convert_pdf_page", return_value=b"fake png data"):
        with patch.object(
            OcrService, "_ocr_page", side_effect=[(1, "Page 1 text"), (2, "Page 2 text")]
        ):
            # Create service and process
            service = OcrService()
            result = service.process_document(b"%PDF-1.4 fake pdf", "application/pdf", "test.pdf")

            assert "Page 1 text" in result
            assert "Page 2 text" in result


@patch("src.ocr_service.pytesseract.get_tesseract_version")
def test_process_unsupported_file(mock_tesseract_version):
    """Test handling unsupported file types."""
    from src.ocr_service import OcrService

    # Mock tesseract
    mock_tesseract_version.return_value = "5.0.0"

    service = OcrService()

    with pytest.raises(ValueError, match="Unsupported file type"):
        service.process_document(b"fake data", "application/zip", "test.zip")
