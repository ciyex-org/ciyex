#!/bin/bash
# Check and fix Alertmanager configuration for kube-stage-onprem

echo "Checking Alertmanager configuration in kube-stage-onprem..."

# Get alertmanager config
kubectl get configmap alertmanager-config -n monitoring -o yaml

echo -e "\n---\nTo fix Teams notifications, update the alertmanager config:\n"

cat <<'EOF'
kubectl edit configmap alertmanager-config -n monitoring

# Change the route to send alerts to Teams instead of PagerDuty:

route:
  receiver: 'teams'
  routes:
    - match:
        severity: critical
      receiver: 'teams'  # Change from 'pagerduty' to 'teams'
    - match:
        severity: warning
      receiver: 'teams'

receivers:
  - name: 'teams'
    webhook_configs:
      - url: 'YOUR_TEAMS_WEBHOOK_URL'
        send_resolved: true

# Then reload alertmanager:
kubectl rollout restart statefulset/alertmanager -n monitoring
EOF
