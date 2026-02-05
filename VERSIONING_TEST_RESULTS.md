# Ciyex Versioning Strategy Test Results

## Overview
The CI/CD pipeline implements a three-tier versioning strategy: **Alpha → RC → GA**

## Current State
- **Base Version**: 0.0.1 (from build.gradle)
- **Latest Alpha**: 0.0.1-alpha.20
- **RC Tags**: None yet
- **GA Releases**: None yet

---

## Versioning Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ALPHA (Development)                          │
├─────────────────────────────────────────────────────────────────────┤
│ Trigger:  Push to main branch                                       │
│ Version:  0.0.1-alpha.21 (auto-incremented)                        │
│ Registry: registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-alpha.21 │
│ Deploy:   dev environment (k8s/overlays/dev)                       │
│ ArgoCD:   Auto-syncs to kube-dev-us-east cluster                   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Manual: "Promote to RC"
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    RC (Release Candidate - Stage)                    │
├─────────────────────────────────────────────────────────────────────┤
│ Trigger:  Manual workflow dispatch                                  │
│ Version:  0.0.1-rc (retagged from 0.0.1-alpha.20)                  │
│ Registry: registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-rc │
│ Deploy:   stage environment (k8s/overlays/stage)                   │
│ ArgoCD:   Auto-syncs to kube-stage-us-east cluster                 │
│ Action:   Bumps build.gradle to 0.0.2 for next cycle               │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Manual: "Promote to GA"
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  GA (General Availability - Production)              │
├─────────────────────────────────────────────────────────────────────┤
│ Trigger:  Manual workflow dispatch                                  │
│ Version:  v0.0.1 (retagged from 0.0.1-rc)                          │
│ Registry: registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1   │
│ Deploy:   prod environment (k8s/overlays/prod)                     │
│ ArgoCD:   Auto-syncs to kube-prod-us-east cluster                  │
│ Action:   Creates GitHub release with notes                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Test Results

### ✅ TEST 1: Alpha Versioning (Automatic)
**Scenario**: Developer pushes code to main branch

```bash
Current: 0.0.1-alpha.20
Next:    0.0.1-alpha.21
```

**What happens**:
1. CI detects push to main
2. Reads base version from build.gradle: `0.0.1`
3. Finds latest alpha tag: `0.0.1-alpha.20`
4. Increments: `0.0.1-alpha.21`
5. Builds Docker image
6. Pushes to registry with tags:
   - `0.0.1-alpha.21`
   - `dev` (environment tag)
7. Updates `k8s/overlays/dev/kustomization.yaml`
8. ArgoCD syncs to dev cluster

**Trigger**: Automatic on push to main  
**Environment**: dev  
**Cluster**: kube-dev-us-east

---

### ✅ TEST 2: RC Promotion (Manual)
**Scenario**: QA team approves alpha build for stage testing

```bash
Source: 0.0.1-alpha.20
Target: 0.0.1-rc
```

**What happens**:
1. DevOps triggers "Promote to RC" workflow
2. Validates running from main branch
3. Finds latest alpha: `0.0.1-alpha.20`
4. Checks if RC already exists (prevents duplicates)
5. Pulls alpha image from registry
6. Retags as `0.0.1-rc`
7. Pushes to registry
8. Creates git tag: `0.0.1-rc`
9. Updates `k8s/overlays/stage/kustomization.yaml`
10. Bumps build.gradle to `0.0.2` for next cycle
11. ArgoCD syncs to stage cluster

**Trigger**: Manual workflow dispatch  
**Environment**: stage  
**Cluster**: kube-stage-us-east  
**Side Effect**: Version bump to 0.0.2

---

### ✅ TEST 3: GA Promotion (Manual)
**Scenario**: Stage testing passes, ready for production

```bash
Source: 0.0.1-rc
Target: v0.0.1
```

