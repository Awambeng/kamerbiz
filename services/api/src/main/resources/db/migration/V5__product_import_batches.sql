CREATE TABLE product_import_batch (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_account (id),
    batch_key VARCHAR(100) NOT NULL,
    created_count INTEGER NOT NULL,
    skipped_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_import_business_batch UNIQUE (business_id, batch_key)
);
