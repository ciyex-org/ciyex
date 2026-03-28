#!/usr/bin/env bash
# ============================================================
# incremental-test.sh — Run incremental role permission tests
#
# Tests the API in phases, building up permissions one scope
# at a time to verify both access grant AND isolation.
#
# Usage:
#   bash testing/scripts/incremental-test.sh [ENV]
#   ENV: local | dev (default: dev)
#
# Requires: curl, jq
# ============================================================

set -euo pipefail

ENV="${1:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../environments/${ENV}.env"

set -a; source "$ENV_FILE"; set +a

PASS=0; FAIL=0; SKIP=0

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

# ── Helpers ───────────────────────────────────────────────────────────────────

get_token() {
  local email="$1" password="$2"
  curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password&client_id=${CLIENT_ID}&username=${email}&password=${password}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null
}

# expect_status <description> <expected_http_code> <token> <method> <path> [body]
expect_status() {
  local desc="$1" expected="$2" token="$3" method="$4" path="$5"
  local body="${6:-}"
  local url="${BASE_URL}${path}"

  local args=(-s -o /dev/null -w "%{http_code}" -X "$method" "$url")
  args+=(-H "Authorization: Bearer $token")
  if [[ -n "$body" ]]; then
    args+=(-H "Content-Type: application/json" -d "$body")
  fi

  local actual
  actual=$(curl "${args[@]}")

  if [[ "$actual" == "$expected" ]]; then
    echo -e "  ${GREEN}✓${NC} [$expected] $desc"
    ((PASS++))
  else
    echo -e "  ${RED}✗${NC} [$expected expected, got $actual] $desc"
    ((FAIL++))
  fi
}

header() {
  echo ""
  echo -e "${BOLD}${CYAN}━━━ $1 ━━━${NC}"
}

# ── PHASE 1: No auth → 401 everywhere ────────────────────────────────────────

header "PHASE 1: No Authentication → 401"
anon_test() {
  local actual
  actual=$(curl -s -o /dev/null -w "%{http_code}" -X "$1" "${BASE_URL}$2" -H "Content-Type: application/json" ${3:+-d "$3"})
  local desc="$4"
  if [[ "$actual" == "401" ]]; then
    echo -e "  ${GREEN}✓${NC} [401] $desc"
    ((PASS++))
  else
    echo -e "  ${RED}✗${NC} [401 expected, got $actual] $desc"
    ((FAIL++))
  fi
}
anon_test GET  "/api/fhir-resource/patients"                    ""   "GET /fhir-resource/patients"
anon_test POST "/api/fhir-resource/patients"                    "{}" "POST /fhir-resource/patients"
anon_test GET  "/api/admin/roles"                               ""   "GET /admin/roles"
anon_test GET  "/api/admin/users"                               ""   "GET /admin/users"
anon_test DELETE "/api/fhir-resource/patients/patient/1/x"     ""   "DELETE /fhir-resource resource"

# ── PHASE 2: Admin token → all endpoints accessible ──────────────────────────

