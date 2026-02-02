#!/bin/bash
# Fix for prod ArgoCD application cluster error

echo "The prod ArgoCD application is configured with an invalid cluster URL."
echo "Cluster configured: https://10.0.0.100:6443"
echo "This cluster does not exist in ArgoCD."
echo ""
echo "To fix this, run ONE of these commands:"
echo ""
echo "Option 1: Delete the prod application (recommended until prod cluster exists):"
echo "kubectl delete application ciyex-app-us-east-prod -n argocd"
echo ""
echo "Option 2: Update to use dev cluster temporarily:"
echo "kubectl patch application ciyex-app-us-east-prod -n argocd --type=json -p='[{\"op\":\"replace\",\"path\":\"/spec/destination/server\",\"value\":\"https://10.0.0.50:6443\"}]'"
echo ""
echo "Option 3: List available clusters in ArgoCD:"
echo "kubectl get secret -n argocd -l argocd.argoproj.io/secret-type=cluster -o jsonpath='{range .items[*]}{.metadata.annotations.argocd\\.argoproj\\.io/cluster-name}{\"\\t\"}{.data.server}{\"\\n\"}{end}' | while read name server; do echo \"Cluster: \$name\"; echo \"Server: \$(echo \$server | base64 -d)\"; echo; done"
