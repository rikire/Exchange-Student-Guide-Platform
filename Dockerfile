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
# DEBT-018: the JVM runs as an unprivileged user, the owner of the media and index directories. A
# named volume mounted there for the first time takes this ownership; one created by an earlier,
# root-run image needs it once (docs/architecture/deployment.md).
RUN groupadd --system guide \
    && useradd --system --gid guide --no-create-home --shell /usr/sbin/nologin guide \
    && mkdir -p /var/lib/guide/index /var/lib/guide/media \
    && chown -R guide:guide /var/lib/guide
WORKDIR /app
COPY --from=build /src/app/target/guide-app.jar guide-app.jar
USER guide
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "guide-app.jar"]
