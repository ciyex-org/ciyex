# Ciyex Deployment Guide

## Current Status
- ✅ Alpha build triggered: **0.0.1-alpha.21**
- 🔄 Building and deploying to **dev** environment
- ⏳ Waiting for GitHub Actions to complete

---

## How to Promote Through Environments

### Step 1: Monitor Alpha Build (In Progress)

**Check GitHub Actions:**
```
https://github.com/qiaben/ciyex/actions
```

Wait for the build to complete successfully. You should see:
- ✅ Build & Deploy (dev) - Success
- ✅ Docker image pushed: registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-alpha.21
- ✅ k8s/overlays/dev/kustomization.yaml updated
- ✅ Git tag created: 0.0.1-alpha.21

---

### Step 2: Promote to RC (Stage Environment)

**When to do this:** After dev testing passes

**Steps:**
1. Go to: https://github.com/qiaben/ciyex/actions
2. Click "CI/CD Pipeline" workflow
3. Click "Run workflow" button (top right)
4. Select:
   - Branch: **main**
   - Action: **Promote to RC**
5. Click "Run workflow"

**What happens:**
- Retags: 0.0.1-alpha.21 → 0.0.1-rc
- Deploys to: stage environment
- Updates: k8s/overlays/stage/kustomization.yaml
- Bumps: build.gradle to 0.0.2
- ArgoCD syncs to: kube-stage-us-east

**Result:** Version **0.0.1-rc** deployed to stage

---

### Step 3: Promote to GA (Production Environment)

**When to do this:** After stage testing passes

**Steps:**
1. Go to: https://github.com/qiaben/ciyex/actions
2. Click "CI/CD Pipeline" workflow
3. Click "Run workflow" button (top right)
4. Select:
   - Branch: **main**
   - Action: **Promote to GA**
5. Click "Run workflow"

**What happens:**
- Retags: 0.0.1-rc → v0.0.1
- Deploys to: prod environment
- Updates: k8s/overlays/prod/kustomization.yaml
- Creates: GitHub release v0.0.1
- ArgoCD syncs to: kube-prod-us-east

**Result:** Version **v0.0.1** deployed to production

---

## Alternative: Use GitHub CLI

If you have GitHub CLI installed and authenticated:

### Promote to RC
```bash
cd /home/dhivya/workspace/ciyex
gh workflow run ci-cd.yml -f action="Promote to RC"
```

### Promote to GA
```bash
cd /home/dhivya/workspace/ciyex
gh workflow run ci-cd.yml -f action="Promote to GA"
```

---

## Monitoring Deployments

### GitHub Actions
```
https://github.com/qiaben/ciyex/actions
```

### ArgoCD Applications
- **Dev**: ciyex-app-us-east-dev
- **Stage**: ciyex-app-us-east-stage  
- **Prod**: ciyex-app-us-east-prod

### Docker Registry
```bash
# Check images
curl -u admin:eAYAx1jdocf#WeZuy3i@LJjiz*3FqzVU \
  https://registry.apps-prod.us-east.in.hinisoft.com/v2/ciyex/tags/list
```

---

## Rollback Scenarios

### Skip RC (if stage testing fails)
1. Go to: https://github.com/qiaben/ciyex/actions
2. Click "CI/CD Pipeline" workflow
3. Click "Run workflow"
4. Select: **Skip RC**
5. This removes the oldest unpromoted RC tag

### Rollback in ArgoCD
1. Access ArgoCD UI
2. Select the application (dev/stage/prod)
3. Click "History and Rollback"
4. Select previous version
5. Click "Rollback"

---

## Quick Reference

| Environment | Version Format    | Example          | Trigger        |
|-------------|-------------------|------------------|----------------|
| Dev         | X.Y.Z-alpha.N     | 0.0.1-alpha.21   | Push to main   |
| Stage       | X.Y.Z-rc          | 0.0.1-rc         | Manual promote |
| Prod        | vX.Y.Z            | v0.0.1           | Manual promote |

---

## Next Steps

1. ✅ Wait for alpha build to complete (~5-10 minutes)
2. 🧪 Test in dev environment
3. 🚀 Promote to RC when ready
4. 🧪 Test in stage environment
5. 🚀 Promote to GA when ready
6. 🎉 Production deployment complete!
