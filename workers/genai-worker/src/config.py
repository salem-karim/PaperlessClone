"""GenAI Worker specific configuration"""

import os
from paperless_shared.config import SharedConfig


class GenAIConfig(SharedConfig):
    """GenAI worker configuration - inherits shared config and adds GenAI-specific settings"""

    # Google Gemini API
    GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
    GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-1.5-flash")

    # Summarization Settings
    SUMMARY_MAX_INPUT_LENGTH = int(
        os.getenv("SUMMARY_MAX_INPUT_LENGTH", "12000")
    )  # chars to send to AI
    
    SUMMARY_PROMPT_TEMPLATE = os.getenv(
        "SUMMARY_PROMPT_TEMPLATE",
        "Summarize the following OCR-extracted document text. "
        "If the text is messy, try to make it concise and readable:\n\n{text}"
    )

    # Worker-specific
    WORKER_NAME = "genai"
