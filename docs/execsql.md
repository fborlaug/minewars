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

- `psql` is available because `postgresql-client` is installed in the Dockerfile.
- ECS Exec must be enabled on the Fargate service (`enableExecuteCommand: true` in CDK).
- The session ends when you type `exit` or the task is replaced during a deployment.

