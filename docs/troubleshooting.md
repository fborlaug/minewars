# Troubleshooting Guide

Debugging commands for infrastructure, production, and pipeline issues.

## Local Development

### Ports in use

```sh
# Check what's using a port
lsof -i :8080    # backend
lsof -i :5173    # frontend
lsof -i :5433    # PostgreSQL

# Kill a process on a port
kill -9 $(lsof -ti :8080)

# Kill all Quarkus/Maven processes
pkill -f "quarkus:dev"
pkill -f "mvnw"

# Show all listening ports
netstat -an | grep LISTEN

# Show listening ports with process names (macOS)
lsof -iTCP -sTCP:LISTEN -nP
```

### Database connectivity

```sh
# Is PostgreSQL running?
docker compose ps

# Can I reach it?
nc -zv localhost 5433

# Tail PostgreSQL logs
docker compose logs -f

# Shell into local PostgreSQL
docker compose exec postgres psql -U minewars

# Reset database (delete all data)
docker compose down -v && docker compose up -d
```

### Quarkus Dev Mode

```sh
# Start with hot reload
cd backend && ./mvnw quarkus:dev

# Dev UI (config, extensions, endpoints)
open http://localhost:8080/q/dev/

# Health check
curl -s localhost:8080/q/health | jq

# Press 'r' in the Quarkus terminal to re-run tests continuously
```

### Maven

```sh
# Run a single test class
./mvnw test -Dtest=AuthResourceTest

# Dependency tree (find conflicts)
./mvnw dependency:tree

# Verbose output for debugging build failures
./mvnw package -X

# Check effective POM (all resolved dependencies)
./mvnw help:effective-pom
```

### Maven build lifecycle

Each phase **includes all previous phases**, so `verify` runs everything above it:

```
compile → test → package → verify → install → deploy
```

| Phase | What it does | Plugin |
|-------|-------------|--------|
| `compile` | Compile `src/main/java` | compiler |
| `test` | Run unit tests (`*Test.java`) | **Surefire** |
| `package` | Build the JAR | jar |
| `verify` | Run integration tests (`*IT.java`) | **Failsafe** |
| `install` | Copy artifact to `~/.m2` | install |
| `deploy` | Upload artifact to remote repo | deploy |

```sh
./mvnw package    # compile + test + jar  (skips integration tests)
./mvnw verify     # compile + test + jar + integration tests
./mvnw test -DskipTests    # compile only — skip all tests
./mvnw package -Dmaven.test.skip=true   # skip test compilation AND execution
```

> Use `verify` in CI — it runs everything including `*IT.java` integration tests.

### Frontend / Node

```sh
# Type errors
npm run type-check

# Check what's installed vs expected
npm ls

# Check for vulnerable dependencies
npm audit

# Clear everything and reinstall
rm -rf node_modules package-lock.json && npm install

# Vite dev server with debug output
npx vite --debug
```

### JWT / Auth debugging

```sh
# Verify the private key is valid
openssl rsa -in backend/src/main/resources/privateKey.pem -check -noout

# Decode a JWT token (header + payload)
TOKEN="eyJ..."
echo "$TOKEN" | cut -d. -f1 | base64 -d 2>/dev/null | jq
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq
```

### Useful tools

| Tool | Install | What for |
|------|---------|----------|
| `jq` | `brew install jq` | JSON parsing |
| `gh` | `brew install gh` | GitHub CLI — view/rerun workflows |
| `httpie` | `brew install httpie` | Friendlier curl: `http POST :8080/api/auth/login username=test password=pass` |
| `psql` | `brew install libpq && brew link --force libpq` | Connect to PostgreSQL without Docker |
| `watch` | `brew install watch` | Repeat command: `watch -n 5 'curl -s localhost:8080/q/health \| jq .status'` |

### Pipes, redirection & streams

Every process has three standard streams:

| Stream | File descriptor | Default destination |
|--------|-----------------|---------------------|
| **stdin** | `0` | keyboard |
| **stdout** | `1` | terminal |
| **stderr** | `2` | terminal |

```sh
# Pipe (|) — send stdout of one command into stdin of the next
cat logs.txt | grep ERROR | wc -l

# Redirect stdout to a file (overwrite)
curl -s localhost:8080/q/health > health.json

# Redirect stdout to a file (append)
./mvnw package >> build.log

# Redirect stderr to a file
./mvnw package 2> errors.log

# Redirect both stdout and stderr to the same file
./mvnw package > build.log 2>&1

# Discard stderr (e.g., suppress warnings)
curl -s https://example.com 2>/dev/null

# Pipe stderr along with stdout (merge first, then pipe)
./mvnw package 2>&1 | grep -i error

# Send input from a file via stdin
psql -U minewars < db/migration/V1__init.sql

# Here-string — pass a literal string as stdin
grep "ERROR" <<< "INFO ok ERROR fail INFO done"

# tee — write to a file AND still see the output in the terminal
./mvnw package 2>&1 | tee build.log

# tee with append (keep previous content)
curl -s localhost:8080/q/health | tee -a health.log

# Combine: run tests, merge streams, save full log, but only show failures on screen
./mvnw test 2>&1 | tee test-output.log | grep -E "FAIL|ERROR"
```

