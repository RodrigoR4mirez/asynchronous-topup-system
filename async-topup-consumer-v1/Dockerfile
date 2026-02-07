# --- STAGE 1: Build --- #
FROM maven:3.9.9-eclipse-temurin-21-noble AS build

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# --- STAGE 2: Runtime --- #
FROM registry.access.redhat.com/ubi9/openjdk-21:1.21

USER root

# Uncomment if you need Spanish locale and Lima timezone
# ENV LANG=es_ES.UTF-8 \
#     LANGUAGE=es_ES.es \
#     TZ=America/Lima

# RUN microdnf install -y tzdata glibc-langpack-es && \
#     ln -snf /usr/share/zoneinfo/America/Lima /etc/localtime && \
#     echo "America/Lima" > /etc/timezone

COPY --chown=185 --from=build /app/target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 --from=build /app/target/quarkus-app/*.jar /deployments/
COPY --chown=185 --from=build /app/target/quarkus-app/app/ /deployments/app/
COPY --chown=185 --from=build /app/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8086
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
