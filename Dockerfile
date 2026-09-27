# Image of the application for docker-compose.yml. Built on a machine with nothing but Docker:
# the first stage builds the jar with the Maven wrapper, the second runs it on a JRE.
#
# Tests are not run here. They run in CI and in the pre-push hook; the image build is for starting
# the stand, and repeating the suite inside it only makes the stand slower to bring up.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Poms first, so the dependency download is cached until a pom changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY tools/pom.xml tools/pom.xml
COPY app/pom.xml app/pom.xml
RUN ./mvnw -B -q -pl app -am dependency:go-offline

COPY app/src app/src
RUN ./mvnw -B -q -pl app -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/app/target/guide-app.jar guide-app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "guide-app.jar"]
