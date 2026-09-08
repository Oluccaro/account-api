FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin app
COPY --from=build /build/target/account-api.jar app.jar
USER app
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
