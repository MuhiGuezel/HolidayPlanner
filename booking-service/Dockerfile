# Build from root: docker build -f booking-service/Dockerfile .
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY shared/src shared/src
COPY booking-service/pom.xml booking-service/pom.xml
COPY booking-service/src booking-service/src
RUN mvn install -pl shared -am -DskipTests && \
    mvn package -pl booking-service -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/booking-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
