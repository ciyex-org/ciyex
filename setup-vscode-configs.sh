#!/bin/bash

# Quick setup script for VS Code configurations
# This is a convenience wrapper around the template setup script

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "Ciyex VS Code Configuration Setup"
echo "========================================="
echo ""
echo "This will install VS Code launch and task configurations."
echo ""

# Run the template setup script
"$PROJECT_DIR/.vscode-templates/setup-vscode.sh"

echo ""
echo "📖 For more information, see:"
echo "   - VSCODE_SETUP.md (Quick start guide)"
echo "   - .vscode-templates/README.md (Detailed documentation)"
echo "   - RUN_CONFIGURATIONS.md (Full reference)"
echo ""
