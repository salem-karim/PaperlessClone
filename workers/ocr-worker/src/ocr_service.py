import io
import logging
import math
import os
import time
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import pymupdf
import pytesseract
from PIL import Image

logger = logging.getLogger(__name__)


class OcrService:
    """Handles OCR processing operations"""

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

    @staticmethod
    def _ocr_page(index_img_tuple):
        """OCR a single image (index, image) pair"""
        index, image_data = index_img_tuple
        image = Image.open(io.BytesIO(image_data))
        text = pytesseract.image_to_string(image, lang="eng")
        return index, text

    @staticmethod
    def _convert_pdf_page(args):
        pdf_data, page_number, dpi = args
        try:
            doc = pymupdf.open(stream=pdf_data, filetype="pdf")
            page = doc.load_page(page_number - 1)
            zoom = dpi / 72
            mat = pymupdf.Matrix(zoom, zoom)
            pix = page.get_pixmap(matrix=mat)
            return pix.tobytes("png")
        except Exception as e:
            logger.error(f"Failed to convert page {page_number}: {e}")
            raise

    def _process_image(self, image_data: bytes) -> str:
        """OCR a single image"""
        try:
            image = Image.open(io.BytesIO(image_data))
            text = pytesseract.image_to_string(image, lang="eng")
            logger.info(f"Extracted {len(text)} chars from image")
            return text.strip()
        except Exception as e:
            logger.error(f"Failed to process image: {e}")
            raise

    def _process_pdf(self, pdf_data: bytes) -> str:
        """OCR a PDF with optional parallel PDF->image conversion"""
        try:
            cpu_count = os.cpu_count() or 1
            thread_count = max(1, math.floor(cpu_count * 0.75))
            logger.info(f"Using {thread_count} threads of {cpu_count} cores")

            SMALL_PDF_THRESHOLD_PAGES = 3
            SMALL_PDF_THRESHOLD_BYTES = 512 * 1024  # 512KB

            # Get total pages without converting everything
            from pypdf import PdfReader

            pdf_reader = PdfReader(io.BytesIO(pdf_data))
            total_pages = len(pdf_reader.pages)
            logger.info(f"PDF has {total_pages} pages")

            # Decide if we should parallelize page conversion
            use_parallel = (
                total_pages > SMALL_PDF_THRESHOLD_PAGES or len(pdf_data) > SMALL_PDF_THRESHOLD_BYTES
            )

            start_time = time.time()
            if use_parallel:
                logger.info("PDF->image conversion: Using parallel processing")
                args_list = [(pdf_data, i, 300) for i in range(1, total_pages + 1)]
                with ProcessPoolExecutor(max_workers=cpu_count) as executor:
                    images = list(executor.map(OcrService._convert_pdf_page, args_list))
            else:
                logger.info("PDF->image conversion: Processing sequentially")
                images = [
                    OcrService._convert_pdf_page((pdf_data, i, 300))
                    for i in range(1, total_pages + 1)
                ]

            conversion_time = time.time() - start_time
            logger.info(f"Converted PDF to {len(images)} images in {conversion_time:.2f}s")

            # OCR (parallel/sequential) as before
            index_image_pairs = list(enumerate(images, start=1))
            use_parallel_ocr = use_parallel
            start_time = time.time()
            if use_parallel_ocr:
                with ProcessPoolExecutor(max_workers=cpu_count) as executor:
                    results = list(executor.map(OcrService._ocr_page, index_image_pairs))
            else:
                results = [OcrService._ocr_page(pair) for pair in index_image_pairs]

            combined_text = "\n\n".join(f"--- Page {i} ---\n{text}" for i, text in results).strip()
            conversion_time = time.time() - start_time
            logger.info(
                f"OCR complete: {len(combined_text)} chars from {len(images)} pages in {conversion_time:.2f}s"
            )
            return combined_text

        except Exception as e:
            logger.error(f"Failed to process PDF: {e}")
            raise

    def process_document(self, file_data: bytes, content_type: str, filename: str) -> str:
        """Determine file type and run appropriate OCR"""
        try:
            file_ext = Path(filename).suffix.lower()

            if file_ext in self.IMAGE_TYPES or content_type.startswith("image/"):
                logger.info(f"Processing image file: {filename}")
                return self._process_image(file_data)
            elif file_ext == self.PDF_TYPE or content_type == "application/pdf":
                logger.info(f"Processing PDF file: {filename}")
                return self._process_pdf(file_data)
            else:
                raise ValueError(f"Unsupported file type: {content_type} (extension: {file_ext})")

        except Exception as e:
            logger.error(f"Failed to process document {filename}: {e}")
            raise
