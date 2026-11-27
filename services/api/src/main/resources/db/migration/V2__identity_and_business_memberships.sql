CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    identity_issuer VARCHAR(512) NOT NULL,
    identity_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320),
    display_name VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_user_account_identity UNIQUE (identity_issuer, identity_subject)
);

CREATE TABLE user_platform_role (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id),
    role VARCHAR(32) NOT NULL,
    CONSTRAINT uq_user_platform_role UNIQUE (user_id, role)
);

CREATE TABLE business_account (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE business_membership (
    id UUID PRIMARY KEY,
    business_id UUID NOT NULL REFERENCES business_account (id),
    user_id UUID NOT NULL REFERENCES user_account (id),
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_business_membership UNIQUE (business_id, user_id)
);

CREATE INDEX ix_business_membership_user_id ON business_membership (user_id);
