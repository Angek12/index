-- Accounts: approval, language, notification preferences
ALTER TABLE users ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE users ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'en';
ALTER TABLE users ADD COLUMN notify_sms BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN notify_whatsapp BOOLEAN NOT NULL DEFAULT FALSE;

-- Cooperatives
ALTER TABLE farmer_profiles ADD COLUMN cooperative BOOLEAN NOT NULL DEFAULT FALSE;

-- Listings: quality, harvest, minimum order, pickup, photo
ALTER TABLE farm_supplies ADD COLUMN quality_grade VARCHAR(10);
ALTER TABLE farm_supplies ADD COLUMN harvest_date DATE;
ALTER TABLE farm_supplies ADD COLUMN min_order_quantity NUMERIC(12,2);
ALTER TABLE farm_supplies ADD COLUMN pickup_point VARCHAR(200);
ALTER TABLE farm_supplies ADD COLUMN photo_updated_at TIMESTAMPTZ;

CREATE TABLE supply_photos (
    supply_id    BIGINT PRIMARY KEY REFERENCES farm_supplies(id) ON DELETE CASCADE,
    content_type VARCHAR(40) NOT NULL,
    data         BYTEA NOT NULL
);

-- Orders: payment tracking and delivery method
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE orders ADD COLUMN payment_method VARCHAR(30);
ALTER TABLE orders ADD COLUMN payment_reference VARCHAR(80);
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN received_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN delivery_method VARCHAR(30) NOT NULL DEFAULT 'TO_BE_AGREED';
ALTER TABLE orders ADD COLUMN pickup_point VARCHAR(200);
ALTER TABLE orders ADD COLUMN transport_paid_by VARCHAR(20);

-- Ratings
CREATE TABLE order_ratings (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    rater_role VARCHAR(10)  NOT NULL,
    stars      INT          NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment    VARCHAR(300),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (order_id, rater_role)
);

-- In-app notifications
CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message    VARCHAR(500) NOT NULL,
    link       VARCHAR(100),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);

-- Password reset codes
CREATE TABLE password_resets (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash  VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_password_resets_user ON password_resets(user_id);

-- Team members for shops and cooperatives
CREATE TABLE org_members (
    id         BIGSERIAL PRIMARY KEY,
    org_type   VARCHAR(10) NOT NULL,
    org_id     BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_org_members_org ON org_members(org_type, org_id);
