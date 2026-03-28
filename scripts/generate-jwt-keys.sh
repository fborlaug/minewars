#!/usr/bin/env bash
# Generates an RSA key pair and stores it in AWS Secrets Manager.
# Run once after the first `cdk deploy`, then restart the ECS service.
#
# Usage: ./scripts/generate-jwt-keys.sh

set -euo pipefail

SECRET_NAME="minewars/jwt-keys"
KEY_BITS=2048
TMPDIR=$(mktemp -d)

echo "Generating RSA-${KEY_BITS} key pair..."
openssl genpkey -algorithm RSA -out "${TMPDIR}/private.pem" -pkeyopt rsa_keygen_bits:${KEY_BITS} 2>/dev/null
openssl rsa -in "${TMPDIR}/private.pem" -pubout -out "${TMPDIR}/public.pem" 2>/dev/null

PRIVATE_KEY=$(cat "${TMPDIR}/private.pem")
PUBLIC_KEY=$(cat "${TMPDIR}/public.pem")

echo "Uploading to Secrets Manager (${SECRET_NAME})..."
aws secretsmanager put-secret-value \
  --secret-id "${SECRET_NAME}" \
  --secret-string "$(jq -n \
    --arg priv "$PRIVATE_KEY" \
    --arg pub "$PUBLIC_KEY" \
    '{privateKey: $priv, publicKey: $pub}')"

rm -rf "${TMPDIR}"

echo ""
echo "Done. Keys stored in Secrets Manager: ${SECRET_NAME}"
echo ""
echo "Restart the ECS service to pick up the new keys:"
echo "  aws ecs update-service --cluster <cluster> --service <service> --force-new-deployment"
echo ""
echo "Or redeploy via CDK:"
echo "  cd infra && cdk deploy"

