# ECS Exec — Connect to RDS via psql

Shell into the running Fargate container and use `psql` to query the production database.

## Prerequisites

Install the AWS SSM Session Manager plugin (one-time):

```sh
brew install --cask session-manager-plugin
```

## Connect

```sh
# Find the cluster and task
CLUSTER=$(aws ecs list-clusters --query "clusterArns[?contains(@,'Minewars')]" --output text)
TASK=$(aws ecs list-tasks --cluster "$CLUSTER" --query "taskArns[0]" --output text)

# Shell into the container
aws ecs execute-command \
  --cluster "$CLUSTER" \
  --task "$TASK" \
  --interactive \
  --command "/bin/sh"
```

## Install psql (inside the container)

`psql` is not pre-installed in the container. Install it manually when needed:

```sh
apt-get update && apt-get install -y postgresql-client
```

> **Note:** This installation is temporary — it's lost when the container restarts or a new deployment happens. This is intentional to keep the Docker image small and the deploy fast.

## Start psql (inside the container)

The database credentials are injected as environment variables by ECS:

```sh
psql "host=$(echo $QUARKUS_DATASOURCE_JDBC_URL | sed 's|jdbc:postgresql://||;s|:5432/minewars||') \
      port=5432 dbname=minewars \
      user=$QUARKUS_DATASOURCE_USERNAME \
      password=$QUARKUS_DATASOURCE_PASSWORD"
```

## Useful psql commands

| Command | Description |
|---------|-------------|
| `\dt` | List tables |
| `\d player` | Describe the player table |
| `SELECT * FROM player;` | List all players |
| `SELECT count(*) FROM player;` | Count players |
| `\q` | Quit psql |

## Notes

- ECS Exec must be enabled on the Fargate service (`enableExecuteCommand: true` in CDK).
- The session ends when you type `exit` or the task is replaced during a deployment.
- The container runs as root during ECS Exec, so `apt-get install` works without `sudo`.
