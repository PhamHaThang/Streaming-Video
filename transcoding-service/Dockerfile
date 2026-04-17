# ==========================================
# STAGE 1: Build
# ==========================================
FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy poms first to optimize cache for dependency download
COPY common-lib/pom.xml common-lib/pom.xml
COPY transcoding-service/pom.xml transcoding-service/pom.xml

RUN mvn -q -f common-lib/pom.xml -DskipTests dependency:go-offline

# Copy sources then compile
COPY common-lib/src common-lib/src
COPY transcoding-service/src transcoding-service/src

RUN mvn -q -f common-lib/pom.xml -DskipTests install
RUN mvn -q -f transcoding-service/pom.xml -DskipTests package
# ==========================================
# STAGE 2: Run
# ==========================================
FROM eclipse-temurin:17-jre-alpine

# Download FFmpeg and curl
RUN apk update && \
    apk add --no-cache ffmpeg curl

WORKDIR /app

# Create temp dir to contain file video
RUN mkdir -p /app/temp/transcode

COPY --from=build /workspace/transcoding-service/target/*.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]