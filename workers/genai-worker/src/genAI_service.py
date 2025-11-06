import logging
import google.generativeai as genai
from .config import GenAIConfig

logger = logging.getLogger(__name__)

class GenAIService:
    """Handles text summarization using Google Gemini"""

    def __init__(self):
        self.config = GenAIConfig()
        
        if not self.config.GOOGLE_API_KEY:
            logger.warning("GOOGLE_API_KEY not found! Summarization will be skipped.")
            self.enabled = False
            return

        try:
            genai.configure(api_key=self.config.GOOGLE_API_KEY)
            self.model = genai.GenerativeModel(self.config.GEMINI_MODEL)
            self.enabled = True
            logger.info(f"Initialized Google Gemini client successfully with model: {self.config.GEMINI_MODEL}")
        except Exception as e:
            logger.error(f"Failed to initialize Gemini: {e}")
            self.enabled = False

    def summarize_text(self, text: str) -> str:
        """Summarize the extracted OCR text"""
        if not self.enabled:
            return "[Summarization skipped: Gemini not configured]"

        try:
            logger.info("Sending text to Google Gemini for summarization...")
            
            # Truncate text if too long
            truncated_text = text[:self.config.SUMMARY_MAX_INPUT_LENGTH]
            if len(text) > self.config.SUMMARY_MAX_INPUT_LENGTH:
                logger.warning(
                    f"Text truncated from {len(text)} to {self.config.SUMMARY_MAX_INPUT_LENGTH} chars"
                )
            
            # Use template from config
            prompt = self.config.SUMMARY_PROMPT_TEMPLATE.format(text=truncated_text)
            
            response = self.model.generate_content(prompt)
            summary = response.text.strip() if response and response.text else "[No summary]"
            logger.info(f"Received summary ({len(summary)} chars).")
            return summary
        except Exception as e:
            logger.error(f"Gemini summarization failed: {e}", exc_info=True)
            return "[Summarization failed]"
