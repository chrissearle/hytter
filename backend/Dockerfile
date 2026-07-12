# syntax=docker/dockerfile:1.25

FROM eclipse-temurin:22-jdk AS build

WORKDIR /app
COPY . .

RUN ./gradlew clean installDist

FROM eclipse-temurin:22-jre AS deploy

COPY --from=build /app/build/install/hytter-backend /opt/app

CMD ["/opt/app/bin/hytter-backend"]
