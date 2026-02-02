#!/bin/bash
set -e

echo "=========================================="
echo "Ciyex Automated Deployment Pipeline"
echo "=========================================="
echo ""

# Function to check workflow status
check_workflow_status() {
    gh run list --workflow=ci-cd.yml --limit 1 --json status,conclusion,displayTitle,databaseId | \
    jq -r '.[] | "\(.status)|\(.conclusion // "N/A")|\(.displayTitle)|\(.databaseId)"'
}

# Function to wait for workflow completion
wait_for_workflow() {
    local workflow_name="$1"
    echo "⏳ Waiting for $workflow_name to complete..."
    
    while true; do
        STATUS_INFO=$(check_workflow_status)
        STATUS=$(echo "$STATUS_INFO" | cut -d'|' -f1)
        CONCLUSION=$(echo "$STATUS_INFO" | cut -d'|' -f2)
        TITLE=$(echo "$STATUS_INFO" | cut -d'|' -f3)
        RUN_ID=$(echo "$STATUS_INFO" | cut -d'|' -f4)
        
        if [ "$STATUS" = "completed" ]; then
            if [ "$CONCLUSION" = "success" ]; then
                echo "✅ $workflow_name completed successfully!"
                echo "   Run ID: $RUN_ID"
                return 0
            else
                echo "❌ $workflow_name failed with conclusion: $CONCLUSION"
                echo "   Run ID: $RUN_ID"
                echo "   View logs: https://github.com/qiaben/ciyex/actions/runs/$RUN_ID"
                return 1
            fi
        fi
        
        echo "   Status: $STATUS - $TITLE"
        sleep 15
    done
}

# Step 1: Wait for alpha build
echo "=========================================="
echo "STEP 1: Alpha Build (Dev Environment)"
echo "=========================================="
wait_for_workflow "Alpha build"
echo ""

# Step 2: Promote to RC
echo "=========================================="
echo "STEP 2: Promote to RC (Stage Environment)"
echo "=========================================="
echo "🚀 Triggering RC promotion..."
gh workflow run ci-cd.yml -f action="Promote to RC"
sleep 5
wait_for_workflow "RC promotion"
echo ""

# Step 3: Promote to GA
echo "=========================================="
echo "STEP 3: Promote to GA (Prod Environment)"
echo "=========================================="
echo "🚀 Triggering GA promotion..."
gh workflow run ci-cd.yml -f action="Promote to GA"
sleep 5
wait_for_workflow "GA promotion"
echo ""

# Summary
echo "=========================================="
echo "✅ DEPLOYMENT COMPLETE!"
echo "=========================================="
echo ""
echo "Deployed versions:"
echo "  Dev:   0.0.1-alpha.21"
echo "  Stage: 0.0.1-rc"
echo "  Prod:  v0.0.1"
echo ""
echo "Check ArgoCD applications:"
echo "  - ciyex-app-us-east-dev"
echo "  - ciyex-app-us-east-stage"
echo "  - ciyex-app-us-east-prod"
echo ""
echo "GitHub Release: https://github.com/qiaben/ciyex/releases/tag/v0.0.1"
echo ""
