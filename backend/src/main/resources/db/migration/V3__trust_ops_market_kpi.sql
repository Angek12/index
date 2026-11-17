-- Phone verification and terms
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET phone_verified = TRUE;
ALTER TABLE users ADD COLUMN terms_accepted_at TIMESTAMPTZ;

CREATE TABLE phone_verifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_phone_verifications_user ON phone_verifications(user_id);

-- Orders: delivery confirmation code and where the order came from
ALTER TABLE orders ADD COLUMN delivery_code VARCHAR(6);
ALTER TABLE orders ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'ADMIN';

-- Problem reports on orders
CREATE TABLE order_issues (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    reported_by_user_id BIGINT        NOT NULL REFERENCES users(id),
    reporter_role       VARCHAR(10)   NOT NULL,
    issue_type          VARCHAR(30)   NOT NULL,
    description         VARCHAR(500)  NOT NULL,
    quantity_affected   NUMERIC(12,2),
    status              VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    resolution          VARCHAR(500),
    resolved_by_user_id BIGINT        REFERENCES users(id),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    resolved_at         TIMESTAMPTZ
);
CREATE INDEX idx_order_issues_order ON order_issues(order_id);
CREATE INDEX idx_order_issues_status ON order_issues(status);

-- Admin activity log
CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    action        VARCHAR(50)  NOT NULL,
    target_type   VARCHAR(30),
    target_id     BIGINT,
    details       VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_created ON audit_log(created_at DESC);

-- Standing (repeating) requests
CREATE TABLE standing_orders (
    id               BIGSERIAL PRIMARY KEY,
    shop_id          BIGINT        NOT NULL REFERENCES shop_profiles(id) ON DELETE CASCADE,
    crop_id          BIGINT        NOT NULL REFERENCES crops(id),
    quantity         NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    max_price        NUMERIC(12,2),
    frequency        VARCHAR(20)   NOT NULL,
    next_needed_date DATE          NOT NULL,
    lead_days        INT           NOT NULL DEFAULT 2,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    notes            VARCHAR(500),
    last_run_on      DATE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_standing_shop ON standing_orders(shop_id);

-- Support inbox
CREATE TABLE support_tickets (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject    VARCHAR(120) NOT NULL,
    category   VARCHAR(20)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tickets_user ON support_tickets(user_id);
CREATE INDEX idx_tickets_status ON support_tickets(status, updated_at DESC);

CREATE TABLE support_messages (
    id             BIGSERIAL PRIMARY KEY,
    ticket_id      BIGINT        NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    sender_user_id BIGINT        NOT NULL REFERENCES users(id),
    sender_role    VARCHAR(10)   NOT NULL,
    body           VARCHAR(1000) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_support_messages_ticket ON support_messages(ticket_id);
