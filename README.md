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

```bash
docker compose up -d rest-api postgres
````


* Access the API at `http://localhost:8081`.

---

## Running only the WebUI

1. Build the WebUI Docker image:

```bash
docker compose up -d webui
````

* Access the frontend at `http://localhost:8080`.

---

## Running only the OCR Worker

1. Build the OCR Worker Docker image:

```bash
docker compose up -d ocr-worker
````

* The worker will process OCR tasks in the background. Make sure it can connect to the REST API and any required queues/databases.




Felix's Cheatsheet:

Dockerfile builden -> wenn änderungen gemacht wurden
Docker compose starten -> servicces starten (docker desktop muss offen sein)
```bash
