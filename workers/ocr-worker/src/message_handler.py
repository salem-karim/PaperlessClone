"""OCR Worker Message Handler"""

import logging
from typing import Dict, Any, Optional

from paperless_shared.abstract_handler import AbstractMessageHandler
from .models import OcrRequestDto, OcrResponseDto
from .ocr_service import OcrService
from .config import OcrConfig

logger = logging.getLogger(__name__)


class OcrMessageHandler(AbstractMessageHandler):
    """Handles OCR processing messages"""

    def __init__(self, rabbitmq_client, minio_client, ocr_service: OcrService, config: OcrConfig):
        super().__init__(rabbitmq_client, minio_client, config)
        self.ocr_service = ocr_service
        self.config: OcrConfig = config  # Type hint for IDE

    def _process_message(self, message: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Process OCR request: download file, run OCR, upload result"""
        request: OcrRequestDto = message  # type: ignore
        doc_id = request.get("document_id", "unknown")
        file_bucket = request.get("file_bucket")
        file_object_key = request.get("file_object_key")
        original_filename = request.get("original_filename", "unknown")
        content_type = request.get("content_type", "application/octet-stream")

        # Validate required fields
        if not doc_id:
            logger.error("Missing required field: document_id")
            return self._create_error_response("unknown", "Missing required field: document_id")

        if not file_bucket or not file_object_key:
            logger.error("Missing required fields: file_bucket or file_object_key")
            return self._create_error_response(doc_id, "Missing required fields: file_bucket or file_object_key")

        try:
            # Download file from MinIO
            logger.info(f"Downloading document {doc_id} from MinIO: {file_bucket}/{file_object_key}")
            file_data = self.minio.download_file(file_bucket, file_object_key)

            # Extract text using OCR
            logger.info(f"Starting OCR processing for document {doc_id}")
            ocr_text = self.ocr_service.process_document(file_data, content_type, original_filename)

            if not ocr_text or ocr_text.strip() == "":
                logger.warning(f"No text extracted from document {doc_id}")
                ocr_text = "[No text could be extracted from this document]"

            # Determine if text should be stored inline or in MinIO
            ocr_text_size = len(ocr_text.encode("utf-8"))
            
            response: OcrResponseDto = {
                "document_id": doc_id,
                "status": "completed",
                "worker": self.config.WORKER_NAME,
            }

            if ocr_text_size < self.config.OCR_TEXT_SIZE_THRESHOLD:
                # Small text: send directly in message
                logger.info(
                    f"OCR text size ({ocr_text_size} bytes) below threshold "
                    f"({self.config.OCR_TEXT_SIZE_THRESHOLD} bytes). Sending inline."
                )
                response["ocr_text"] = ocr_text
            else:
                # Large text: store in MinIO and send reference
                logger.info(
                    f"OCR text size ({ocr_text_size} bytes) above threshold "
                    f"({self.config.OCR_TEXT_SIZE_THRESHOLD} bytes). Storing in MinIO."
                )
                ocr_bucket = self.minio.ocr_results_bucket
                ocr_object_key = f"ocr/{doc_id}.txt"
                self.minio.upload_text(ocr_bucket, ocr_object_key, ocr_text)
                response["ocr_text_object_key"] = ocr_object_key

            logger.info(f"Completed OCR processing for document {doc_id}")
            return response

        except ValueError as e:
            # Unsupported file type
            error_msg = f"Unsupported file type: {str(e)}"
            logger.error(f"Failed processing document {doc_id}: {error_msg}")
            return self._create_error_response(doc_id, error_msg)

        except Exception as e:
            # Generic error
            error_msg = f"OCR processing failed: {str(e)}"
            logger.error(f"Failed processing document {doc_id}: {error_msg}", exc_info=True)
            return self._create_error_response(doc_id, error_msg)
