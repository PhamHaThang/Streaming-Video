FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy poms first to optimize cache for dependency download
COPY common-lib/pom.xml common-lib/pom.xml
COPY video-catalog-service/pom.xml video-catalog-service/pom.xml

RUN mvn -q -f common-lib/pom.xml -DskipTests dependency:go-offline

# Copy sources then compile
COPY common-lib/src common-lib/src
COPY video-catalog-service/src video-catalog-service/src

RUN mvn -q -f common-lib/pom.xml -DskipTests install
RUN mvn -q -f video-catalog-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /workspace/video-catalog-service/target/*.jar app.jar

RUN apk add --no-cache curl

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]