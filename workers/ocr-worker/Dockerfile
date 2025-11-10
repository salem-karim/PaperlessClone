FROM python:3.11-slim

# Install system dependencies for Tesseract, Poppler (pdf2image), and image processing
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Set working directory
WORKDIR /app

# Copy shared package first (from repo root context)
COPY workers/shared /app/shared

# Install shared package first
RUN uv pip install --system --no-cache /app/shared

# Copy OCR worker files
COPY workers/ocr-worker/pyproject.toml ./
COPY workers/ocr-worker/src/ ./src/

# Install OCR worker dependencies (shared is already installed)
# Use --no-sources to ignore [tool.uv.sources] which is only for local dev
RUN uv pip install --system --no-cache --no-sources .

# Set Python to run in unbuffered mode
ENV PYTHONUNBUFFERED=1
CMD ["python", "-m", "src.main"]
