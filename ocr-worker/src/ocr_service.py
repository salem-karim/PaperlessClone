"""OCR processing business logic"""

import logging
import io
import tempfile
from pathlib import Path
from typing import Optional

import pytesseract
from PIL import Image
from pdf2image import convert_from_bytes

logger = logging.getLogger(__name__)


class OcrService:
    """Handles OCR processing operations"""

    # Supported file types
    IMAGE_TYPES = {".png", ".jpg", ".jpeg", ".tiff", ".bmp", ".gif"}
    PDF_TYPE = ".pdf"

    def __init__(self):
        # Verify Tesseract is installed
        try:
            pytesseract.get_tesseract_version()
            logger.info("Tesseract OCR is available")
        except Exception as e:
            logger.error(f"Tesseract not found: {e}")
            raise

    def process_document(
        self, file_data: bytes, content_type: str, filename: str
    ) -> str:
        """
        Process a document and extract text

        Args:
            file_data: File contents as bytes
            content_type: MIME type of the file
            filename: Original filename

        Returns:
            Extracted text from OCR

        Raises:
            ValueError: If file type is not supported
            Exception: If OCR processing fails
        """
        logger.info(f"Processing file: {filename} (type: {content_type})")

        try:
            file_ext = Path(filename).suffix.lower()

            if file_ext in self.IMAGE_TYPES or content_type.startswith("image/"):
                return self._process_image(file_data)
            elif file_ext == self.PDF_TYPE or content_type == "application/pdf":
                return self._process_pdf(file_data)
            else:
                raise ValueError(
                    f"Unsupported file type: {content_type} (extension: {file_ext})"
                )

        except Exception as e:
            logger.error(f"Failed to process document {filename}: {e}")
            raise

    def _process_image(self, image_data: bytes) -> str:
        """
        Extract text from an image

        Args:
            image_data: Image file as bytes

        Returns:
            Extracted text
        """
        logger.info("Processing image with Tesseract")
        try:
            image = Image.open(io.BytesIO(image_data))
            text = pytesseract.image_to_string(image, lang="eng")
            logger.info(f"Extracted {len(text)} characters from image")
            return text.strip()
        except Exception as e:
            logger.error(f"Failed to process image: {e}")
            raise

    def _process_pdf(self, pdf_data: bytes) -> str:
        """
        Extract text from a PDF by converting to images

        Args:
            pdf_data: PDF file as bytes

        Returns:
            Extracted text from all pages
        """
        logger.info("Processing PDF with pdf2image and Tesseract")
        try:
            # Convert PDF to images (one per page)
            images = convert_from_bytes(
                pdf_data,
                dpi=300,  # Higher DPI = better quality
                fmt="png",
            )
            logger.info(f"Converted PDF to {len(images)} images")

            # Extract text from each page
            all_text = []
            for i, image in enumerate(images, 1):
                logger.info(f"Processing page {i}/{len(images)}")
                page_text = pytesseract.image_to_string(image, lang="eng")
                all_text.append(f"--- Page {i} ---\n{page_text}")

            combined_text = "\n\n".join(all_text).strip()
            logger.info(f"Extracted {len(combined_text)} characters from PDF")
            return combined_text

        except Exception as e:
            logger.error(f"Failed to process PDF: {e}")
            raise
