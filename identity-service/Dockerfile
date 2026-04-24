# Build from root: docker build -f identity-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
COPY identity-service/pom.xml identity-service/pom.xml
COPY identity-service/src identity-service/src
RUN mvn install -pl shared -am -DskipTests && \
    mvn package -pl identity-service -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/identity-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
