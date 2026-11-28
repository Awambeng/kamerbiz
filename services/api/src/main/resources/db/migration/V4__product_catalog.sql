CREATE TABLE product (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_account (id),
    product_code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(120),
    price NUMERIC(14, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(1000),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_business_code UNIQUE (business_id, product_code),
    CONSTRAINT ck_product_price_positive CHECK (price > 0),
    CONSTRAINT ck_product_stock_nonnegative CHECK (stock_quantity >= 0)
);

CREATE INDEX ix_product_business_active ON product (business_id, archived, name);
