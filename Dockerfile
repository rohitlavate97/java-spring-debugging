# Multi-stage build for EOPIS Application
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S eopis && adduser -S eopis -G eopis
USER eopis

COPY --from=build /workspace/app/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
EXPOSE 8080 5005

ENTRYPOINT ["java", "-jar", "app.jar"]
