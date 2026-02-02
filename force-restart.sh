#!/bin/bash
# Force rollout by adding restart annotation

cat > /tmp/restart-patch.yaml <<EOF
spec:
  template:
    metadata:
      annotations:
        kubectl.kubernetes.io/restartedAt: "$(date +%Y-%m-%dT%H:%M:%S%z)"
EOF

echo "To force restart the deployment, run:"
echo "kubectl patch deployment ciyex -n ciyex --patch-file /tmp/restart-patch.yaml"
echo ""
echo "Or manually delete the pod:"
echo "kubectl delete pod ciyex-57777dbcc6-n6frl -n ciyex"
