"""Tests for RabbitMQ client."""

from unittest.mock import MagicMock, Mock, patch

from paperless_shared.config import SharedConfig
from paperless_shared.rabbitmq_client import RabbitMQClient


def test_rabbitmq_client_module_exists():
    """Test that RabbitMQ client module exists."""
    from paperless_shared import rabbitmq_client

    assert rabbitmq_client is not None


@patch("paperless_shared.rabbitmq_client.pika")
def test_rabbitmq_client_initialization(mock_pika):
    """Test RabbitMQ client initialization."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.RABBITMQ_HOST = "localhost"
    mock_config.RABBITMQ_PORT = 5672
    mock_config.RABBITMQ_USER = "guest"
    mock_config.RABBITMQ_PASSWORD = "guest"

    # Create client (should not connect yet)
    client = RabbitMQClient(mock_config)

    assert client is not None
    assert client.connection is None
    assert client.channel is None


@patch("paperless_shared.rabbitmq_client.pika")
def test_rabbitmq_connect(mock_pika):
    """Test connecting to RabbitMQ."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.RABBITMQ_HOST = "localhost"
    mock_config.RABBITMQ_PORT = 5672
    mock_config.RABBITMQ_USER = "guest"
    mock_config.RABBITMQ_PASSWORD = "guest"
    mock_config.RABBITMQ_EXCHANGE = "test_exchange"
    mock_config.RABBITMQ_QUEUE = "test_queue"
    mock_config.RABBITMQ_RESPONSE_QUEUE = "test_response_queue"
    mock_config.RABBITMQ_ROUTING_KEY_REQUEST = "test.request"
    mock_config.RABBITMQ_ROUTING_KEY_RESPONSE = "test.response"

    # Mock the connection
    mock_connection = MagicMock()
    mock_channel = MagicMock()
    mock_connection.channel.return_value = mock_channel
    mock_pika.BlockingConnection.return_value = mock_connection

    # Create client and connect
    client = RabbitMQClient(mock_config)
    client.connect()

    assert client.connection is not None
    assert client.channel is not None
    mock_pika.BlockingConnection.assert_called_once()
    mock_channel.exchange_declare.assert_called_once()
    assert mock_channel.queue_declare.call_count == 2
    assert mock_channel.queue_bind.call_count == 2


@patch("paperless_shared.rabbitmq_client.pika")
def test_rabbitmq_publish_response(mock_pika):
    """Test publishing a response message."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)
    mock_config.RABBITMQ_HOST = "localhost"
    mock_config.RABBITMQ_PORT = 5672
    mock_config.RABBITMQ_USER = "guest"
    mock_config.RABBITMQ_PASSWORD = "guest"
    mock_config.RABBITMQ_EXCHANGE = "test_exchange"
    mock_config.RABBITMQ_ROUTING_KEY_RESPONSE = "test.response"

    # Mock the connection
    mock_connection = MagicMock()
    mock_channel = MagicMock()
    mock_connection.channel.return_value = mock_channel
    mock_pika.BlockingConnection.return_value = mock_connection

    # Create client with mocked channel
    client = RabbitMQClient(mock_config)
    client.channel = mock_channel

    # Publish response
    response = {"document_id": "123", "status": "completed", "worker": "test"}
    client.publish_response(response)

    # Verify publish was called
    mock_channel.basic_publish.assert_called_once()
    call_kwargs = mock_channel.basic_publish.call_args[1]
    assert call_kwargs["exchange"] == "test_exchange"
    assert call_kwargs["routing_key"] == "test.response"


@patch("paperless_shared.rabbitmq_client.pika")
def test_rabbitmq_stop(mock_pika):
    """Test stopping RabbitMQ connection."""
    # Mock config
    mock_config = Mock(spec=SharedConfig)

    # Mock the connection
    mock_connection = MagicMock()
    mock_channel = MagicMock()
    mock_connection.is_closed = False

    # Create client with mocked connection
    client = RabbitMQClient(mock_config)
    client.connection = mock_connection
    client.channel = mock_channel

    # Stop
    client.stop()

    mock_channel.stop_consuming.assert_called_once()
    mock_connection.close.assert_called_once()
