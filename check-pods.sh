#!/bin/bash

echo "Checking Kubernetes Pods Status..."
echo "===================================="

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl not found. Install it first."
    exit 1
fi

echo ""
echo "Stage Environment Pods:"
kubectl get pods -n kube-stage | grep ciyex

echo ""
echo "Dev Environment Pods:"
kubectl get pods -n kube-dev | grep ciyex

echo ""
echo "===================================="
echo "To see logs of a crashed pod:"
echo "kubectl logs <pod-name> -n kube-stage --tail=50"
