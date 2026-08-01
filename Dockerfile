# --- Frontend bauen -------------------------------------------------------
FROM node:20-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# --- Backend bauen --------------------------------------------------------
FROM gradle:8.10-jdk21 AS backend
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
# Das Frontend ist im vorigen Schritt schon gebaut, daher hier ueberspringen.
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN gradle bootJar --no-daemon -PskipFrontend

# --- Laufzeit -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
