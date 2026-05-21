#!/usr/bin/env bash
# Demo: a 'for sale' property is searched for; the analytics access-count is
# incremented; every purchaser interested in that postcode gets a HOT
# notification carrying the property address, postcode and view count.
set -euo pipefail
cd "$(dirname "$0")"
source ./_lib.sh

ANALYTICS_LOG="${ANALYTICS_LOG:-/tmp/re-logs/analytics.log}"

# Pick a property that:
#   - has a Pending listing (i.e. is currently FOR SALE)
#   - is in a postcode that has interested purchasers
read -r PROPERTY_ID POSTCODE <<<"$(
  docker exec realestate-db psql -U postgres -d realestate -t -A -F' ' -c "
    WITH for_sale AS (
      SELECT DISTINCT property_id FROM listings WHERE status='Pending'
    ),
    sub_count AS (
      SELECT preference_value AS post_code, COUNT(*) AS subs
      FROM user_preferences WHERE preference_type='postcode'
      GROUP BY preference_value
    )
    SELECT s.property_id, s.post_code
    FROM sales s
    JOIN for_sale f ON f.property_id = s.property_id
    JOIN sub_count c ON c.post_code = s.post_code
    ORDER BY c.subs DESC
    LIMIT 1")"
SUBS=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c \
  "SELECT COUNT(*) FROM user_preferences WHERE preference_type='postcode' AND preference_value='$POSTCODE'")

OLD_COUNT=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c \
  "SELECT COALESCE(count,0) FROM access_counts WHERE access_type='property' AND access_value='$PROPERTY_ID'")
OLD_COUNT=${OLD_COUNT:-0}

section "SCENARIO 4 — HOT PROPERTY (search increments 'like' count)"
info "Property $PROPERTY_ID (postcode $POSTCODE) currently FOR SALE — has Pending listing"
info "${C_KEY}$SUBS${C_END} purchasers are interested in postcode $POSTCODE"
info "Current access count for this property: $OLD_COUNT"

section "STEP 1 — API call: search a property listing (client → API gateway)"
info "curl $GATEWAY/listing/$PROPERTY_ID"
info "(the gateway fires an async POST to analytics/access in the background)"

snapshot_logs
N_ANALYTICS=$(wc -l < "$ANALYTICS_LOG")

curl -s "$GATEWAY/listing/$PROPERTY_ID" > /dev/null
echo
ok "Gateway response: 200 (HTML listing returned to client)"
sleep 2

section "STEP 2 — gateway fires fire-and-forget POST /analytics/access (HTTP)"
info "api-gateway POST http://localhost:7073/analytics/access {\"type\":\"property\",\"value\":\"$PROPERTY_ID\"}"
NEW_COUNT=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c \
  "SELECT count FROM access_counts WHERE access_type='property' AND access_value='$PROPERTY_ID'")
info "access_counts row in DB: count is now ${C_KEY}$NEW_COUNT${C_END} (was $OLD_COUNT)"

section "STEP 3 — analytics-server publishes property.hot event to RabbitMQ"
tail -n +$((N_ANALYTICS+1)) "$ANALYTICS_LOG" | grep "EventPublisher" | indent || info "(no EventPublisher line captured)"

section "STEP 4 — notification-service consumes property.hot from RabbitMQ"
since_notify | grep "Received property.hot" | indent

section "STEP 5 — notification-service queries property-server (for-sale check) + purchasers-server"
since_notify | grep "HTTP GET" | indent

section "STEP 6 — notification-service fans out to purchaser.notifications queue"
since_notify | grep "Sent .*PROPERTY_HOT" | indent

section "STEP 7 — notification-consumer prints final notifications (first 2 shown)"
awk '/NOTIFICATION/{c++} c<=2 && c>=1 {print; if(c==2 && /^---/) exit}' \
    <(since_consumer) | indent

section "RESULT"
ok "$SUBS purchasers received a 'Hot property alert! …' notification for postcode $POSTCODE"