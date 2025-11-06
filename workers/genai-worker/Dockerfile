FROM python:3.11-slim

# Install uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

# Set working directory
WORKDIR /app

# Copy shared package first (from repo root context)
COPY workers/shared /app/shared

# Install shared package first
RUN uv pip install --system --no-cache /app/shared

# Copy GenAI worker files
COPY workers/genai-worker/pyproject.toml ./
COPY workers/genai-worker/src/ ./src/

# Install GenAI worker dependencies (shared is already installed)
RUN uv pip install --system --no-cache .

# Set Python to run in unbuffered mode
ENV PYTHONUNBUFFERED=1
CMD ["python", "-m", "src.main"]
