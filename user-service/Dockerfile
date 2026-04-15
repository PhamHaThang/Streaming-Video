FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy poms first to optimize cache for dependency download
COPY common-lib/pom.xml common-lib/pom.xml
COPY user-service/pom.xml user-service/pom.xml

RUN mvn -q -f common-lib/pom.xml -DskipTests dependency:go-offline

# Copy sources then compile
COPY common-lib/src common-lib/src
COPY user-service/src user-service/src

RUN mvn -q -f common-lib/pom.xml -DskipTests install
RUN mvn -q -f user-service/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /workspace/user-service/target/user-service-1.0.0.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]