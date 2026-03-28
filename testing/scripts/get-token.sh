#!/usr/bin/env bash
# ============================================================
# get-token.sh — Obtain a Keycloak JWT for a given role/user
#
# Usage:
#   source testing/scripts/get-token.sh [ENV] [ROLE]
#
#   ENV:  local | dev (default: dev)
#   ROLE: admin | provider | nurse | ma | front_desk | billing | patient
#         (default: admin)
#
# Examples:
#   source testing/scripts/get-token.sh dev admin
#   source testing/scripts/get-token.sh local provider
#
# After sourcing, the variable TOKEN contains the JWT and
# AUTH_HEADER contains "Bearer <token>" for use with curl.
# ============================================================

set -euo pipefail

ENV="${1:-dev}"
ROLE="${2:-admin}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../environments/${ENV}.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: environment file not found: $ENV_FILE" >&2
  exit 1
fi

# Load environment variables
set -a
# shellcheck source=/dev/null
source "$ENV_FILE"
set +a

# Map ROLE to email/password
case "$ROLE" in
  admin)       EMAIL="$ADMIN_EMAIL";      PASSWORD="$ADMIN_PASSWORD" ;;
  provider)    EMAIL="$PROVIDER_EMAIL";   PASSWORD="$PROVIDER_PASSWORD" ;;
  nurse)       EMAIL="$NURSE_EMAIL";      PASSWORD="$NURSE_PASSWORD" ;;
  ma)          EMAIL="$MA_EMAIL";         PASSWORD="$MA_PASSWORD" ;;
  front_desk)  EMAIL="$FRONT_DESK_EMAIL"; PASSWORD="$FRONT_DESK_PASSWORD" ;;
  billing)     EMAIL="$BILLING_EMAIL";    PASSWORD="$BILLING_PASSWORD" ;;
  patient)     EMAIL="$PATIENT_EMAIL";    PASSWORD="$PATIENT_PASSWORD" ;;
  *)
    echo "ERROR: Unknown role '$ROLE'. Valid: admin|provider|nurse|ma|front_desk|billing|patient" >&2
    exit 1
    ;;
esac

TOKEN_URL="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token"

echo "Fetching token for role=$ROLE (email=$EMAIL) from $TOKEN_URL ..."

RESPONSE=$(curl -s -X POST "$TOKEN_URL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=${CLIENT_ID}" \
  -d "username=${EMAIL}" \
  -d "password=${PASSWORD}")

TOKEN=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('access_token',''))" 2>/dev/null || \
        echo "$RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [[ -z "$TOKEN" ]]; then
  echo "ERROR: Failed to get token. Response:" >&2
  echo "$RESPONSE" >&2
  exit 1
fi

export TOKEN
export AUTH_HEADER="Bearer $TOKEN"

echo "✓ Token obtained for $ROLE"
echo "  Sub-claim: $(echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("preferred_username","?"))' 2>/dev/null || echo "?")"
echo ""
echo "Usage: curl -H \"Authorization: \$AUTH_HEADER\" \$BASE_URL/api/..."
