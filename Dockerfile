FROM maven:3.9.6-eclipse-temurin-21 AS builder
COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jre-jammy
COPY --from=builder target/*-fat.jar app.jar
COPY webroot /webroot

EXPOSE 8888
ENTRYPOINT [ "java", "-jar", "app.jar" ]
