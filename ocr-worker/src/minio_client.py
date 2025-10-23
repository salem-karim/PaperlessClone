"""MinIO client for file operations"""

import logging
import io
from minio import Minio
from minio.error import S3Error

from .config import Config

logger = logging.getLogger(__name__)


class MinioClient:
    """Handles MinIO file operations"""

    def __init__(self):
        self.client = Minio(
            Config.MINIO_ENDPOINT,
            access_key=Config.MINIO_ACCESS_KEY,
            secret_key=Config.MINIO_SECRET_KEY,
            secure=Config.MINIO_SECURE,
        )
        self.documents_bucket = Config.MINIO_DOCUMENTS_BUCKET
        self.ocr_results_bucket = Config.MINIO_OCR_TEXT_BUCKET
        
        self._ensure_buckets()

    def _ensure_buckets(self):
        """Ensure required buckets exist"""
        for bucket in [self.documents_bucket, self.ocr_results_bucket]:
            try:
                if not self.client.bucket_exists(bucket):
                    self.client.make_bucket(bucket)
                    logger.info(f"Created bucket: {bucket}")
                else:
                    logger.info(f"Bucket already exists: {bucket}")
            except S3Error as e:
                logger.error(f"Failed to ensure bucket {bucket}: {e}")
                raise

    def download_file(self, bucket: str, object_key: str) -> bytes:
        """
        Download a file from MinIO

        Args:
            bucket: Bucket name
            object_key: Object key in bucket

        Returns:
            File contents as bytes

        Raises:
            S3Error: If download fails
        """
        try:
            logger.info(f"Downloading {object_key} from bucket {bucket}")
            response = self.client.get_object(bucket, object_key)
            data = response.read()
            response.close()
            response.release_conn()
            logger.info(f"Downloaded {len(data)} bytes from {object_key}")
            return data
        except S3Error as e:
            logger.error(f"Failed to download {object_key} from {bucket}: {e}")
            raise

    def upload_text(self, bucket: str, object_key: str, text: str) -> None:
        """
        Upload text content to MinIO

        Args:
            bucket: Bucket name
            object_key: Object key to store
            text: Text content to upload

        Raises:
            S3Error: If upload fails
        """
        try:
            logger.info(f"Uploading text to {object_key} in bucket {bucket}")
            text_bytes = text.encode("utf-8")
            data = io.BytesIO(text_bytes)
            
            self.client.put_object(
                bucket,
                object_key,
                data,
                length=len(text_bytes),
                content_type="text/plain; charset=utf-8",
            )
            logger.info(
                f"Uploaded {len(text_bytes)} bytes to {object_key} in {bucket}"
            )
        except S3Error as e:
            logger.error(f"Failed to upload text to {object_key}: {e}")
            raise

    def file_exists(self, bucket: str, object_key: str) -> bool:
        """Check if a file exists in MinIO"""
        try:
            self.client.stat_object(bucket, object_key)
            return True
        except S3Error:
            return False