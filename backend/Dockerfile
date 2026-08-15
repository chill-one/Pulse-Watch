FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY persistence/pom.xml persistence/pom.xml

COPY common/src common/src
COPY backend/src backend/src
COPY persistence/src persistence/src

RUN mvn -pl backend -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/backend/target/backend-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]