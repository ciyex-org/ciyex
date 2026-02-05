#!/bin/bash
set -e

echo "=========================================="
echo "Testing Ciyex Versioning Strategy"
echo "=========================================="
echo ""

# Get base version from build.gradle
BASE_VERSION=$(grep "^version = " build.gradle | sed "s/version = '\(.*\)'/\1/")
BASE=$(echo "$BASE_VERSION" | sed -E 's/-(alpha|rc).*$//')
echo "📦 Base version from build.gradle: $BASE_VERSION"
echo "📦 Clean base version: $BASE"
echo ""

# Test 1: Alpha versioning (main branch)
echo "=========================================="
echo "TEST 1: Alpha Versioning (main branch)"
echo "=========================================="
LAST_ALPHA=$(git tag -l "${BASE}-alpha.*" --sort=-v:refname | head -n1)
if [ -n "$LAST_ALPHA" ]; then
  LAST_NUM=$(echo "$LAST_ALPHA" | sed "s/${BASE}-alpha.//")
  ALPHA_NUM=$((LAST_NUM + 1))
else
  ALPHA_NUM=1
fi
NEXT_ALPHA="${BASE}-alpha.${ALPHA_NUM}"
echo "✅ Last alpha tag: $LAST_ALPHA"
echo "✅ Next alpha version: $NEXT_ALPHA"
echo "   → Triggered by: Push to main branch"
echo "   → Environment: dev"
echo "   → Overlay: k8s/overlays/dev"
echo ""

# Test 2: RC versioning (promote from alpha)
echo "=========================================="
echo "TEST 2: RC Versioning (promote from alpha)"
echo "=========================================="
MAJOR=$(echo $BASE | cut -d. -f1)
MINOR=$(echo $BASE | cut -d. -f2)
PATCH=$(echo $BASE | cut -d. -f3 | cut -d- -f1)
RC_VERSION="${MAJOR}.${MINOR}.${PATCH}"

# Check if RC already exists
EXISTING_RC=$(git tag -l "${RC_VERSION}-rc" 2>/dev/null)
if [ -n "$EXISTING_RC" ]; then
  echo "⚠️  RC already exists: $EXISTING_RC"
  echo "   → Cannot promote again until version is bumped"
else
  echo "✅ RC version: ${RC_VERSION}-rc"
  echo "   → Promoted from: $LAST_ALPHA"
  echo "   → Triggered by: Manual workflow dispatch 'Promote to RC'"
  echo "   → Environment: stage"
  echo "   → Overlay: k8s/overlays/stage"
  echo "   → After promotion, build.gradle bumps to: ${MAJOR}.${MINOR}.$((PATCH + 1))"
fi
echo ""

# Test 3: GA versioning (promote from RC)
echo "=========================================="
echo "TEST 3: GA Versioning (promote from RC)"
echo "=========================================="
# Find oldest unpromoted RC
RC_TAG=""
for tag in $(git tag -l "*-rc" --sort=v:refname); do
  GA_VERSION=$(echo "$tag" | sed 's/-rc$//')
  if ! git rev-parse "v${GA_VERSION}" >/dev/null 2>&1; then
    RC_TAG="$tag"
    break
  fi
done

if [ -z "$RC_TAG" ]; then
  echo "⚠️  No unpromoted RC tags found"
  echo "   → All RCs have been promoted to GA"
  echo "   → Available RC tags:"
  git tag -l "*-rc" --sort=v:refname | sed 's/^/      /'
else
  GA_VERSION=$(echo "$RC_TAG" | sed 's/-rc$//')
  echo "✅ Oldest unpromoted RC: $RC_TAG"
  echo "✅ GA version: v${GA_VERSION}"
  echo "   → Triggered by: Manual workflow dispatch 'Promote to GA'"
  echo "   → Environment: prod"
  echo "   → Overlay: k8s/overlays/prod"
  echo "   → Creates GitHub release: v${GA_VERSION}"
fi
echo ""

# Test 4: Show all tags by type
echo "=========================================="
echo "TEST 4: Current Tag Summary"
echo "=========================================="
echo "Alpha tags (dev builds):"
git tag -l "*-alpha.*" --sort=-v:refname | head -5 | sed 's/^/  /'
echo ""
echo "RC tags (stage builds):"
RC_COUNT=$(git tag -l "*-rc" --sort=-v:refname | wc -l)
if [ "$RC_COUNT" -eq 0 ]; then
  echo "  (none yet)"
else
  git tag -l "*-rc" --sort=-v:refname | sed 's/^/  /'
fi
echo ""
echo "GA tags (prod releases):"
GA_COUNT=$(git tag -l "v*" --sort=-v:refname | wc -l)
if [ "$GA_COUNT" -eq 0 ]; then
  echo "  (none yet)"
else
  git tag -l "v*" --sort=-v:refname | head -5 | sed 's/^/  /'
fi
echo ""

# Test 5: Workflow simulation
echo "=========================================="
echo "TEST 5: Workflow Simulation"
echo "=========================================="
echo "Scenario: Complete release cycle"
echo ""
echo "Step 1: Developer pushes to main"
echo "  → CI builds: $NEXT_ALPHA"
echo "  → Deploys to: dev environment"
echo "  → Updates: k8s/overlays/dev/kustomization.yaml"
echo ""
echo "Step 2: QA approves, trigger 'Promote to RC'"
echo "  → Retags: $LAST_ALPHA → ${RC_VERSION}-rc"
echo "  → Deploys to: stage environment"
echo "  → Updates: k8s/overlays/stage/kustomization.yaml"
echo "  → Bumps build.gradle: ${MAJOR}.${MINOR}.$((PATCH + 1))"
echo ""
echo "Step 3: Stage testing passes, trigger 'Promote to GA'"
if [ -n "$RC_TAG" ]; then
  echo "  → Retags: $RC_TAG → v${GA_VERSION}"
else
  echo "  → Retags: ${RC_VERSION}-rc → v${RC_VERSION}"
fi
echo "  → Deploys to: prod environment"
echo "  → Updates: k8s/overlays/prod/kustomization.yaml"
echo "  → Creates: GitHub release"
echo ""

echo "=========================================="
echo "✅ Versioning test complete!"
echo "=========================================="
