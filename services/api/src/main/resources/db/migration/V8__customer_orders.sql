CREATE TABLE customer_order (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_account(id),
    reference VARCHAR(32) NOT NULL UNIQUE,
    tracking_token_hash VARCHAR(64) NOT NULL UNIQUE,
    customer_name VARCHAR(160) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    fulfilment_method VARCHAR(16) NOT NULL,
    delivery_address VARCHAR(1000),
    customer_note VARCHAR(500),
    order_status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    fulfilment_status VARCHAR(24) NOT NULL DEFAULT 'NOT_ARRANGED',
    total_amount NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_order_fulfilment_method CHECK (fulfilment_method IN ('DELIVERY','PICKUP')),
    CONSTRAINT ck_order_total_positive CHECK (total_amount > 0)
);
CREATE TABLE customer_order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_order(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES product(id),
    product_name VARCHAR(255) NOT NULL,
    unit_price NUMERIC(18,2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(18,2) NOT NULL,
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_customer_order_business_created ON customer_order(business_id, created_at DESC);
CREATE INDEX ix_customer_order_item_order ON customer_order_item(order_id);
