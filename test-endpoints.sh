#!/bin/bash

echo "Testing Ciyex Endpoints..."
echo "================================"

test_endpoint() {
  url=$1
  status=$(curl -s -o /dev/null -w '%{http_code}' "$url")
  if [ "$status" = "200" ]; then
    echo "✅ $url - OK ($status)"
  elif [ "$status" = "401" ]; then
    echo "🔒 $url - Auth Required ($status)"
  elif [ "$status" = "403" ]; then
    echo "❌ $url - Cloudflare Blocked ($status)"
  else
    echo "⚠️  $url - Status: $status"
  fi
}

echo ""
echo "DEV Environment:"
test_endpoint "https://api-dev.ciyex.org/actuator/health"
test_endpoint "https://api-dev.ciyex.org/actuator/info"
test_endpoint "https://api-dev.ciyex.org"

echo ""
echo "STAGE Environment:"
test_endpoint "https://api-stage.ciyex.org/actuator/health"
test_endpoint "https://api-stage.ciyex.org/actuator/info"
test_endpoint "https://api-stage.ciyex.org"

echo ""
echo "================================"
echo "Done!"
