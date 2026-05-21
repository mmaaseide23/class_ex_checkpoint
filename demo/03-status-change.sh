#!/usr/bin/env bash
# Demo: an EXISTING listing has its status toggled (Pending ↔ Sold); every
# purchaser interested in that postcode is notified. Walks the event chain.
set -euo pipefail
cd "$(dirname "$0")"
source ./_lib.sh

# Find a listing whose postcode has at least one subscriber.
read -r LISTING_ID PROPERTY_ID POSTCODE OLD_STATUS <<<"$(
  docker exec realestate-db psql -U postgres -d realestate -t -A -F' ' -c "
    SELECT l.id, l.property_id, s.post_code, l.status
    FROM listings l
    JOIN sales s ON s.property_id = l.property_id
    WHERE s.post_code IN (
      SELECT DISTINCT preference_value
      FROM user_preferences WHERE preference_type='postcode')
    ORDER BY l.id
    LIMIT 1")"
SUBS=$(docker exec realestate-db psql -U postgres -d realestate -t -A -c \
  "SELECT COUNT(*) FROM user_preferences WHERE preference_type='postcode' AND preference_value='$POSTCODE'")
# Toggle to the opposite status.
if [[ "$OLD_STATUS" == "Pending" ]]; then NEW_STATUS=Sold; else NEW_STATUS=Pending; fi

section "SCENARIO 3 — STATUS CHANGE on existing listing"
info "Listing $LISTING_ID (property $PROPERTY_ID, postcode $POSTCODE): $OLD_STATUS → $NEW_STATUS"
info "${C_KEY}$SUBS${C_END} purchasers are interested in postcode $POSTCODE"

section "STEP 1 — API call (client → API gateway → property-server)"
info "curl -X PATCH $GATEWAY/listing/$LISTING_ID/status \\"
info "     -H 'Content-Type: application/json' \\"
info "     -d '{\"status\":\"$NEW_STATUS\"}'"

snapshot_logs
RESPONSE=$(curl -s -X PATCH "$GATEWAY/listing/$LISTING_ID/status" \
    -H 'Content-Type: application/json' \
    -d "{\"status\":\"$NEW_STATUS\"}")
echo
ok  "Gateway response: $RESPONSE"
sleep 2

show_chain

section "RESULT"
ok "$SUBS purchasers received a 'Status changed…' notification for postcode $POSTCODE"