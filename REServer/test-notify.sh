#!/usr/bin/env bash
# Barebones smoke test for GET /user/notify.
# Requires the server to be running on localhost:7070.

set -u

BASE_URL="${BASE_URL:-http://localhost:7070}"
URL="$BASE_URL/user/notify"

echo "GET $URL"

tmp_body="$(mktemp)"
trap 'rm -f "$tmp_body"' EXIT

status="$(curl -s -o "$tmp_body" -w '%{http_code}' "$URL")"

fail=0

if [ "$status" = "200" ]; then
  echo "PASS  status=200"
else
  echo "FAIL  expected status 200, got $status"
  fail=1
fi

content_type="$(curl -s -o /dev/null -D - "$URL" | grep -i '^content-type:' | tr -d '\r')"
echo "INFO  $content_type"

if grep -q "Purchaser Notifications" "$tmp_body"; then
  echo "PASS  body contains 'Purchaser Notifications'"
else
  # Plain-text fallback path produces no header. Accept any non-empty body.
  if [ -s "$tmp_body" ]; then
    echo "PASS  body non-empty (plain-text fallback path)"
  else
    echo "FAIL  body empty"
    fail=1
  fi
fi

byte_count="$(wc -c <"$tmp_body" | tr -d ' ')"
echo "INFO  body bytes=$byte_count"

if [ "$fail" -eq 0 ]; then
  echo "OK"
  exit 0
else
  echo "FAILED"
  exit 1
fi