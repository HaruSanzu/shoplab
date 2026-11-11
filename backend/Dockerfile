# Multi-stage build: stage 1 compiles the code, stage 2 only runs it.
# The final image contains Java + our jar, but no Maven and no source code: smaller and safer.

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom.xml first and download dependencies. Docker caches this layer, so as long as
# pom.xml doesn't change, rebuilding after a code change doesn't re-download everything.
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/shoplab-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
