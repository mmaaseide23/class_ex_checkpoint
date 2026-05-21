#!/usr/bin/env bash
# Demo: a NEW listing is posted; every purchaser interested in that postcode
# is notified. Walks through the full event chain.
set -euo pipefail
cd "$(dirname "$0")"
source ./_lib.sh

# Pick the property with the most subscribers so the demo is visible.
PROPERTY_ID=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c "
  SELECT s.property_id
  FROM sales s
  JOIN user_preferences p
    ON p.preference_type='postcode' AND p.preference_value = s.post_code
  GROUP BY s.property_id, s.post_code
  ORDER BY COUNT(*) DESC
  LIMIT 1")
POSTCODE=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c \
  "SELECT post_code FROM sales WHERE property_id=$PROPERTY_ID LIMIT 1")
SUBS=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c \
  "SELECT COUNT(*) FROM user_preferences WHERE preference_type='postcode' AND preference_value='$POSTCODE'")
PRICE=$((RANDOM * 100 + 500000))
DATE="2026-12-$(printf '%02d' $((RANDOM % 28 + 1)))"

section "SCENARIO 1 — NEW LISTING"
info "Property $PROPERTY_ID, postcode $POSTCODE has ${C_KEY}$SUBS${C_END} interested purchasers"

section "STEP 1 — API call (client → API gateway → property-server)"
info "curl -X POST $GATEWAY/listing \\"
info "     -H 'Content-Type: application/json' \\"
info "     -d '{\"propertyId\":$PROPERTY_ID,\"listingDate\":\"$DATE\",\"price\":$PRICE}'"

snapshot_logs
RESPONSE=$(curl -s -X POST "$GATEWAY/listing" \
    -H 'Content-Type: application/json' \
    -d "{\"propertyId\":$PROPERTY_ID,\"listingDate\":\"$DATE\",\"price\":$PRICE}")
echo
ok  "Gateway response: $RESPONSE"
sleep 2

show_chain

section "RESULT"
ok "$SUBS purchasers received a 'New property listed…' notification for postcode $POSTCODE"