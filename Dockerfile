# The default deploy instructions (https://biffweb.com/docs/reference/production/) don't
# use Docker, but this file is provided in case you'd like to deploy with containers.
#
# When running the container, make sure you set any environment variables defined in config.env,
# e.g. using whatever tools your deployment platform provides for setting environment variables.
from clojure:temurin-17-tools-deps-bullseye

RUN apt-get update
RUN apt-get update && apt-get install -y \
  curl default-jre \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY src ./src
COPY dev ./dev
COPY resources ./resources
COPY deps.edn .
COPY opentelemetry-javaagent.jar opentelemetry-javaagent.jar

RUN clj -M:dev uberjar && cp target/jar/app.jar . && rm -r target
RUN rm -rf src dev resources deps.edn

EXPOSE 8080

ENV BIFF_PROFILE=dev
CMD ["/usr/bin/java", "-XX:-OmitStackTraceInFastThrow", "-XX:+CrashOnOutOfMemoryError", "-Dbiff.env.BIFF_PROFILE=dev", "-javaagent:opentelemetry-javaagent.jar", "-Dotel.resource.attributes=service.name=counter-service", "-Dotel.metrics.exporter=none", "-Dotel.logs.exporter=none", "-jar", "app.jar"]
