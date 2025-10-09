FROM ghcr.io/astral-sh/uv:python3.13-bookworm-slim

# Setup a non-root user
RUN groupadd --system --gid 999 nonroot \
    && useradd --system --gid 999 --uid 999 --create-home nonroot

# Install the project into `/app`
WORKDIR /app

# Copy from the cache instead of linking since it's a mounted volume
ENV UV_LINK_MODE=copy

# Ensure installed tools can be executed out of the box
ENV UV_TOOL_BIN_DIR=/usr/local/bin

# Note: maybe need to install tesseract itself for the OCR
# RUN apt-get update && apt-get install -y tesseract-ocr && rm -rf /var/lib/apt/lists/*


# Install the project's dependencies using the lockfile if it exists
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    sh -c 'if [ -f uv.lock ]; then \
    uv sync --locked --no-install-project --no-dev; \
    else \
    uv sync --no-install-project --no-dev; \
    fi'

# Then, add the rest of the project source code and install it
# Installing separately from its dependencies allows optimal layer caching

COPY . /app
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked --no-dev

# Place executables in the environment at the front of the path
ENV PATH="/app/.venv/bin:$PATH"

# Reset the entrypoint, don't invoke `uv`
ENTRYPOINT []

# Use the non-root user to run our application
USER nonroot

CMD [ "python", "main.py" ]