### Shell error handling (set)

Control how the shell reacts to errors in scripts and multi-line CI steps:

| Option | Meaning |
|--------|---------|
| `set -e` | Exit immediately if any command returns non-zero |
| `set -u` | Error on undefined variables instead of treating them as empty |
| `set -o pipefail` | A pipeline fails if **any** command in it fails (not just the last) |
| `set -euo pipefail` | All three — the "strict mode" best practice |

```sh
# Without set -e — second command runs even if first fails
openssl genpkey -algorithm INVALID -out key.pem
echo "this still runs"

# With set -e — script stops at the first failure
set -e
openssl genpkey -algorithm INVALID -out key.pem
echo "this never runs"

# Strict mode (recommended for scripts)
#!/usr/bin/env bash
set -euo pipefail

# Pipefail example — without it, this succeeds (grep returns 0)
false | grep "x"     # exit code 1 only with pipefail

# Allow a specific command to fail (override set -e temporarily)
set -e
some_command || true   # continues even if some_command fails
```

> **GitHub Actions note:** each `run:` step uses `set -e` by default for single
> commands. For multi-line `run: |` blocks, add `set -euo pipefail` at the top.

### Redirection quick reference

| Syntax | Meaning | Example |
|--------|---------|---------|
| `>/dev/null` | Discard stdout | `curl -s url >/dev/null` |
| `2>/dev/null` | Discard stderr (warnings/errors) | `openssl genpkey ... 2>/dev/null` |
| `&>/dev/null` | Discard both stdout and stderr | `some_command &>/dev/null` |
| `2>&1` | Merge stderr into stdout | `./mvnw package 2>&1 \| grep ERROR` |
| `> file` | Write stdout to file (overwrite) | `curl -s url > out.json` |
| `>> file` | Append stdout to file | `echo "done" >> build.log` |
| `2> file` | Write stderr to file | `./mvnw package 2> errors.log` |
| `> file 2>&1` | Write both streams to file | `./mvnw package > build.log 2>&1` |

> **Caveat with `2>/dev/null`:** if a command fails, the error message is also
> discarded — you'll see a non-zero exit code but no explanation of *why* it failed.

### YAML block scalar indicators

Used in CI workflows (`ci.yml`, `deploy.yml`) and CDK config to write multi-line strings:

| Indicator | Newlines | Trailing newline |
|-----------|----------|------------------|
| `\|` (literal) | Preserved as-is | Kept |
| `\|-` (literal, strip) | Preserved as-is | Stripped |
| `>` (folded) | Replaced with spaces | Kept |
| `>-` (folded, strip) | Replaced with spaces | Stripped |

```yaml
# >- (folded + strip) — most common in CI for multi-line single commands
options: >-
  --health-cmd pg_isready
  --health-interval 10s
  --health-timeout 5s

# Produces: "--health-cmd pg_isready --health-interval 10s --health-timeout 5s"

# | (literal) — preserves line breaks (useful for inline scripts)
run: |
  set -euo pipefail
  echo "line 1"
  echo "line 2"

# Produces two separate lines — each echo runs independently
```

| Symbol | Mnemonic |
|--------|----------|
| `\|` | The pipe is **straight** — lines stay as they are |
| `>` | The arrow **folds** lines together |
| `-` suffix | **Strips** the trailing newline |
| `+` suffix | **Keeps** all trailing newlines |

## AWS CLI / SSO

### Login & profile setup

```sh
# Configure SSO (first time — creates a named profile)
aws configure sso
# Follow the prompts: SSO start URL, region, account, role, profile name

# Log in with an existing SSO profile
aws sso login --profile minewars

# Set a default profile for the current shell (avoids --profile on every command)
export AWS_PROFILE=minewars

# Make it permanent (add to ~/.zshrc)
echo 'export AWS_PROFILE=minewars' >> ~/.zshrc

# Verify you're authenticated and using the right account
aws sts get-caller-identity
```

### Credentials & session troubleshooting

```sh
# Check which profile/credentials are active
aws configure list

# List all configured profiles
aws configure list-profiles

# Force re-login (e.g., after token expired)
aws sso login --profile minewars

# Clear cached SSO tokens (nuclear option)
rm -rf ~/.aws/sso/cache/*
aws sso login --profile minewars

# Check when your session token expires
aws sts get-caller-identity 2>&1 || echo "Session expired — run: aws sso login"
```

