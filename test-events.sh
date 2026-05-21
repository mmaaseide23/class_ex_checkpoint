set -e

GATEWAY="http://localhost:7070"
PROPERTY="http://localhost:7071"

PROPERTY_ID=$(( (RANDOM * RANDOM) % 90000000 + 10000000 ))
POSTCODE="2830"
EMAIL="demo-$RANDOM@example.com"

step()  { echo; echo "─── $1 ───────────────────────────────────────────"; }
pause() { sleep 1; }   # brief delay so notifications arrive in order

cat <<EOF
============================================================
  Event-Based Architecture Test
  Property ID:  $PROPERTY_ID
  Postcode:     $POSTCODE
  Test buyer:   $EMAIL
============================================================
EOF


step "1. Create a sale  →  fires NEW_SALE event"
# POST /sale  →  Property Server inserts row in 'sales' table
# Property Server then publishes property.changed with action=NEW_SALE.
curl -s -X POST "$GATEWAY/sale" \
  -H 'Content-Type: application/json' \
  -d "{
        \"propertyId\":     $PROPERTY_ID,
        \"postCode\":       \"$POSTCODE\",
        \"address\":        \"99 Demo Street, Bourke\",
        \"purchasePrice\":  450000,
        \"councilName\":    \"Bourke Shire\",
        \"propertyType\":   \"House\",
        \"zoning\":         \"Residential\"
      }"
echo
pause

step "2. Register purchaser + add postcode preference  (no event)"
# Without a purchaser interested in this postcode, the events above had nothing
# to match. This is the subscription side of the system.
PURCHASER=$(curl -s -X POST "$GATEWAY/purchaser" \
  -H 'Content-Type: application/json' \
  -d "{
        \"firstName\": \"Demo\",
        \"lastName\":  \"Buyer\",
        \"email\":     \"$EMAIL\",
        \"phone\":     \"555-0100\"
      }")
PURCHASER_ID=$(echo "$PURCHASER" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "  Purchaser id = $PURCHASER_ID"

curl -s -X POST "$GATEWAY/purchaser/$PURCHASER_ID/preference" \
  -H 'Content-Type: application/json' \
  -d "{ \"preferenceType\": \"postcode\", \"preferenceValue\": \"$POSTCODE\" }" > /dev/null
echo "  Subscribed to postcode $POSTCODE"
pause

# ── Events ──────────────────────────────────────────────────────────────────
#
#  From here on, every step should produce one notification box in the
#  NotificationConsumer terminal because our purchaser is now subscribed
#  to postcode $POSTCODE.
#

step "3. Create a listing  →  fires NEW_LISTING event"
# POST /listing  →  Property Server inserts row, looks up postcode from sales,
# publishes property.changed with action=NEW_LISTING.
curl -s -X POST "$GATEWAY/listing" \
  -H 'Content-Type: application/json' \
  -d "{
        \"propertyId\":  $PROPERTY_ID,
        \"listingDate\": \"2026-05-21\",
        \"price\":       3800000,
        \"status\":      \"Pending\"
      }"
echo
pause

# Look up the auto-generated listing id (we need it for PUT calls).
# The gateway returns HTML for listings, so we ask the property server directly.
LISTING_ID=$(curl -s "$PROPERTY/listing/$PROPERTY_ID" \
              | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
if [ -z "$LISTING_ID" ]; then
  echo "ERROR: could not resolve listing id"; exit 1
fi
echo "  Listing id = $LISTING_ID"
pause

step "4. Update listing price  →  fires PRICE_CHANGE event"
# PUT /listing/{id} with a new price. Controller compares old vs new and
# only fires PRICE_CHANGE if the value actually differs.
curl -s -X PUT "$GATEWAY/listing/$LISTING_ID" \
  -H 'Content-Type: application/json' \
  -d '{ "price": 4000000 }'
echo
pause

step "5. Change status Pending → Sold  →  fires STATUS_CHANGE event"
# PUT /listing/{id} with a new status. Same diff-and-publish logic.
curl -s -X PUT "$GATEWAY/listing/$LISTING_ID" \
  -H 'Content-Type: application/json' \
  -d '{ "status": "Sold" }'
echo
pause

step "6. Look up the sale  →  fires PROPERTY_HOT event"
# GET /sale/{id}  →  Gateway calls Property Server AND (async) tells the
# Analytics Server to record the access. Analytics increments the count and
# publishes property.hot. NotificationService consumes it, fetches property
# details for the postcode, then matches purchasers.
curl -s "$GATEWAY/sale/$PROPERTY_ID" > /dev/null
echo "  Access logged; analytics fired property.hot"

# ── Summary ─────────────────────────────────────────────────────────────────

cat <<EOF

============================================================
  Done. The NotificationConsumer should now show 5 boxes:

    1)  NEW_SALE       — "New sale registered at ..."
    2)  NEW_LISTING    — "New listing at ... priced at \$..."
    3)  PRICE_CHANGE   — "Price change at ...: was \$X, now \$Y"
    4)  STATUS_CHANGE  — "Status change at ...: Pending -> Sold"
    5)  PROPERTY_HOT   — "Hot property alert! ... is trending"

  Each box was produced by a separate domain event flowing through
  RabbitMQ. No service knows who consumes its events — that is the
  whole point of an event-based architecture.
============================================================
EOF
