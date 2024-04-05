# JCode Teacher

## Building

To launch your tests:
```
./mvnw clean test
```

To package your application:
```
./mvnw clean package
```

To run your application:
```
./mvnw clean compile exec:java
```

### Help

* https://vertx.io/docs/[Vert.x Documentation]
* https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15[Vert.x Stack Overflow]
* https://groups.google.com/forum/?fromgroups#!forum/vertx[Vert.x User Group]
* https://discord.gg/6ry7aqPWXy[Vert.x Discord]
* https://gitter.im/eclipse-vertx/vertx-users[Vert.x Gitter]

## Run all in containers

```bash
HTTP_PORT=8080 LLM=deepseek-coder OLLAMA_BASE_URL=http://ollama-service:11434 docker compose --profile container up
# the instruct version is better if you need a constructive conversation to create a tutorial for example
HTTP_PORT=8080 LLM=deepseek-coder:instruct OLLAMA_BASE_URL=http://ollama-service:11434 docker compose --profile container up
```
> The first time only, you must wait for the complete downloading of the model. ⏳

## Use the native Ollama install

> To do for the first time only:
```bash
LLM=deepseek-coder:instruct
ollama pull ${LLM}
```

```bash
HTTP_PORT=8080 LLM=deepseek-coder:instruct OLLAMA_BASE_URL=http://host.docker.internal:11434 docker compose --profile webapp up
```

## Use a specific env file

```bash
docker compose --env-file deepseek-coder-instruct.env --profile webapp up
```

## Open the Web UI

http://localhost:8080

