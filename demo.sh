#!/bin/bash
LLM=deepseek-coder:instruct OLLAMA_BASE_URL=http://host.docker.internal:11434 docker compose --profile webapp up