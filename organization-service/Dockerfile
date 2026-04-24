# Build from root: docker build -f organization-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
COPY organization-service/pom.xml organization-service/pom.xml
COPY organization-service/src organization-service/src
RUN mvn install -pl shared -am -DskipTests && \
    mvn package -pl organization-service -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/organization-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
