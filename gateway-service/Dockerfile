FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN sh gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
