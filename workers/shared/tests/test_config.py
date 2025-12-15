"""Tests for configuration."""

import os

from paperless_shared.config import SharedConfig


def test_shared_config_can_be_loaded():
    """Test that SharedConfig can be loaded."""
    # Set some environment variables
    os.environ["RABBITMQ_HOST"] = "localhost"
    os.environ["MINIO_ENDPOINT"] = "localhost:9000"

    config = SharedConfig()

    assert config is not None
    assert config.RABBITMQ_HOST == "localhost"
    assert config.MINIO_ENDPOINT == "localhost:9000"
