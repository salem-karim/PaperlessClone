FROM python:3.11-slim

# Install system dependencies for Tesseract, Poppler (pdf2image), and image processing
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    poppler-utils \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Set working directory
WORKDIR /app

# Copy project files
COPY pyproject.toml ./
COPY src/ ./src/

# Install dependencies and the package using uv
RUN uv pip install --system --no-cache .

# Set Python to run in unbuffered mode
ENV PYTHONUNBUFFERED=1

CMD ["python", "-m", "src.main"]
