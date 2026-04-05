FROM maven:3.9.9-eclipse-temurin-23 AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:23-jre

WORKDIR /app

COPY --from=build /app/target/*SNAPSHOT.jar /app/app.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
