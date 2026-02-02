# Registry Image Verification Report

**Date**: 2026-02-02  
**Registry**: registry.apps-prod.us-east.in.hinisoft.com  
**Repository**: ciyex

---

## ✅ Test Results: PASSED

All three deployment stages (Alpha, RC, GA) have been successfully verified in the production registry.

---

## Image Verification

### Alpha (Development)
```
Tag:    0.0.1-alpha.21
Status: ✅ EXISTS
Digest: sha256:66b18b112438ef8b1558239dde19ffdd34e6b3da912a6548015f9393e808bad3
URL:    registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-alpha.21
```

### RC (Stage)
```
Tag:    0.0.1-rc
Status: ✅ EXISTS
Digest: sha256:66b18b112438ef8b1558239dde19ffdd34e6b3da912a6548015f9393e808bad3
URL:    registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1-rc
```

### GA (Production)
```
Tag:    0.0.1
Status: ✅ EXISTS
Digest: sha256:66b18b112438ef8b1558239dde19ffdd34e6b3da912a6548015f9393e808bad3
URL:    registry.apps-prod.us-east.in.hinisoft.com/ciyex:0.0.1
```

### Environment Tags
```
dev:   ✅ EXISTS (points to latest alpha)
stage: ❌ NOT FOUND (workflow doesn't create this tag)
prod:  ❌ NOT FOUND (workflow doesn't create this tag)
```

---

## Digest Comparison

**Critical Finding**: All three versions share the **SAME digest**:
```
sha256:66b18b112438ef8b1558239dde19ffdd34e6b3da912a6548015f9393e808bad3
```

### What This Means

✅ **RC and GA are retagged versions of Alpha** (not rebuilt)
- This confirms the CI/CD pipeline is working correctly
- No code changes between environments
- Ensures consistency across dev → stage → prod
- Saves build time and registry storage

### Promotion Flow Verified

```
0.0.1-alpha.21 (built)
       ↓
   [retag]
       ↓
   0.0.1-rc (same image)
       ↓
   [retag]
       ↓
   0.0.1 (same image)
```

---

## All Available Tags

```
dev
0.0.1-rc
0.0.1-alpha.21
0.0.1-alpha.20
0.0.1-alpha.19
0.0.1-alpha.18
0.0.1-alpha.17
0.0.1-alpha.16
0.0.1-alpha.15
0.0.1-alpha.14
0.0.1-alpha.13
0.0.1
```

---

## Deployment Status

| Environment | Image Tag        | Overlay File                          | Cluster           |
|-------------|------------------|---------------------------------------|-------------------|
| Dev         | 0.0.1-alpha.21   | k8s/overlays/dev/kustomization.yaml   | kube-dev-us-east  |
| Stage       | 0.0.1-rc         | k8s/overlays/stage/kustomization.yaml | kube-stage-us-east|
| Prod        | 0.0.1            | k8s/overlays/prod/kustomization.yaml  | kube-prod-us-east |

---

## Test Commands Used

### List all tags
```bash
curl -s -u admin:PASSWORD \
  https://registry.apps-prod.us-east.in.hinisoft.com/v2/ciyex/tags/list | jq
```

### Check if tag exists
```bash
curl -s -o /dev/null -w "%{http_code}" -u admin:PASSWORD \
  https://registry.apps-prod.us-east.in.hinisoft.com/v2/ciyex/manifests/0.0.1-alpha.21
```

### Get image digest
```bash
curl -s -I -u admin:PASSWORD \
  https://registry.apps-prod.us-east.in.hinisoft.com/v2/ciyex/manifests/0.0.1-alpha.21 \
  -H "Accept: application/vnd.docker.distribution.manifest.v2+json" | \
  grep -i "docker-content-digest"
```

---

## Conclusion

✅ **All tests passed successfully**

1. Alpha image built and pushed: `0.0.1-alpha.21`
2. RC promotion successful: `0.0.1-rc` (retagged from alpha)
3. GA promotion successful: `0.0.1` (retagged from RC)
4. All three versions share identical digest (no rebuilds)
5. Images are available in production registry
6. Deployment pipeline working as designed

**Next Steps:**
- Monitor ArgoCD sync status in all three clusters
- Verify application health in each environment
- Review GitHub release notes: https://github.com/qiaben/ciyex/releases/tag/v0.0.1

---

**Report Generated**: 2026-02-02 06:15 UTC  
**Verified By**: Automated deployment pipeline
