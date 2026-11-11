-- V1: database structure.
-- Flyway runs every V<number>__<name>.sql file ONCE, in order, and records it in the
-- table "flyway_schema_history". Never edit a migration that has already run:
-- to change the schema, add a new file (V3__..., V4__...).

CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    sku           VARCHAR(20)   NOT NULL UNIQUE,
    title         VARCHAR(200)  NOT NULL,
    brand         VARCHAR(100)  NOT NULL,
    category      VARCHAR(50)   NOT NULL,
    description   TEXT          NOT NULL,
    price         NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    rating        NUMERIC(2,1)  NOT NULL CHECK (rating BETWEEN 0 AND 5),
    reviews_count INTEGER       NOT NULL DEFAULT 0,
    stock         INTEGER       NOT NULL CHECK (stock >= 0),
    emoji         VARCHAR(16)   NOT NULL,
    color         VARCHAR(7)    NOT NULL
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,   -- BCrypt hash, never the plain password
    name          VARCHAR(100) NOT NULL,
    locked        BOOLEAN      NOT NULL DEFAULT FALSE,
    slow_login    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Emails are unique ignoring case: "A@x.test" and "a@x.test" are the same account.
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

-- Login tokens. A row exists while the user is logged in; logout deletes it.
CREATE TABLE auth_tokens (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One cart per user.
CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    promo_code VARCHAR(20)
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT  NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id BIGINT  NOT NULL REFERENCES products (id),
    quantity   INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    order_number      VARCHAR(20)   NOT NULL UNIQUE,     -- public id, e.g. SL-482913
    user_id           BIGINT        NOT NULL REFERENCES users (id),
    status            VARCHAR(20)   NOT NULL CHECK (status IN ('PROCESSING', 'CANCELLED')),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    delivery_method   VARCHAR(20)   NOT NULL CHECK (delivery_method IN ('STANDARD', 'EXPRESS')),
    payment_method    VARCHAR(20)   NOT NULL CHECK (payment_method IN ('CARD', 'COD')),
    card_last4        VARCHAR(4),                         -- we never store the full card number
    promo_code        VARCHAR(20),
    subtotal          NUMERIC(10,2) NOT NULL,
    discount          NUMERIC(10,2) NOT NULL,
    shipping_cost     NUMERIC(10,2) NOT NULL,
    total             NUMERIC(10,2) NOT NULL,
    ship_full_name    VARCHAR(100)  NOT NULL,
    ship_address      VARCHAR(200)  NOT NULL,
    ship_city         VARCHAR(100)  NOT NULL,
    ship_zip          VARCHAR(5)    NOT NULL,
    ship_country      VARCHAR(60)   NOT NULL,
    ship_phone        VARCHAR(20)   NOT NULL
);
CREATE INDEX ix_orders_user ON orders (user_id);

-- Each line of an order. Title and price are COPIED from the product at purchase time,
-- so later price changes don't rewrite order history.
CREATE TABLE order_items (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id    BIGINT        NOT NULL REFERENCES products (id),
    product_title VARCHAR(200)  NOT NULL,
    unit_price    NUMERIC(10,2) NOT NULL,
    quantity      INTEGER       NOT NULL CHECK (quantity > 0),
    line_total    NUMERIC(10,2) NOT NULL
);
