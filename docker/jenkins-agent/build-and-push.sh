#!/usr/bin/env bash
set -euo pipefail
# Build and push the ciyex Jenkins agent image to ACR.
# Usage:
#  ACR_NAME=hinikubestageacr.azurecr.io TAG=v1.0.0 ./docker/jenkins-agent/build-and-push.sh
# Note: this script expects either the Azure CLI (`az`) to be available and logged in,
# or environment variables ACR_USERNAME and ACR_PASSWORD to be set for docker login.

ACR_NAME=${ACR_NAME:-}
TAG=${TAG:-v1.0.0}
IMAGE_NAME=ciyex-jenkins-agent

if [ -z "$ACR_NAME" ]; then
  echo "Error: ACR_NAME not set. Example: ACR_NAME=hinikubestageacr.azurecr.io"
  exit 1
fi

REGISTRY="$ACR_NAME"
FULL_IMAGE="$REGISTRY/$IMAGE_NAME:$TAG"

echo "Building image: $FULL_IMAGE"
docker build -t "$FULL_IMAGE" ./docker/jenkins-agent

# Try to login via Azure CLI first
if command -v az >/dev/null 2>&1; then
  echo "Logging into ACR using Azure CLI"
  ACR_REGISTRY_SHORT="$(echo $ACR_NAME | cut -d'.' -f1)"
  az acr login --name "$ACR_REGISTRY_SHORT"
else
  echo "Azure CLI not available; falling back to docker login with ACR_USERNAME/ACR_PASSWORD env vars"
  if [ -z "${ACR_USERNAME:-}" ] || [ -z "${ACR_PASSWORD:-}" ]; then
    echo "Error: ACR_USERNAME and ACR_PASSWORD must be set if Azure CLI is not available"
    exit 1
  fi
  echo "$ACR_PASSWORD" | docker login "$ACR_NAME" -u "$ACR_USERNAME" --password-stdin
fi

echo "Pushing image: $FULL_IMAGE"
docker push "$FULL_IMAGE"

echo "Success: $FULL_IMAGE pushed"
