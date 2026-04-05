# ECS Exec — Connect to RDS via psql

Shell into the running Fargate container and use `psql` to query the production database.

## Prerequisites

Install the AWS SSM Session Manager plugin (one-time):

```sh
curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/mac_arm64/session-manager-plugin.pkg" -o "session-manager-plugin.pkg"
sudo installer -pkg session-manager-plugin.pkg -target /
sudo ln -s /usr/local/sessionmanagerplugin/bin/session-manager-plugin /usr/local/bin/session-manager-plugin
```

> See the [official install guide](https://docs.aws.amazon.com/systems-manager/latest/userguide/install-plugin-verify.html) for other platforms.

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
- The container runs as non-root (UID 185). You may need to run commands that require root via workarounds, or temporarily override the user in the task definition for debugging.
