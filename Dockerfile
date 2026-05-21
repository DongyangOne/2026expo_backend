FROM eclipse-temurin:17-jdk AS base
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

FROM base AS dependencies
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

FROM dependencies AS build
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre AS prod
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]