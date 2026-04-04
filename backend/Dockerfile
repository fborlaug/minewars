FROM eclipse-temurin:25-jre

RUN groupadd -r -g 185 appuser && useradd -r -u 185 -g appuser appuser

WORKDIR /deployments

# Ordered for optimal Docker layer caching (least → most frequently changed)
COPY --chown=185 target/quarkus-app/lib/ lib/
COPY --chown=185 target/quarkus-app/*.jar ./
COPY --chown=185 target/quarkus-app/app/ app/
COPY --chown=185 target/quarkus-app/quarkus/ quarkus/

EXPOSE 8080
USER 185

ENTRYPOINT ["java", \
    "-Dquarkus.http.host=0.0.0.0", \
    "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
    "-jar", "quarkus-run.jar"]
