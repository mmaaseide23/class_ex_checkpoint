#!/bin/bash
set -e

CSV_FILE="/data/nsw_property_data.csv"

if [ ! -f "$CSV_FILE" ]; then
    echo "WARNING: $CSV_FILE not found. Skipping property data load."
    echo "Place nsw_property_data.csv in REServer/data/ and recreate the container."
    exit 0
fi

echo "Loading property data from CSV..."
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    COPY properties (
        property_id, download_date, council_name, purchase_price,
        address, post_code, property_type, strata_lot_number,
        property_name, area, area_type, contract_date,
        settlement_date, zoning, nature_of_property,
        primary_purpose, legal_description
    )
    FROM '$CSV_FILE'
    WITH (FORMAT csv, HEADER true, NULL '');
EOSQL

COUNT=$(psql -t --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "SELECT COUNT(*) FROM properties;")
echo "Loaded $COUNT properties."
