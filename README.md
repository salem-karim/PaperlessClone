````markdown
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

---

## Running only the REST API

1. Build the REST API Docker image:

```bash
docker build -t rest-api ./rest-api
```

2. Run the container:

```bash
docker run --rm -p 8081:8080 rest-api
```

* Access the API at `http://localhost:8081`.

---

## Running only the WebUI

1. Build the WebUI Docker image:

```bash
docker build -t webui ./webui
```

2. Run the container:

```bash
docker run --rm -p 3000:80 webui
```

* Access the frontend at `http://localhost:3000`.

---

## Running only the OCR Worker

1. Build the OCR Worker Docker image:

```bash
docker build -t ocr-worker ./ocr-worker
```

2. Run the container:

```bash
docker run --rm ocr-worker
```

* The worker will process OCR tasks in the background. Make sure it can connect to the REST API and any required queues/databases.
