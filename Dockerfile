FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY contracts ./contracts
COPY src ./src
RUN mvn clean verify
RUN cmp contracts/openapi.json target/openapi.json

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN apk add --no-cache curl
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "app.jar"]
