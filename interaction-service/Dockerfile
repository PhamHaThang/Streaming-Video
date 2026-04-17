FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy poms first to optimize cache for dependency download
COPY common-lib/pom.xml common-lib/pom.xml
COPY interaction-service/pom.xml interaction-service/pom.xml

RUN mvn -q -f common-lib/pom.xml -DskipTests dependency:go-offline

# Copy sources then compile
COPY common-lib/src common-lib/src
COPY interaction-service/src interaction-service/src

RUN mvn -q -f common-lib/pom.xml -DskipTests install
RUN mvn -q -f interaction-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /workspace/interaction-service/target/*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]