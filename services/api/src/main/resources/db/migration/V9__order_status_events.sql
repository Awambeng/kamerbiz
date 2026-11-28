CREATE TABLE order_status_event (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_order(id) ON DELETE CASCADE,
    business_id UUID NOT NULL REFERENCES business_account(id),
    event_type VARCHAR(24) NOT NULL,
    old_status VARCHAR(24),
    new_status VARCHAR(24) NOT NULL,
    actor_user_id UUID REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_order_event_type CHECK (event_type IN ('ORDER','FULFILMENT'))
);

CREATE INDEX ix_order_status_event_order_created
    ON order_status_event(order_id, created_at);
CREATE INDEX ix_order_status_event_business_created
    ON order_status_event(business_id, created_at DESC);
