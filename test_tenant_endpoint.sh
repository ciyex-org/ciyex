#!/bin/bash

# Test script to check /api/tenants/accessible endpoint

echo "Testing /api/tenants/accessible endpoint..."
echo ""

# You need to replace TOKEN with actual JWT token from alice@example.com
TOKEN="YOUR_JWT_TOKEN_HERE"

curl -X GET "http://localhost:8080/api/tenants/accessible" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq .

echo ""
echo "Expected response:"
echo '{'
echo '  "success": true,'
echo '  "data": {'
echo '    "hasFullAccess": false,'
echo '    "tenants": ["CareWell", "Qiaben Health"],'
echo '    "requiresSelection": true'
echo '  }'
echo '}'
