CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE shop_profiles (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    shop_name VARCHAR(150) NOT NULL,
    district  VARCHAR(80),
    sector    VARCHAR(80),
    address   VARCHAR(255)
);

CREATE TABLE farmer_profiles (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    farm_name VARCHAR(150) NOT NULL,
    district  VARCHAR(80),
    sector    VARCHAR(80),
    address   VARCHAR(255)
);

CREATE TABLE crops (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(80) NOT NULL UNIQUE,
    category VARCHAR(50),
    unit     VARCHAR(20) NOT NULL DEFAULT 'kg',
    active   BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE sales_records (
    id         BIGSERIAL PRIMARY KEY,
    shop_id    BIGINT        NOT NULL REFERENCES shop_profiles(id) ON DELETE CASCADE,
    crop_id    BIGINT        NOT NULL REFERENCES crops(id),
    quantity   NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    sold_on    DATE          NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sales_shop_date ON sales_records(shop_id, sold_on);
CREATE INDEX idx_sales_crop_date ON sales_records(crop_id, sold_on);

CREATE TABLE demand_requests (
    id         BIGSERIAL PRIMARY KEY,
    shop_id    BIGINT        NOT NULL REFERENCES shop_profiles(id) ON DELETE CASCADE,
    crop_id    BIGINT        NOT NULL REFERENCES crops(id),
    quantity   NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    max_price  NUMERIC(12,2),
    needed_by  DATE          NOT NULL,
    status     VARCHAR(20)   NOT NULL,
    notes      VARCHAR(500),
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_demand_status ON demand_requests(status, needed_by);
CREATE INDEX idx_demand_shop ON demand_requests(shop_id);

CREATE TABLE farm_supplies (
    id                 BIGSERIAL PRIMARY KEY,
    farmer_id          BIGINT        NOT NULL REFERENCES farmer_profiles(id) ON DELETE CASCADE,
    crop_id            BIGINT        NOT NULL REFERENCES crops(id),
    quantity_available NUMERIC(12,2) NOT NULL CHECK (quantity_available >= 0),
    price_per_unit     NUMERIC(12,2) NOT NULL CHECK (price_per_unit >= 0),
    available_from     DATE          NOT NULL,
    available_until    DATE,
    status             VARCHAR(20)   NOT NULL,
    notes              VARCHAR(500),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_supply_crop_status ON farm_supplies(crop_id, status);
CREATE INDEX idx_supply_farmer ON farm_supplies(farmer_id);

CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    demand_request_id BIGINT        NOT NULL REFERENCES demand_requests(id),
    farm_supply_id    BIGINT        NOT NULL REFERENCES farm_supplies(id),
    shop_id           BIGINT        NOT NULL REFERENCES shop_profiles(id),
    farmer_id         BIGINT        NOT NULL REFERENCES farmer_profiles(id),
    crop_id           BIGINT        NOT NULL REFERENCES crops(id),
    quantity          NUMERIC(12,2) NOT NULL CHECK (quantity > 0),
    agreed_price      NUMERIC(12,2) NOT NULL CHECK (agreed_price >= 0),
    delivery_date     DATE          NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    transport_cost    NUMERIC(12,2),
    spoiled_quantity  NUMERIC(12,2),
    notes             VARCHAR(500),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_shop ON orders(shop_id);
CREATE INDEX idx_orders_farmer ON orders(farmer_id);

CREATE TABLE order_status_history (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT      NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status        VARCHAR(20),
    to_status          VARCHAR(20) NOT NULL,
    changed_by_user_id BIGINT      REFERENCES users(id),
    note               VARCHAR(500),
    changed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_history_order ON order_status_history(order_id);

INSERT INTO crops (name, category, unit) VALUES
 ('Tomatoes', 'Vegetable', 'kg'),
 ('Onions', 'Vegetable', 'kg'),
 ('Irish potatoes', 'Tuber', 'kg'),
 ('Cabbage', 'Vegetable', 'kg'),
 ('Carrots', 'Vegetable', 'kg'),
 ('Green peppers', 'Vegetable', 'kg'),
 ('Bananas', 'Fruit', 'kg');
