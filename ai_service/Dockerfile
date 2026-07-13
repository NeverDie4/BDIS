FROM python:3.12-slim AS base

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY requirements-common.txt ./
RUN pip install --no-cache-dir --timeout 120 --retries 5 -r requirements-common.txt

FROM base AS recognition
COPY requirements-recognition.txt ./
RUN pip install --no-cache-dir --timeout 120 --retries 5 -r requirements-recognition.txt
COPY recognize_app.py ./
EXPOSE 8001

FROM base AS feature
COPY requirements-feature.txt ./
RUN pip install --no-cache-dir --timeout 120 --retries 5 \
    --index-url https://download.pytorch.org/whl/cpu \
    -r requirements-feature.txt
COPY feature_app.py ./
EXPOSE 8002
