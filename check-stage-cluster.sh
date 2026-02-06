#!/bin/bash

echo "=== Fixing Stage API via Private Network ==="
echo ""

# Stage cluster private IPs (from terraform)
CONTROL_PLANE_IP="10.0.0.60"

echo "Step 1: Getting kubeconfig from control plane..."
ssh -o StrictHostKeyChecking=no -i ~/.ssh/onprem debian@$CONTROL_PLANE_IP "sudo cat /etc/rancher/k3s/k3s.yaml" > /tmp/kube-stage-temp.yaml

# Replace localhost with actual IP
sed "s/127.0.0.1/$CONTROL_PLANE_IP/g" /tmp/kube-stage-temp.yaml > ~/.kube/kube-stage-onprem

echo "✅ Kubeconfig saved to ~/.kube/kube-stage-onprem"
echo ""

export KUBECONFIG=~/.kube/kube-stage-onprem

echo "Step 2: Finding ciyex pods..."
kubectl get pods --all-namespaces | grep -i ciyex

echo ""
echo "Step 3: Finding ciyex deployments..."
kubectl get deployments --all-namespaces | grep -i ciyex

echo ""
echo "Step 4: Checking ciyex services..."
kubectl get svc --all-namespaces | grep -i ciyex

echo ""
echo "=== If you see ciyex resources above, tell me the namespace and deployment name ==="
echo "If NOT found, ciyex-api is not deployed to this cluster yet!"
