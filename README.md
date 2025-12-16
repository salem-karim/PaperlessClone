# PaperlessClone

FH-Technikum Wien SWEN3 Project - Paperless Clone

## Architecture Overview

This project is a document management system built with a microservices architecture:

### Services

- **WebUI** – React + Vite frontend served by Nginx (includes reverse proxy)
- **REST API** – Spring Boot backend serving as the central access point
- **OCR Worker** – Python-based worker for optical character recognition
- **GenAI Worker** – Python-based worker for AI-powered document summarization
- **Batch Processor** – Processes access logs in batches

### Infrastructure

- **RabbitMQ** – Message queue for worker communication
- **MinIO** – S3-compatible object storage for document files
- **PostgreSQL** – Relational database for metadata (accessed only by REST API)
- **Elasticsearch** – Search index for document metadata

### Quality Assurance

- **Testing**: Unit and integration tests for REST API and Batch Processor
- **CI/CD**: GitHub Actions pipeline for automated testing and deployment
- **Linting**:
  - WebUI: ESLint
  - REST API & Batch Processor: Checkstyle & SpotBugs
  - Workers: mypy & ruff

## Running the Project

### Start All Services

To start all components in detached mode:

```bash
docker compose up -d
```

This will start:
- `webui` (accessible at http://localhost:80)
- `rest-api` (accessible at http://localhost:8080)
- `ocr-worker`
- `genai-worker`
- `batch-processor`
- Supporting infrastructure (RabbitMQ, MinIO, PostgreSQL, Elasticsearch)

### Stop All Services

```bash
docker compose down
```

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f rest-api
```

### Rebuild After Changes

If you've made changes to the code:

```bash
docker compose up -d --build
```

## Development

### Prerequisites

- Docker Desktop (must be running)
- Node.js 20+ (for local WebUI development)
- Java 21+ (for local REST API development)
- Python 3.11+ (for local worker development)

### Local Development

#### WebUI
```bash
cd webui
bun install
bun run dev
```

#### REST API
```bash
cd rest-api
./mvnw spring-boot:run
```

#### Workers
```bash
cd workers
pip install -e ./shared
cd ocr-worker
pip install -r requirements.txt
python -m workers.ocr_worker.main
```

## Quick Reference (Felix's Cheatsheet)

1. **After code changes**: Rebuild Dockerfile
2. **To start services**: Run `docker compose up -d` (Docker Desktop must be running)
3. **To view logs**: Run `docker compose logs -f [service-name]`
4. **To stop services**: Run `docker compose down`