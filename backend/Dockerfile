# syntax=docker/dockerfile:1.27

FROM eclipse-temurin:25-jdk AS build

WORKDIR /app
COPY . .

# The app jar is moved aside so it can be copied as its own layer below.
RUN ./gradlew clean installDist \
    && mkdir -p /app/appjar \
    && mv /app/build/install/hytter-backend/lib/hytter-backend-*.jar /app/appjar/

FROM eclipse-temurin:25-jre AS deploy

# Third-party dependencies are a large layer that changes rarely, so it stays
# cached across builds; the app jar is a small layer that changes every commit.
COPY --from=build /app/build/install/hytter-backend/bin /opt/app/bin
COPY --from=build /app/build/install/hytter-backend/lib /opt/app/lib

COPY --from=build /app/appjar/ /opt/app/lib/

CMD ["/opt/app/bin/hytter-backend"]
