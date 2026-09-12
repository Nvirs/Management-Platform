FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN sh gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Pinned numeric UID/GID
RUN addgroup -g 1000 -S spring && adduser -u 1000 -S -D -H -G spring spring
COPY --from=build /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
