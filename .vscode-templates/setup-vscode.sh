#!/bin/bash

# Setup VS Code configurations for Ciyex project
# This script copies the template configurations to .vscode directory

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$PROJECT_DIR/.vscode-templates"
VSCODE_DIR="$PROJECT_DIR/.vscode"

echo "========================================="
echo "VS Code Configuration Setup"
echo "========================================="
echo ""

# Create .vscode directory if it doesn't exist
if [ ! -d "$VSCODE_DIR" ]; then
    echo "Creating .vscode directory..."
    mkdir -p "$VSCODE_DIR"
    echo "✅ Created .vscode directory"
else
    echo "✅ .vscode directory exists"
fi

echo ""

# Function to copy file with backup
copy_with_backup() {
    local file=$1
    local source="$TEMPLATE_DIR/$file"
    local dest="$VSCODE_DIR/$file"
    
    if [ -f "$dest" ]; then
        echo "⚠️  $file already exists"
        read -p "   Overwrite? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            # Create backup
            cp "$dest" "$dest.backup.$(date +%Y%m%d_%H%M%S)"
            echo "   📦 Backup created"
            cp "$source" "$dest"
            echo "   ✅ $file updated"
        else
            echo "   ⏭️  Skipped $file"
        fi
    else
        cp "$source" "$dest"
        echo "✅ $file created"
    fi
}

# Copy configuration files
echo "Copying configuration files..."
echo ""
copy_with_backup "tasks.json"
copy_with_backup "launch.json"

echo ""
echo "========================================="
echo "✅ Setup Complete!"
echo "========================================="
echo ""
echo "Configuration files are now in: $VSCODE_DIR"
echo ""
echo "Next steps:"
echo "  1. Reload VS Code window (Ctrl+Shift+P → 'Reload Window')"
echo "  2. Press F5 to see available launch configurations"
echo "  3. Select a configuration to start services"
echo ""
echo "Available configurations:"
echo "  • Ciyex Backend (Auto Build) - Port 8080"
echo "  • EHR UI (Next.js) - Port 3000"
echo "  • Portal UI (Next.js) - Port 3001"
echo "  • Full Stack (Backend + EHR UI)"
echo "  • Full Stack (Backend + Portal UI)"
echo "  • All Services (Backend + Both UIs)"
echo ""
echo "========================================="
