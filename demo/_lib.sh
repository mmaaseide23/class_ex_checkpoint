#!/usr/bin/env bash
# Shared helpers for the three event-flow demos.

GATEWAY="${GATEWAY:-http://localhost:7070}"
PROPERTY_LOG="${PROPERTY_LOG:-/tmp/re-logs/property.log}"
NOTIFY_LOG="${NOTIFY_LOG:-/tmp/re-logs/notify.log}"
CONSUMER_LOG="${CONSUMER_LOG:-/tmp/re-logs/consumer.log}"

C_HEAD=$'\033[1;36m'; C_OK=$'\033[1;32m'; C_KEY=$'\033[1;33m'; C_END=$'\033[0m'

section() { printf "\n%s══════ %s ══════%s\n" "$C_HEAD" "$*" "$C_END"; }
info()    { printf "  %s\n" "$*"; }
ok()      { printf "  %s%s%s\n" "$C_OK" "$*" "$C_END"; }
indent()  { sed 's/^/  /'; }

# Snapshot current line counts of every log we tail later.
snapshot_logs() {
  N_PROP=$(wc -l < "$PROPERTY_LOG")
  N_NOTIFY=$(wc -l < "$NOTIFY_LOG")
  N_CONSUMER=$(wc -l < "$CONSUMER_LOG")
}

# Show only lines appended since snapshot_logs.
since_property() { tail -n +$((N_PROP+1)) "$PROPERTY_LOG"; }
since_notify()   { tail -n +$((N_NOTIFY+1)) "$NOTIFY_LOG"; }
since_consumer() { tail -n +$((N_CONSUMER+1)) "$CONSUMER_LOG"; }

# Render steps 2-6 of the chain from log snapshots after an API call.
show_chain() {
  section "STEP 2 — property-server publishes event to RabbitMQ (topic exchange)"
  since_property | grep "EventPublisher" | indent || info "(no EventPublisher line captured)"

  section "STEP 3 — notification-service consumes property.changed from RabbitMQ"
  since_notify | grep "Received" | indent

  section "STEP 4 — notification-service queries purchasers-server over HTTP"
  since_notify | grep "HTTP GET" | indent

  section "STEP 5 — notification-service fans out to purchaser.notifications queue"
  since_notify | grep "Sent " | indent

  section "STEP 6 — notification-consumer prints final notifications (first 2 shown)"
  awk '/NOTIFICATION/{c++} c<=2 && c>=1 {print; if(c==2 && /^---/) exit}' \
      <(since_consumer) | indent
}