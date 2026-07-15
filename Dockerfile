# syntax=docker/dockerfile:1
FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src/ src/
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/target/hevy-mcp-*.jar application.jar
USER app:app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
