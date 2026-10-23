# Stage 1: Build the React TypeScript Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the Spring Boot Backend (embedding the frontend)
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests

# Stage 3: Minimal, High-Performance JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/*.jar app.jar

# Cloud providers (Render, Koyeb, Railway) pass the port via PORT env var
ENV PORT=8080
EXPOSE 8080

# Keep memory within free tier 512MB RAM constraints
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-jar", "app.jar"]