header "PHASE 2: ADMIN role → full access"
echo "  Fetching ADMIN token..."
ADMIN_TOKEN=$(get_token "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
if [[ -z "$ADMIN_TOKEN" ]]; then
  echo -e "  ${YELLOW}SKIP${NC} — could not obtain ADMIN token (check credentials)"
  ((SKIP+=5))
else
  expect_status "GET /fhir-resource/patients"    "200" "$ADMIN_TOKEN" GET  "/api/fhir-resource/patients"
  expect_status "GET /admin/roles"               "200" "$ADMIN_TOKEN" GET  "/api/admin/roles"
  expect_status "GET /admin/users"               "200" "$ADMIN_TOKEN" GET  "/api/admin/users"
  expect_status "POST /fhir-resource/patients"   "201" "$ADMIN_TOKEN" POST "/api/fhir-resource/patients" '{"resourceType":"Patient"}'
fi

# ── PHASE 3: PROVIDER token — clinical yes, admin no ─────────────────────────

header "PHASE 3: PROVIDER role — clinical access, no admin"
echo "  Fetching PROVIDER token..."
PROVIDER_TOKEN=$(get_token "$PROVIDER_EMAIL" "$PROVIDER_PASSWORD")
if [[ -z "$PROVIDER_TOKEN" ]]; then
  echo -e "  ${YELLOW}SKIP${NC} — could not obtain PROVIDER token"
  ((SKIP+=5))
else
  expect_status "GET /fhir-resource/patients → 200"  "200" "$PROVIDER_TOKEN" GET "/api/fhir-resource/patients"
  expect_status "GET /fhir-resource/encounters → 200" "200" "$PROVIDER_TOKEN" GET "/api/fhir-resource/encounters"
  expect_status "GET /admin/roles → 403"             "403" "$PROVIDER_TOKEN" GET "/api/admin/roles"
  expect_status "GET /admin/users → 403"             "403" "$PROVIDER_TOKEN" GET "/api/admin/users"
  expect_status "POST /admin/roles → 403"            "403" "$PROVIDER_TOKEN" POST "/api/admin/roles" '{}'
fi

# ── PHASE 4: BILLING token — financial access only ───────────────────────────

header "PHASE 4: BILLING role — financial access, no clinical write"
echo "  Fetching BILLING token..."
BILLING_TOKEN=$(get_token "$BILLING_EMAIL" "$BILLING_PASSWORD")
if [[ -z "$BILLING_TOKEN" ]]; then
  echo -e "  ${YELLOW}SKIP${NC} — could not obtain BILLING token"
  ((SKIP+=5))
else
  expect_status "GET /fhir-resource/claims → 200"      "200" "$BILLING_TOKEN" GET  "/api/fhir-resource/claims"
  expect_status "GET /fhir-resource/patients → 200"    "200" "$BILLING_TOKEN" GET  "/api/fhir-resource/patients"
  expect_status "POST /fhir-resource/encounters → 403" "403" "$BILLING_TOKEN" POST "/api/fhir-resource/encounters/patient/1" '{}'
  expect_status "GET /admin/roles → 403"               "403" "$BILLING_TOKEN" GET  "/api/admin/roles"
fi

# ── PHASE 5: FRONT_DESK token — scheduling access ────────────────────────────

header "PHASE 5: FRONT_DESK role — scheduling + registration, no clinical write"
echo "  Fetching FRONT_DESK token..."
FD_TOKEN=$(get_token "$FRONT_DESK_EMAIL" "$FRONT_DESK_PASSWORD")
if [[ -z "$FD_TOKEN" ]]; then
  echo -e "  ${YELLOW}SKIP${NC} — could not obtain FRONT_DESK token"
  ((SKIP+=4))
else
  expect_status "GET /fhir-resource/patients → 200"      "200" "$FD_TOKEN" GET  "/api/fhir-resource/patients"
  expect_status "GET /fhir-resource/appointments → 200"  "200" "$FD_TOKEN" GET  "/api/fhir-resource/appointments"
  expect_status "POST /fhir-resource/encounters → 403"   "403" "$FD_TOKEN" POST "/api/fhir-resource/encounters/patient/1" '{}'
  expect_status "GET /admin/users → 403"                 "403" "$FD_TOKEN" GET  "/api/admin/users"
fi

# ── PHASE 6: PATIENT token — self-access only ────────────────────────────────

header "PHASE 6: PATIENT role — staff endpoints all forbidden"
echo "  Fetching PATIENT token..."
PATIENT_TOKEN=$(get_token "$PATIENT_EMAIL" "$PATIENT_PASSWORD")
if [[ -z "$PATIENT_TOKEN" ]]; then
  echo -e "  ${YELLOW}SKIP${NC} — could not obtain PATIENT token"
  ((SKIP+=4))
else
  expect_status "GET /fhir-resource/patients → 403"   "403" "$PATIENT_TOKEN" GET "/api/fhir-resource/patients"
  expect_status "GET /fhir-resource/encounters → 403" "403" "$PATIENT_TOKEN" GET "/api/fhir-resource/encounters"
  expect_status "GET /admin/roles → 403"              "403" "$PATIENT_TOKEN" GET "/api/admin/roles"
  expect_status "GET /admin/users → 403"              "403" "$PATIENT_TOKEN" GET "/api/admin/users"
fi

# ── Summary ───────────────────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}━━━ Test Summary ━━━${NC}"
echo -e "  ${GREEN}PASS: $PASS${NC}"
echo -e "  ${RED}FAIL: $FAIL${NC}"
if [[ "$SKIP" -gt 0 ]]; then
  echo -e "  ${YELLOW}SKIP: $SKIP${NC} (Keycloak unreachable or credentials wrong)"
fi

if [[ "$FAIL" -gt 0 ]]; then
  echo ""
  echo -e "${RED}FAILED${NC}: $FAIL test(s) did not pass."
  exit 1
else
  echo ""
  echo -e "${GREEN}All tests passed!${NC}"
fi
