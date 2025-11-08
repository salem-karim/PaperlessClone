"""GenAI Worker Message Handler"""

import logging
from typing import Optional

from paperless_shared.abstract_handler import AbstractMessageHandler
from .models import GenAIRequestDto, GenAIResponseDto
from .genAI_service import GenAIService
from .config import GenAIConfig

logger = logging.getLogger(__name__)


class GenAIMessageHandler(AbstractMessageHandler[GenAIRequestDto, GenAIResponseDto]):
    """Handles GenAI summarization messages"""

    def __init__(
        self,
        rabbitmq_client,
        minio_client,
        genai_service: GenAIService,
        config: GenAIConfig,
    ):
        super().__init__(rabbitmq_client, minio_client, config)
        self.genai_service = genai_service
        self.config: GenAIConfig = config

    def _process_message(self, message: GenAIRequestDto) -> Optional[GenAIResponseDto]:
        """Process GenAI request: get OCR text and generate summary"""
        doc_id = message.get("document_id", "unknown")

        # Validate required fields
        if not doc_id:
            logger.error("Missing required field: document_id")
            return self._create_error_response(
                "unknown", "Missing required field: document_id"
            )

        try:
            # Get OCR text (either inline or from MinIO)
            ocr_text = message.get("ocr_text")
            ocr_text_object_key = message.get("ocr_text_object_key")

            if ocr_text:
                # Text was sent inline
                logger.info(
                    f"Using inline OCR text for document {doc_id} ({len(ocr_text)} chars)"
                )
            elif ocr_text_object_key:
                # Text is stored in MinIO - download it
                logger.info(
                    f"Downloading OCR text for document {doc_id} from MinIO: {ocr_text_object_key}"
                )
                ocr_text_bytes = self.minio.download_file(
                    self.minio.ocr_results_bucket, ocr_text_object_key
                )
                ocr_text = ocr_text_bytes.decode("utf-8")
                logger.info(f"Downloaded OCR text ({len(ocr_text)} chars)")
            else:
                error_msg = "No OCR text provided (neither inline nor object key)"
                logger.error(error_msg)
                return self._create_error_response(doc_id, error_msg)

            # Generate summary using GenAI
            logger.info(
                f"Generating summary for document {doc_id} using {self.config.GEMINI_MODEL}"
            )
            summary_text = self.genai_service.summarize_text(ocr_text)

            if summary_text == "[No summary generated]":
                return self._create_error_response(doc_id, "No Summary got generated")
            if "[Summarization failed:" in summary_text:
                return self._create_error_response(doc_id, "Summarization failed")

            response: GenAIResponseDto = {
                "document_id": doc_id,
                "status": "completed",
                "worker": self.config.WORKER_NAME,
                "summary_text": summary_text,
            }

            logger.info(f"Completed GenAI processing for document {doc_id}")
            return response

        except Exception as e:
            # Generic error
            error_msg = f"GenAI processing failed: {str(e)}"
            logger.error(
                f"Failed processing document {doc_id}: {error_msg}", exc_info=True
            )
            return self._create_error_response(doc_id, error_msg)
