CREATE TABLE IF NOT EXISTS sales (
    id              SERIAL PRIMARY KEY,
    property_id     BIGINT,
    download_date   DATE,
    council_name    VARCHAR(255),
    purchase_price  BIGINT,
    address         VARCHAR(500),
    post_code       VARCHAR(20),
    property_type   VARCHAR(100),
    strata_lot_number VARCHAR(100),
    property_name   VARCHAR(255),
    area            DOUBLE PRECISION,
    area_type       VARCHAR(100),
    contract_date   DATE,
    settlement_date DATE,
    zoning          VARCHAR(100),
    nature_of_property VARCHAR(100),
    primary_purpose VARCHAR(100),
    legal_description TEXT
);

CREATE INDEX IF NOT EXISTS idx_sales_property_id ON sales (property_id);
CREATE INDEX IF NOT EXISTS idx_sales_post_code ON sales (post_code);
CREATE INDEX IF NOT EXISTS idx_sales_purchase_price ON sales (purchase_price);

CREATE TABLE IF NOT EXISTS listings (
    id            SERIAL PRIMARY KEY,
    property_id   BIGINT NOT NULL,
    listing_date  DATE NOT NULL,
    price         BIGINT NOT NULL,
    CONSTRAINT unique_listing UNIQUE (property_id, listing_date, price)
);

CREATE INDEX IF NOT EXISTS idx_listings_property_id ON listings (property_id);

CREATE TABLE IF NOT EXISTS users (
    id           SERIAL PRIMARY KEY,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    phone        VARCHAR(20),
    created_date DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS user_preferences (
    id               SERIAL PRIMARY KEY,
    user_id          INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preference_type  VARCHAR(100) NOT NULL,
    preference_value VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences (user_id);

CREATE TABLE IF NOT EXISTS access_counts (
    id           SERIAL PRIMARY KEY,
    access_type  VARCHAR(20) NOT NULL,
    access_value VARCHAR(255) NOT NULL,
    count        INTEGER NOT NULL DEFAULT 0,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_access UNIQUE (access_type, access_value)
);

CREATE INDEX IF NOT EXISTS idx_access_counts_type_value ON access_counts (access_type, access_value);

