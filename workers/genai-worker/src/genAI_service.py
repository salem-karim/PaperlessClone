import logging
from google import genai
from google.genai import types
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
            # Initialize the new Google GenAI client
            self.client = genai.Client(api_key=self.config.GOOGLE_API_KEY)
            self.enabled = True
            logger.info(
                f"Initialized Google Gemini client successfully with model: {self.config.GEMINI_MODEL}"
            )
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
            truncated_text = text[: self.config.SUMMARY_MAX_INPUT_LENGTH]
            if len(text) > self.config.SUMMARY_MAX_INPUT_LENGTH:
                logger.warning(
                    f"Text truncated from {len(text)} to {self.config.SUMMARY_MAX_INPUT_LENGTH} chars"
                )

            # Use template from config
            prompt = self.config.SUMMARY_PROMPT_TEMPLATE.format(text=truncated_text)

            # Generate content using the new SDK
            response = self.client.models.generate_content(
                model=self.config.GEMINI_MODEL, 
                contents=prompt,
                config=types.GenerateContentConfig(
                    temperature=0.7,
                    top_p=0.95,
                    top_k=40,
                    max_output_tokens=1024,
                    response_modalities=["TEXT"],
                )
            )
            
            # Extract text from response
            if response and response.text:
                summary = response.text.strip()
                logger.info(f"Received summary ({len(summary)} chars).")
                return summary
            else:
                logger.warning("No text in response from Gemini")
                return "[No summary generated]"
                
        except Exception as e:
            logger.error(f"Gemini summarization failed: {e}", exc_info=True)
            return f"[Summarization failed: {str(e)}]"
