FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy pom first for better Docker layer cache
COPY api-gateway/pom.xml api-gateway/pom.xml

WORKDIR /workspace/api-gateway
RUN mvn -q -DskipTests dependency:go-offline

# Copy source then build
COPY api-gateway/src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /workspace/api-gateway/target/api-gateway-1.0.0.jar app.jar

RUN apk add --no-cache curl

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]