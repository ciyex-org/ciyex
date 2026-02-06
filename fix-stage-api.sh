#!/bin/bash

echo "=== Fixing Stage API (502 Bad Gateway) ==="
echo ""

# Step 1: Login to Azure
echo "Step 1: Login to Azure..."
az login

# Step 2: Get AKS credentials (update with your actual values)
echo ""
echo "Step 2: Connect to AKS cluster..."
echo "Enter your resource group name:"
read -r RESOURCE_GROUP
echo "Enter your AKS cluster name:"
read -r CLUSTER_NAME

az aks get-credentials --resource-group "$RESOURCE_GROUP" --name "$CLUSTER_NAME" --overwrite-existing

# Step 3: Check stage pods
echo ""
echo "Step 3: Checking stage pods..."
kubectl get pods -n kube-stage

# Step 4: Get deployment name
echo ""
echo "Available deployments in kube-stage:"
kubectl get deployments -n kube-stage

echo ""
echo "Enter the deployment name to restart (e.g., ciyex, ciyex-api):"
read -r DEPLOYMENT_NAME

# Step 5: Restart deployment
echo ""
echo "Step 5: Restarting deployment..."
kubectl rollout restart deployment/"$DEPLOYMENT_NAME" -n kube-stage

# Step 6: Watch rollout status
echo ""
echo "Step 6: Watching rollout status..."
kubectl rollout status deployment/"$DEPLOYMENT_NAME" -n kube-stage

# Step 7: Check pods again
echo ""
echo "Step 7: Checking pods after restart..."
kubectl get pods -n kube-stage

echo ""
echo "=== Done! Wait 30 seconds and test: https://api-stage.ciyex.org ==="
