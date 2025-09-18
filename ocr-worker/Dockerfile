FROM python:3.12
WORKDIR /app

# Install deps
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Add source
COPY . .

CMD ["python", "main.py"]
