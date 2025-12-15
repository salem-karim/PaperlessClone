"""Tests for MinIO client."""

from unittest.mock import MagicMock, Mock, patch

from paperless_shared.config import SharedConfig
from paperless_shared.minio_client import MinioClient


def test_minio_client_module_exists():
    """Test that MinIO client module exists."""
    from paperless_shared import minio_client

    assert minio_client is not None


@patch("paperless_shared.minio_client.Minio")
def test_minio_client_initialization(mock_minio_class):
    """Test MinIO client initialization."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.MINIO_ENDPOINT = "localhost:9000"
    mock_config.MINIO_ACCESS_KEY = "minioadmin"
    mock_config.MINIO_SECRET_KEY = "minioadmin"
    mock_config.MINIO_SECURE = False
    mock_config.MINIO_DOCUMENTS_BUCKET = "documents"
    mock_config.MINIO_OCR_TEXT_BUCKET = "ocr-text"

    # Mock the Minio client
    mock_client = MagicMock()
    mock_client.bucket_exists.return_value = True
    mock_minio_class.return_value = mock_client

    # Create client
    client = MinioClient(mock_config)

    assert client is not None
    mock_minio_class.assert_called_once()
    # Should check if buckets exist
    assert mock_client.bucket_exists.call_count == 2


@patch("paperless_shared.minio_client.Minio")
def test_minio_download_file(mock_minio_class):
    """Test downloading a file."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.MINIO_ENDPOINT = "localhost:9000"
    mock_config.MINIO_ACCESS_KEY = "minioadmin"
    mock_config.MINIO_SECRET_KEY = "minioadmin"
    mock_config.MINIO_SECURE = False
    mock_config.MINIO_DOCUMENTS_BUCKET = "documents"
    mock_config.MINIO_OCR_TEXT_BUCKET = "ocr-text"

    # Mock the Minio client
    mock_client = MagicMock()
    mock_client.bucket_exists.return_value = True

    # Mock the response
    mock_response = MagicMock()
    mock_response.read.return_value = b"test file content"
    mock_client.get_object.return_value = mock_response

    mock_minio_class.return_value = mock_client

    # Create client and download
    client = MinioClient(mock_config)
    result = client.download_file("test-bucket", "test.pdf")

    assert result == b"test file content"
    mock_client.get_object.assert_called_once_with("test-bucket", "test.pdf")
    mock_response.read.assert_called_once()
    mock_response.close.assert_called_once()


@patch("paperless_shared.minio_client.Minio")
def test_minio_upload_text(mock_minio_class):
    """Test uploading text."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.MINIO_ENDPOINT = "localhost:9000"
    mock_config.MINIO_ACCESS_KEY = "minioadmin"
    mock_config.MINIO_SECRET_KEY = "minioadmin"
    mock_config.MINIO_SECURE = False
    mock_config.MINIO_DOCUMENTS_BUCKET = "documents"
    mock_config.MINIO_OCR_TEXT_BUCKET = "ocr-text"

    # Mock the Minio client
    mock_client = MagicMock()
    mock_client.bucket_exists.return_value = True
    mock_minio_class.return_value = mock_client

    # Create client and upload
    client = MinioClient(mock_config)
    client.upload_text("test-bucket", "test.txt", "Hello World")

    # Verify upload was called
    mock_client.put_object.assert_called_once()
    call_args = mock_client.put_object.call_args
    assert call_args[0][0] == "test-bucket"
    assert call_args[0][1] == "test.txt"


@patch("paperless_shared.minio_client.Minio")
def test_minio_file_exists(mock_minio_class):
    """Test checking if file exists."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.MINIO_ENDPOINT = "localhost:9000"
    mock_config.MINIO_ACCESS_KEY = "minioadmin"
    mock_config.MINIO_SECRET_KEY = "minioadmin"
    mock_config.MINIO_SECURE = False
    mock_config.MINIO_DOCUMENTS_BUCKET = "documents"
    mock_config.MINIO_OCR_TEXT_BUCKET = "ocr-text"

    # Mock the Minio client
    mock_client = MagicMock()
    mock_client.bucket_exists.return_value = True
    mock_client.stat_object.return_value = MagicMock()
    mock_minio_class.return_value = mock_client

    # Create client and check
    client = MinioClient(mock_config)
    result = client.file_exists("test-bucket", "test.pdf")

    assert result is True
    mock_client.stat_object.assert_called_once_with("test-bucket", "test.pdf")