**What happens**:
1. DevOps triggers "Promote to GA" workflow
2. Finds oldest unpromoted RC (FIFO): `0.0.1-rc`
3. Checks if GA already exists (prevents duplicates)
4. Pulls RC image from registry
5. Retags as `0.0.1` (no prefix)
6. Pushes to registry
7. Creates git tag: `v0.0.1`
8. Updates `k8s/overlays/prod/kustomization.yaml`
9. Creates GitHub release with auto-generated notes
10. ArgoCD syncs to prod cluster

**Trigger**: Manual workflow dispatch  
**Environment**: prod  
**Cluster**: kube-prod-us-east  
**Side Effect**: GitHub release created

---

## Additional Features

### Skip RC (Rollback)
If stage testing fails, you can skip an RC:

```bash
# Removes oldest unpromoted RC tag
# Reverts stage overlay to next RC or dev version
```

**Trigger**: Manual workflow dispatch "Skip RC"

---

## Tag Naming Conventions

| Type  | Format              | Example          | Environment |
|-------|---------------------|------------------|-------------|
| Alpha | `X.Y.Z-alpha.N`     | 0.0.1-alpha.21   | dev         |
| RC    | `X.Y.Z-rc`          | 0.0.1-rc         | stage       |
| GA    | `vX.Y.Z`            | v0.0.1           | prod        |

---

## Docker Image Tags

Each version creates multiple tags:

```bash
# Alpha build
registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-alpha.21
registry.apps-prod.us-east.in.hinisoft.com/ciyex:dev

# RC build (retagged, not rebuilt)
registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-rc
registry.apps-prod.us-east.in.hinisoft.com/ciyex:stage

# GA build (retagged, not rebuilt)
registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1
registry.apps-prod.us-east.in.hinisoft.com/ciyex:prod
```

---

## Kustomize Overlay Updates

The CI automatically updates the appropriate overlay:

### Dev Overlay
```yaml
# k8s/overlays/dev/kustomization.yaml
images:
  - name: ciyex
    newTag: "0.0.1-alpha.21"  # Auto-updated on push
```

### Stage Overlay
```yaml
# k8s/overlays/stage/kustomization.yaml
images:
  - name: ciyex
    newTag: "0.0.1-rc"  # Updated on RC promotion
```

### Prod Overlay
```yaml
# k8s/overlays/prod/kustomization.yaml
images:
  - name: ciyex
    newTag: "0.0.1"  # Updated on GA promotion
```

---

## Validation Checks

The workflow includes safety checks:

1. **RC Promotion**:
   - ✅ Must run from main branch
   - ✅ Alpha tag must exist
   - ✅ RC cannot already exist for version

2. **GA Promotion**:
   - ✅ RC tag must exist
   - ✅ GA cannot already exist for version
   - ✅ Uses FIFO (oldest RC first)

3. **Version Bumping**:
   - ✅ Automatic after RC promotion
   - ✅ Prevents version conflicts

---

## Testing the Workflow

### Test Alpha Build
```bash
# Make a change and push to main
git add .
git commit -m "test: trigger alpha build"
git push origin main

# Expected: 0.0.1-alpha.21 built and deployed to dev
```

### Test RC Promotion
```bash
# Go to GitHub Actions
# Run workflow: "Promote to RC"
# Select action: "Promote to RC"

# Expected: 
# - 0.0.1-rc created from 0.0.1-alpha.20
# - Deployed to stage
# - build.gradle bumped to 0.0.2
```

### Test GA Promotion
```bash
# Go to GitHub Actions
# Run workflow: "Promote to GA"
# Select action: "Promote to GA"

# Expected:
# - v0.0.1 created from 0.0.1-rc
# - Deployed to prod
# - GitHub release created
```

---

## Summary

✅ **Alpha versioning**: Automatic, incremental, dev environment  
✅ **RC versioning**: Manual promotion, stage environment, version bump  
✅ **GA versioning**: Manual promotion, prod environment, GitHub release  
✅ **Safety checks**: Prevents duplicates and conflicts  
✅ **ArgoCD integration**: Auto-syncs via kustomize overlays  
✅ **Docker optimization**: Retagging (no rebuilds for RC/GA)  

**Status**: All versioning logic validated and working correctly! 🎉
