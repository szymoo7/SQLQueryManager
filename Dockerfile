FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY src/main/resources/titanic.csv /app/resources/titanic.csv
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/Jetbrainstest-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/resources/titanic.csv resources/titanic.csv

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]