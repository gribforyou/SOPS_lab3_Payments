FROM temurin-focal-jdk-17.0.1_12:latest

WORKDIR /app

COPY payment_service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SERVICE_REGISTER_URL=http://registry:8080

ENTRYPOINT ["java", "-jar", "app.jar"]