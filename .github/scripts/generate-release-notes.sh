#!/bin/bash
set -e

# Generate release notes for RC or GA releases
# Usage: generate-release-notes.sh <version> <release_type> <prev_tag> <app_name>
# Example: generate-release-notes.sh 1.0.0 ga v0.9.0 ciyex-hapi-fhir

VERSION="$1"
RELEASE_TYPE="$2"
PREV_TAG="$3"
APP_NAME="${4:-ciyex-hapi-fhir}"

if [ -z "$VERSION" ] || [ -z "$RELEASE_TYPE" ]; then
    echo "Usage: $0 <version> <release_type> <prev_tag> [app_name]"
    exit 1
fi

RELEASE_DATE=$(date +"%Y-%m-%d")
OUTPUT_FILE="/tmp/release_notes.rst"

# Determine release type label
if [ "$RELEASE_TYPE" = "rc" ]; then
    TYPE_LABEL="Release Candidate"
    DISPLAY_VERSION="${VERSION}-rc"
else
    TYPE_LABEL="General Availability Release"
    DISPLAY_VERSION="${VERSION}"
fi

# Start building release notes
cat > "$OUTPUT_FILE" << EOF

${DISPLAY_VERSION} (${RELEASE_DATE})
----------------------------------------

**${TYPE_LABEL}**

Pull Requests
~~~~~~~~~~~~~

EOF

# Get PR numbers from merge commits
if [ -n "$PREV_TAG" ] && git rev-parse "$PREV_TAG" >/dev/null 2>&1; then
    PR_LIST=$(git log "${PREV_TAG}..HEAD" --oneline --grep="Merge pull request" 2>/dev/null | sed -n 's/.*#\([0-9]*\).*/\1/p' | sort -u || echo "")
else
    PR_LIST=$(git log --oneline --grep="Merge pull request" 2>/dev/null | sed -n 's/.*#\([0-9]*\).*/\1/p' | sort -u | head -20 || echo "")
fi

PR_FOUND=false
if [ -n "$PR_LIST" ]; then
    for PR in $PR_LIST; do
        if command -v gh &> /dev/null; then
            PR_URL=$(gh pr view "$PR" --json url --jq '.url' 2>/dev/null || echo "")
            PR_TITLE=$(gh pr view "$PR" --json title --jq '.title' 2>/dev/null || echo "")
            PR_AUTHOR=$(gh pr view "$PR" --json author --jq '.author.login' 2>/dev/null || echo "")
            
            if [ -n "$PR_URL" ] && [ -n "$PR_TITLE" ]; then
                echo "* \`#${PR} <${PR_URL}>\`_ - ${PR_TITLE} (@${PR_AUTHOR})" >> "$OUTPUT_FILE"
                PR_FOUND=true
            fi
        fi
    done
fi

if [ "$PR_FOUND" = false ]; then
    echo "* No pull requests found for this release" >> "$OUTPUT_FILE"
fi

# Get commits that aren't from PRs
echo "" >> "$OUTPUT_FILE"
echo "Commits" >> "$OUTPUT_FILE"
echo "~~~~~~~" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

if [ -n "$PREV_TAG" ] && git rev-parse "$PREV_TAG" >/dev/null 2>&1; then
    COMMITS=$(git log "${PREV_TAG}..HEAD" --oneline --no-merges --format="* %s" 2>/dev/null | grep -v "\[skip ci\]" | head -20 || echo "")
else
    COMMITS=$(git log --oneline --no-merges --format="* %s" 2>/dev/null | grep -v "\[skip ci\]" | head -20 || echo "")
fi

if [ -n "$COMMITS" ]; then
    echo "$COMMITS" >> "$OUTPUT_FILE"
else
    echo "* No direct commits found" >> "$OUTPUT_FILE"
fi

echo "" >> "$OUTPUT_FILE"

echo "✅ Generated release notes for ${DISPLAY_VERSION}"
cat "$OUTPUT_FILE"
