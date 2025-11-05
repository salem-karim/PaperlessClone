import logging
import google.generativeai as genai
from .config import Config

logger = logging.getLogger(__name__)

class GenAIService:
    """Handles text summarization using Google Gemini"""

    def __init__(self):
        if not Config.GOOGLE_API_KEY:
            logger.warning("GOOGLE_API_KEY not found! Summarization will be skipped.")
            self.enabled = False
            return

        try:
            genai.configure(api_key=Config.GOOGLE_API_KEY)
            self.model = genai.GenerativeModel("gemini-1.5-flash")
            self.enabled = True
            logger.info("Initialized Google Gemini client successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize Gemini: {e}")
            self.enabled = False

    def summarize_text(self, text: str) -> str:
        """Summarize the extracted OCR text"""
        if not self.enabled:
            return "[Summarization skipped: Gemini not configured]"

        try:
            logger.info("Sending text to Google Gemini for summarization...")
            prompt = (
                "Summarize the following OCR-extracted document text. "
                "If the text is messy, try to make it concise and readable:\n\n"
                f"{text[:12000]}"  # avoid huge payloads
            )
            response = self.model.generate_content(prompt)
            summary = response.text.strip() if response and response.text else "[No summary]"
            logger.info(f"Received summary ({len(summary)} chars).")
            return summary
        except Exception as e:
            logger.error(f"Gemini summarization failed: {e}", exc_info=True)
            return "[Summarization failed]"
