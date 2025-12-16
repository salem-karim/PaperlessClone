# PaperlessClone
FH-Technikum Wien SWEN3 Project Paperless Clone

## Running the whole project

To start all components (webui, rest-api, ocr-worker) in detached mode:

```bash
docker compose up -d
````

This will start the following services:

* **webui** – the frontend web interface
* **rest-api** – the backend REST API
* **ocr-worker** – the OCR processing worker
* **genAI-worker** – the AI Summarization worker
* **batch** – the Batch Processor for Access Logs

---

The WebUI is using React + Vite for the frontend and using NginX as a WebServer and Reverse Proxy
The API is built using Spring-Boot and connects to the Workers via a RabbitMQ Queueing Service
The Document Files are stored in a MinIO Bucket Storage and the Metadata in a PostgreSQL Database as well as indexed in a ElasticSearch Storage container
The Workers are written in python and use a shared module for abstract custom RabbitMQ and MinIO Clients
Only the API accesses the PostgreSQL Database and can be considered as the central point
For the API as well as the Batch Processor Unit & Integration Tests have been written and tested using a GitHub Actions CI/CD Pipeline
Also linting is ran in the Pipeline for all 3 different spaces (WebUI using eslint, API and Batch Processor using checkstyles and the workers using mypy and ruff)

Felix's Cheatsheet:

Dockerfile builden -> wenn änderungen gemacht wurden
Docker compose starten -> servicces starten (docker desktop muss offen sein)
```bash
