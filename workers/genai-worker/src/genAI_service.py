import logging
import re
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

            # Clean the text before processing in a single pass
            # Multiple spaces/tabs → single space
            cleaned_text = re.sub(r"[ \t]+", " ", text)
            # Remove trailing spaces before newlines
            cleaned_text = re.sub(r" *\n", "\n", cleaned_text)
            # Max 2 consecutive newlines
            cleaned_text = re.sub(r"\n{3,}", "\n\n", cleaned_text)
            cleaned_text = cleaned_text.strip()  # Remove leading/trailing whitespace

            original_size = len(text)
            cleaned_size = len(cleaned_text)
            if cleaned_size < original_size:
                logger.info(
                    f"Cleaned text: reduced from {original_size} to {cleaned_size} chars ({original_size - cleaned_size} chars removed)"
                )

            # Truncate text if still too long
            truncated_text = cleaned_text[: self.config.SUMMARY_MAX_INPUT_LENGTH]
            if len(cleaned_text) > self.config.SUMMARY_MAX_INPUT_LENGTH:
                logger.warning(
                    f"Text truncated from {len(cleaned_text)} to {self.config.SUMMARY_MAX_INPUT_LENGTH} chars"
                )

            # Use template from config
            prompt = self.config.SUMMARY_PROMPT_TEMPLATE.format(text=truncated_text)

            logger.info(f"This is the prompt used: {prompt}")
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
                ),
            )

            logger.info(f"Received Response from Gemini: {response}")

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