### Common config files

| File | Purpose |
|------|---------|
| `~/.aws/config` | Profiles, SSO settings, default region |
| `~/.aws/credentials` | Static access keys (not used with SSO) |
| `~/.aws/sso/cache/` | Cached SSO session tokens |

**Example `~/.aws/config` profile:**

```ini
[profile minewars]
sso_session = my-sso
sso_account_id = 123456789012
sso_role_name = AdministratorAccess
region = eu-north-1
output = json

[sso-session my-sso]
sso_start_url = https://my-org.awsapps.com/start
sso_region = eu-north-1
sso_registration_scopes = sso:account:access
```

**Common errors we've seen:**

| Error | Cause | Fix |
|-------|-------|-----|
| `The SSO session associated with this profile has expired` | SSO token expired (typically after 8 h) | `aws sso login --profile minewars` |
| `Unable to locate credentials` | No profile set and no env vars | `export AWS_PROFILE=minewars` or pass `--profile` |
| `An error occurred (ExpiredTokenException)` | Cached token is stale | Clear cache and re-login (see above) |
| `The config profile (xxx) could not be found` | Typo or missing profile in `~/.aws/config` | Run `aws configure list-profiles` and check spelling |

## ECS / Fargate

### Container won't start

```sh
# Check why the task failed
CLUSTER=$(aws ecs list-clusters --query "clusterArns[?contains(@,'Minewars')]" --output text)
aws ecs list-tasks --cluster "$CLUSTER" --desired-status STOPPED --output text

# Get the stop reason (e.g., "exec format error", "secret retrieval failed")
TASK_ARN=$(aws ecs list-tasks --cluster "$CLUSTER" --desired-status STOPPED --query "taskArns[0]" --output text)
aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK_ARN" \
  --query "tasks[0].{status:lastStatus,reason:stoppedReason,stopCode:stopCode}" --output table
```

**Common errors we've seen:**

| Error | Cause | Fix |
|-------|-------|-----|
| `exec format error` | Docker image built for wrong CPU architecture (ARM vs x86) | Set `Platform.LINUX_ARM64` in `fromAsset` + `runtimePlatform` in ECS |
| `unable to pull secrets` / `invalid character '+'` | Secrets Manager value isn't valid JSON | Initialize secret with valid JSON placeholder |
| `SRJWT05028: Signing key can not be created` | SmallRye JWT can't parse the key | Use `%dev.` prefix on `*.location` properties so prod uses env vars |
| `MalformedURLException: no protocol: privateKey.pem` | Classpath key resolution fails in fast-jar | Add `classpath:` prefix to key locations |

### View container logs

```sh
# Tail logs live
aws logs tail /ecs/minewars --follow

# Last 30 minutes
aws logs tail /ecs/minewars --since 30m

# Search for errors
aws logs tail /ecs/minewars --since 1h --filter-pattern "ERROR"
```

### Shell into running container (ECS Exec)

```sh
CLUSTER=$(aws ecs list-clusters --query "clusterArns[?contains(@,'Minewars')]" --output text)
TASK=$(aws ecs list-tasks --cluster "$CLUSTER" --query "taskArns[0]" --output text)
aws ecs execute-command --cluster "$CLUSTER" --task "$TASK" --interactive --command "/bin/sh"

# Inside the container — check env vars
env | grep QUARKUS
env | grep JWT
```

### Force restart (new deployment)

```sh
CLUSTER=$(aws ecs list-clusters --query "clusterArns[?contains(@,'Minewars')]" --output text)
SERVICE=$(aws ecs list-services --cluster "$CLUSTER" --query "serviceArns[0]" --output text)
aws ecs update-service --cluster "$CLUSTER" --service "$SERVICE" --force-new-deployment
```

## CloudFormation / CDK

### Deployment fails

```sh
# Check stack status
aws cloudformation describe-stacks --stack-name MinewarsStack \
  --query "Stacks[0].StackStatus" --output text

# See what failed and why
aws cloudformation describe-stack-events --stack-name MinewarsStack \
  --query "StackEvents[?ResourceStatus=='CREATE_FAILED' || ResourceStatus=='UPDATE_FAILED'].[LogicalResourceId,ResourceStatusReason]" \
  --output table

# Stuck change set — list and delete
aws cloudformation list-change-sets --stack-name MinewarsStack \
  --query "Summaries[?Status!='DELETE_COMPLETE'].[ChangeSetName,Status]" --output table
```

**Common errors we've seen:**

| Error | Cause | Fix |
|-------|-------|-----|
| `Character sets beyond ASCII` | Em dash (—) in security group description | Use plain ASCII only |
| `Cannot delete ChangeSet in status CREATE_IN_PROGRESS` | Concurrent deploys | Add `concurrency` to deploy.yml, wait and retry |

