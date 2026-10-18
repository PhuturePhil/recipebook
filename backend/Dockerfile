FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

# Läuft als eigener Nutzer ohne Root-Rechte; /app bleibt für die Logdateien beschreibbar
RUN addgroup -S app && adduser -S -G app -h /app app

WORKDIR /app

COPY --from=builder --chown=app:app /app/target/recipebook-1.0.0.jar app.jar
RUN mkdir -p /app/logs && chown -R app:app /app

USER app

EXPOSE 8080

# Feste Heap-Grenze passend zum Container-Limit (docker-compose mem_limit 1200m); bei Speichermangel neu starten
# statt halb kaputt weiterzulaufen
ENTRYPOINT ["java", "-Xmx640m", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
