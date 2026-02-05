# Fix Stage Deployment Notifications to Teams

## Problem
Alertmanager is sending stage deployment alerts to PagerDuty instead of Teams.

## Solution
Update Alertmanager routing to send deployment notifications to Teams.

## Steps

### 1. Login to Azure
```bash
az login
# Or with service principal:
az login --service-principal \
  -u <AZURE_CLIENT_ID> \
  -p <AZURE_CLIENT_SECRET> \
  --tenant <AZURE_TENANT_ID>
```

### 2. Get AKS credentials
```bash
az aks get-credentials \
  --resource-group hiniKubeStage-rg \
  --name hiniKubeStage \
  --overwrite-existing
```

### 3. Backup current config
```bash
kubectl get configmap alertmanager-config -n monitoring -o yaml > alertmanager-backup.yaml
```

### 4. Edit Alertmanager config
```bash
kubectl edit configmap alertmanager-config -n monitoring
```

### 5. Update the route section
Find the `route:` section and add this route **BEFORE** the critical route:

```yaml
route:
  receiver: 'teams'
  group_by: ['alertname', 'cluster', 'service']
  routes:
    # ADD THIS FIRST - deployment notifications to Teams
    - match:
        alertname: 'DeploymentNotification'
      receiver: 'teams'
      repeat_interval: 1h
    
    # Keep existing critical route
    - match:
        severity: critical
      receiver: 'pagerduty'
    
    # Keep existing warning route
    - match:
        severity: warning
      receiver: 'teams'
```

### 6. Reload Alertmanager
```bash
kubectl rollout restart statefulset/alertmanager -n monitoring
```

### 7. Verify
```bash
kubectl logs -n monitoring alertmanager-0 | grep -i "reload"
```

## Quick Fix Script
Run this after logging into Azure:

```bash
chmod +x k8s/fix-alertmanager-stage.sh
./k8s/fix-alertmanager-stage.sh
```
