# Build from root: docker build -f booklet-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
COPY booklet-service/pom.xml booklet-service/pom.xml
COPY booklet-service/src booklet-service/src
RUN mvn install -pl shared -am -DskipTests && \
    mvn package -pl booklet-service -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/booklet-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
