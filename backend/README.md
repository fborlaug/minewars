# backend

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Prerequisites

The backend requires a PostgreSQL database. Start it with Docker Compose from the **project root**:

```shell script
docker compose up -d
```

This starts a PostgreSQL 17 container with:

| Setting  | Value     |
|----------|-----------|
| Host     | localhost |
| Port     | 5433      |
| Database | minewars  |
| User     | minewars  |
| Password | minewars  |

Data is persisted in a Docker named volume (`pgdata`), so it survives restarts.

To stop PostgreSQL (data kept):

```shell script
docker compose down
```

To stop and **delete all data**:

```shell script
docker compose down -v
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Docker

Build the application first, then the container image:

```shell script
./mvnw package -DskipTests
docker build -t minewars-backend .
```

Run it (connecting to the local PostgreSQL on macOS):

```shell script
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5433/minewars \
  -e QUARKUS_DATASOURCE_USERNAME=minewars \
  -e QUARKUS_DATASOURCE_PASSWORD=minewars \
  minewars-backend
```

The container runs as a non-root user (UID 185) on `eclipse-temurin:25-jre`.

Health check endpoint: `GET /q/health`

## Related Guides

- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplify your persistence code for Hibernate ORM via the active record or the repository pattern
- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- SmallRye JWT ([guide](https://quarkus.io/guides/security-jwt)): Secure your applications with JSON Web Token
- SmallRye JWT Build ([guide](https://quarkus.io/guides/security-jwt-build)): Generate and sign JSON Web Tokens (companion to SmallRye JWT)
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- Flyway ([guide](https://quarkus.io/guides/flyway)): Handle your database schema migrations
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Health check endpoints for liveness and readiness probes
- jBCrypt: Password hashing using the bcrypt algorithm
