FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y --no-install-recommends postgresql-client && rm -rf /var/lib/apt/lists/*

WORKDIR /deployments

# Ordered for optimal Docker layer caching (least → most frequently changed)
COPY --chown=185 target/quarkus-app/lib/ lib/
COPY --chown=185 target/quarkus-app/quarkus/ quarkus/
COPY --chown=185 target/quarkus-app/*.jar ./
COPY --chown=185 target/quarkus-app/app/ app/

EXPOSE 8080
USER 185

ENTRYPOINT ["java", \
    "-Dquarkus.http.host=0.0.0.0", \
    "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
    "-jar", "quarkus-run.jar"]