### Preview changes before deploying

```sh
cd infra && npx cdk diff    # show what will change
cd infra && npx cdk synth   # generate the full CloudFormation template
```

## Secrets Manager

### Check secret contents

```sh
# List all secrets
aws secretsmanager list-secrets --no-cli-pager

# Read DB credentials
aws secretsmanager get-secret-value \
  --secret-id MinewarsStackDatabaseSecret-xxx \
  --query SecretString --output text | python3 -m json.tool

# Read JWT keys (verify they're not PLACEHOLDER)
aws secretsmanager get-secret-value \
  --secret-id minewars/jwt-keys \
  --query SecretString --output text | python3 -c "
import json, sys
d = json.load(sys.stdin)
for k in d:
    v = d[k]
    print(f'{k}: {v[:40]}...' if len(v) > 40 else f'{k}: {v}')
"
```

## GitHub Actions / CI/CD

### OIDC authentication fails

```sh
# Verify the OIDC provider exists
aws iam list-open-id-connect-providers --no-cli-pager

# Check the deploy role trust policy
ROLE_NAME=$(aws cloudformation describe-stacks --stack-name MinewarsStack \
  --query "Stacks[0].Outputs[?OutputKey=='DeployRoleArn'].OutputValue" --output text | awk -F/ '{print $NF}')
aws iam get-role --role-name "$ROLE_NAME" --no-cli-pager \
  --query "Role.AssumeRolePolicyDocument" | python3 -m json.tool
```

**Common errors we've seen:**

| Error | Cause | Fix |
|-------|-------|-----|
| `Not authorized to perform sts:AssumeRoleWithWebIdentity` | `AWS_ROLE_ARN` secret missing/wrong, or audience mismatch | Verify secret matches CDK output, add `audience: sts.amazonaws.com` |
| `exec format error` in deploy | ARM64 Docker build on x86 runner with `RUN` commands | Remove `RUN` from Dockerfile or add `docker/setup-qemu-action` |
| `mvnw: cannot open .mvn/wrapper/maven-wrapper.properties` | `.mvn/` directory gitignored | Remove the ignore rule, commit the wrapper files |

### Re-run a failed workflow

```sh
# List recent workflow runs
gh run list --limit 5

# Re-run a failed run
gh run rerun <run-id>

# View logs of a failed run
gh run view <run-id> --log-failed
```

> Requires: `brew install gh` (GitHub CLI)

## RDS / Database

### Check connectivity from ECS

```sh
# Shell into container first (see ECS Exec above), then:
apt-get update && apt-get install -y postgresql-client
psql "host=$(echo $QUARKUS_DATASOURCE_JDBC_URL | sed 's|jdbc:postgresql://||;s|:5432/minewars||') \
      port=5432 dbname=minewars \
      user=$QUARKUS_DATASOURCE_USERNAME \
      password=$QUARKUS_DATASOURCE_PASSWORD"
```

### Can't connect from local machine

RDS is in an **isolated subnet** — this is by design. You cannot connect directly. Use ECS Exec (above) or add a bastion host.

## CloudFront

### API returns unexpected response

```sh
# Test directly against ALB (bypass CloudFront)
ALB=$(aws cloudformation describe-stacks --stack-name MinewarsStack \
  --query "Stacks[0].Outputs[?OutputKey=='BackendUrl'].OutputValue" --output text)
curl -s "$ALB/q/health" | python3 -m json.tool
curl -s "$ALB/api/hello" -H "Authorization: Bearer <token>"

# If ALB works but CloudFront doesn't → CloudFront is stripping headers or caching
```

**Common errors we've seen:**

| Error | Cause | Fix |
|-------|-------|-----|
| 401 on `/api/hello` through CloudFront | Stale JWT from before key rotation | Logout and login again |
| 401 with valid fresh token | `mp.jwt.verify.publickey.location` overriding Secrets Manager key | Use `%dev.` prefix on `*.location` properties |

## Quick Health Check (everything at once)

```sh
CF="https://d3um3coa2we7s5.cloudfront.net"

echo "=== Stack ==="
aws cloudformation describe-stacks --stack-name MinewarsStack \
  --query "Stacks[0].StackStatus" --output text

echo "=== ECS ==="
CLUSTER=$(aws ecs list-clusters --query "clusterArns[?contains(@,'Minewars')]" --output text)
aws ecs describe-services --cluster "$CLUSTER" \
  --services $(aws ecs list-services --cluster "$CLUSTER" --query "serviceArns[0]" --output text) \
  --query "services[0].{running:runningCount,desired:desiredCount,status:status}" --output table

echo "=== Health ==="
curl -s "$CF/q/health" | python3 -m json.tool

echo "=== Frontend ==="
curl -s -o /dev/null -w "HTTP %{http_code}" "$CF"
```

