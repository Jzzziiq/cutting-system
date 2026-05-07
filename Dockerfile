FROM node:18-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY pom.xml ./
RUN mvn "-Dmaven.repo.local=/app/.m2" dependency:go-offline -q
COPY src/ ./src/
RUN mvn "-Dmaven.repo.local=/app/.m2" package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
COPY --from=frontend-build /app/frontend/dist/ /app/static/

ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
