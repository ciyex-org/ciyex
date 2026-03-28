#!/bin/bash
set -e

# Update docs repository changelog with release notes
# Usage: update-docs-changelog.sh <version> <app_name> <docs_deploy_key>

VERSION="$1"
APP_NAME="${2:-ciyex-hapi-fhir}"
DOCS_DEPLOY_KEY="$3"

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version> <app_name> [docs_deploy_key]"
    exit 1
fi

if [ -z "$DOCS_DEPLOY_KEY" ]; then
    echo "⚠️ DOCS_DEPLOY_KEY not provided, skipping docs update"
    exit 0
fi

RELEASE_NOTES_FILE="/tmp/release_notes.rst"
if [ ! -f "$RELEASE_NOTES_FILE" ]; then
    echo "❌ Release notes file not found at $RELEASE_NOTES_FILE"
    exit 1
fi

# Setup SSH for docs repo
mkdir -p ~/.ssh
echo "$DOCS_DEPLOY_KEY" > ~/.ssh/docs_deploy_key
chmod 600 ~/.ssh/docs_deploy_key
ssh-keyscan github.com >> ~/.ssh/known_hosts 2>/dev/null

# Clone docs repo
rm -rf /tmp/docs
GIT_SSH_COMMAND="ssh -i ~/.ssh/docs_deploy_key" git clone git@github.com:ciyex-org/docs.git /tmp/docs
cd /tmp/docs

git config user.name "GitHub Actions"
git config user.email "actions@github.com"

# Ensure directory exists
mkdir -p "source/releases/${APP_NAME}"

CHANGELOG="source/releases/${APP_NAME}/CHANGELOG.rst"

if [ -f "$CHANGELOG" ]; then
    # Get header (first 8 lines)
    head -n 8 "$CHANGELOG" > /tmp/changelog_header.rst
    # Get rest of file
    tail -n +9 "$CHANGELOG" > /tmp/changelog_body.rst
    # Combine: header + new notes + existing body
    cat /tmp/changelog_header.rst "$RELEASE_NOTES_FILE" /tmp/changelog_body.rst > "$CHANGELOG"
else
    # Create new changelog with header
    cat > "$CHANGELOG" << EOF
Changelog
=========

All notable changes to ${APP_NAME} will be documented in this file.

.. note::
   This file is automatically updated by the CI/CD pipeline when RC and GA releases are created.

EOF
    cat "$RELEASE_NOTES_FILE" >> "$CHANGELOG"
fi

# Also ensure index.rst exists
INDEX_FILE="source/releases/${APP_NAME}/index.rst"
if [ ! -f "$INDEX_FILE" ]; then
    APP_TITLE=$(echo "$APP_NAME" | sed 's/-/ /g' | sed 's/\b\(.\)/\u\1/g')
    cat > "$INDEX_FILE" << EOF
${APP_TITLE} Release Notes
$(printf '=%.0s' $(seq 1 $((${#APP_TITLE} + 14))))

Release history for ${APP_TITLE}.

.. toctree::
   :maxdepth: 1
   :caption: Releases
   :reversed:

   CHANGELOG
EOF
fi

git add "source/releases/${APP_NAME}/"
git commit -m "docs: add release notes for ${APP_NAME} ${VERSION}" || echo "No changes to commit"
GIT_SSH_COMMAND="ssh -i ~/.ssh/docs_deploy_key" git push origin main || echo "Push failed"

echo "✅ Updated docs repository with release notes for ${APP_NAME} ${VERSION}"

# Cleanup
rm -f ~/.ssh/docs_deploy_key
