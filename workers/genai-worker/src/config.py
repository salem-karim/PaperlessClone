"""GenAI Worker specific configuration"""

import os

from paperless_shared.config import SharedConfig


class GenAIConfig(SharedConfig):
    """GenAI worker configuration - inherits shared config and adds GenAI-specific settings"""

    # Google Gemini API
    GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
    GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")

    # Summarization Settings
    SUMMARY_MAX_INPUT_LENGTH = int(
        os.getenv("SUMMARY_MAX_INPUT_LENGTH", "12000")
    )  # chars to send to AI

    SUMMARY_PROMPT_TEMPLATE = os.getenv(
        "SUMMARY_PROMPT_TEMPLATE",
        """
            You are a document summarization assistant for a Document Management System (DMS).
            Your task is to analyse the following OCR-extracted text and provide a structured summary.

            Instructions:
            1. Create a concise summary (2-3 sentences)
            2. Identify the document type if possible
            3. Also include newlines for better paragraph structure
            4. Keep the summary factual and objective - do not add interpretations

            Document text:
            ---
            {text}
            ---

            Provide the summary now.
        """,
    )

    # Worker-specific
    WORKER_NAME = "genai"
