#!/usr/bin/env bash
# Master demo — walks through every event scenario one at a time, pausing
# between each so the audience can read the output.
set -euo pipefail
cd "$(dirname "$0")"
source ./_lib.sh

C_BANNER=$'\033[1;35m'

banner() {
  local msg="$*"
  printf "\n%s╔══════════════════════════════════════════════════════════════════╗%s\n" "$C_BANNER" "$C_END"
  printf "%s║ %-64s ║%s\n" "$C_BANNER" "$msg" "$C_END"
  printf "%s╚══════════════════════════════════════════════════════════════════╝%s\n" "$C_BANNER" "$C_END"
}

pause() {
  echo
  printf "%s>>> %s — press Enter…%s " "$C_KEY" "$*" "$C_END"
  read -r _
}

check_http() {
  local name=$1 url=$2
  if curl -fsS --max-time 2 "$url/" >/dev/null 2>&1; then
    printf "  %s✓%s %-22s %s\n" "$C_OK" "$C_END" "$name" "$url"
  else
    printf "  ✗ %-22s %s (NOT RESPONDING)\n" "$name" "$url"
    return 1
  fi
}

check_docker() {
  local name=$1 container=$2 cmd=$3
  if docker exec "$container" $cmd >/dev/null 2>&1; then
    printf "  %s✓%s %-22s container=%s\n" "$C_OK" "$C_END" "$name" "$container"
  else
    printf "  ✗ %-22s container=%s (NOT RESPONDING)\n" "$name" "$container"
    return 1
  fi
}

preflight() {
  banner "PRE-FLIGHT — verifying infrastructure + services"
  local fail=0
  check_docker "Postgres"           realestate-db     "pg_isready -U postgres"      || fail=1
  check_docker "RabbitMQ"           realestate-rabbit "rabbitmq-diagnostics -q ping" || fail=1
  check_http   "API Gateway"        http://localhost:7070 || fail=1
  check_http   "Property server"    http://localhost:7071 || fail=1
  check_http   "Purchasers server"  http://localhost:7072 || fail=1
  check_http   "Analytics server"   http://localhost:7073 || fail=1
  # notification-service / -consumer have no HTTP port; check the process list.
  if pgrep -f "notification\.NotificationService" >/dev/null; then
    printf "  %s✓%s %-22s pid=%s\n" "$C_OK" "$C_END" "Notification service" \
      "$(pgrep -f notification\.NotificationService)"
  else
    printf "  ✗ %-22s (process not running)\n" "Notification service"; fail=1
  fi
  if pgrep -f "notification\.NotificationConsumer" >/dev/null; then
    printf "  %s✓%s %-22s pid=%s\n" "$C_OK" "$C_END" "Notification consumer" \
      "$(pgrep -f notification\.NotificationConsumer)"
  else
    printf "  ✗ %-22s (process not running)\n" "Notification consumer"; fail=1
  fi
  if [[ $fail -ne 0 ]]; then
    echo
    echo "  One or more services aren't responding. Start them and re-run."
    exit 1
  fi
}

run_scenario() {
  local n=$1 title=$2 script=$3
  banner "SCENARIO $n of 4 — $title"
  "./$script"
}

clear
banner "REAL-ESTATE EVENT-DRIVEN MICROSERVICES — END-TO-END DEMO"
cat <<'EOF'
  This walkthrough demonstrates four event scenarios end-to-end:

    1. NEW LISTING    POST  /listing                action=NEW_LISTING
    2. PRICE CHANGE   PATCH /listing/{id}/price     action=PRICE_CHANGED
    3. STATUS CHANGE  PATCH /listing/{id}/status    action=STATUS_CHANGED
    4. HOT PROPERTY   GET   /listing/{id}           event=PROPERTY_HOT

  Each scenario flows: API gateway → property/analytics server →
  RabbitMQ → notification service → purchasers server (HTTP) →
  per-purchaser fan-out → notification consumer.
EOF

preflight
pause "Start scenario 1"

run_scenario 1 "NEW LISTING"     01-new-listing.sh
pause "Continue to scenario 2"

run_scenario 2 "PRICE CHANGE"    02-price-change.sh
pause "Continue to scenario 3"

run_scenario 3 "STATUS CHANGE"   03-status-change.sh
pause "Continue to scenario 4"

run_scenario 4 "HOT PROPERTY"    04-hot-property.sh

banner "DEMO COMPLETE"
echo "  All four scenarios delivered notifications through the event chain."
echo "  Per-purchaser messages are tailed in /tmp/re-logs/consumer.log."