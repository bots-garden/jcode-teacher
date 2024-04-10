#!/bin/bash
HTTP_PORT=8080 \
LLM=deepseek-coder:instruct \
OLLAMA_BASE_URL=http://host.docker.internal:11434 \
docker compose --profile webapp up
#HTTP_PORT=8080 LLM=deepseek-coder:instruct OLLAMA_BASE_URL=http://ollama-service:11434 docker compose --profile container up
