CREATE TABLE product_image_cleanup (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_account(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_product_image_cleanup_business_url UNIQUE (business_id, image_url)
);

CREATE INDEX idx_product_image_cleanup_due
    ON product_image_cleanup (next_attempt_at, created_at);
