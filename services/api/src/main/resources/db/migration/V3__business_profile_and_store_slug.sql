ALTER TABLE business_account
    ADD COLUMN slug VARCHAR(100),
    ADD COLUMN description VARCHAR(2000),
    ADD COLUMN logo_url VARCHAR(1000),
    ADD COLUMN phone VARCHAR(32),
    ADD COLUMN whatsapp VARCHAR(32),
    ADD COLUMN email VARCHAR(320),
    ADD COLUMN address VARCHAR(500),
    ADD COLUMN city VARCHAR(120),
    ADD COLUMN region VARCHAR(120),
    ADD COLUMN country VARCHAR(80) NOT NULL DEFAULT 'Cameroon',
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE business_account
SET slug = 'business-' || REPLACE(id::TEXT, '-', '')
WHERE slug IS NULL;

ALTER TABLE business_account
    ALTER COLUMN slug SET NOT NULL,
    ADD CONSTRAINT uq_business_account_slug UNIQUE (slug),
    ADD CONSTRAINT ck_business_account_slug_format
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$');
