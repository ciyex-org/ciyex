#!/bin/bash

echo "=== Fixing Stage API (502 Bad Gateway) ==="
echo ""

# Connect to kube-stage cluster
export KUBECONFIG=~/.kube/kube-stage-onprem

echo "Step 1: Checking if kubeconfig exists..."
if [ ! -f "$KUBECONFIG" ]; then
  echo "❌ Kubeconfig not found at $KUBECONFIG"
  echo "Run: cd /home/dhivya/workspace/kube-terraform/environments/stage && terraform output"
  exit 1
fi

echo "✅ Using kubeconfig: $KUBECONFIG"
echo ""

# Check all namespaces for ciyex
echo "Step 2: Finding ciyex pods..."
kubectl get pods --all-namespaces | grep -i ciyex

echo ""
echo "Step 3: Checking deployments..."
kubectl get deployments --all-namespaces | grep -i ciyex

echo ""
echo "Which namespace is the ciyex-api in? (e.g., kube-stage, default, ciyex)"
read -r NAMESPACE

echo ""
echo "What is the deployment name? (e.g., ciyex-api, ciyex)"
read -r DEPLOYMENT

echo ""
echo "Step 4: Checking pod logs..."
kubectl logs -n "$NAMESPACE" -l app="$DEPLOYMENT" --tail=50

echo ""
echo "Step 5: Restarting deployment..."
kubectl rollout restart deployment/"$DEPLOYMENT" -n "$NAMESPACE"

echo ""
echo "Step 6: Watching rollout..."
kubectl rollout status deployment/"$DEPLOYMENT" -n "$NAMESPACE"

echo ""
echo "Step 7: Checking pods after restart..."
kubectl get pods -n "$NAMESPACE"

echo ""
echo "=== Done! Test: https://api-stage.ciyex.org ==="
