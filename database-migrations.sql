-- ============================================================
-- Mono Studio — Database Migration Script
-- Target: PostgreSQL
-- Run this after Hibernate auto-migration (ddl-auto=update)
-- ============================================================

BEGIN;

-- ============================================================
-- 1. Orders — cart_session_token
-- ============================================================
-- Tracks which CartSession an order originated from.
-- Enables per-variant stock confirmation in markAsPaid()
-- without accidentally deducting the same reservation twice.

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_cart_session_token VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_orders_cart_session_token
    ON orders (order_cart_session_token)
    WHERE order_cart_session_token IS NOT NULL;


-- ============================================================
-- 2. Orders — discount tracking
-- ============================================================
-- Persists the applied discount so it survives the flow:
-- CheckoutServiceImpl → OrdersProcessServiceImpl (redeemDiscount
-- was moved from checkout-start to payment-confirmed).

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_discount_code VARCHAR(50);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_discount_value INTEGER NOT NULL DEFAULT 0;


-- ============================================================
-- 3. Order Details — product_variant_id
-- ============================================================
-- Links each order line item to the specific ProductVariant
-- purchased (size + colour).

ALTER TABLE order_details
    ADD COLUMN IF NOT EXISTS product_variant_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_order_details_product_variant_id
    ON order_details (product_variant_id)
    WHERE product_variant_id IS NOT NULL;

-- NOTE: FK intentionally omitted (entity uses NO_CONSTRAINT).
-- Run manually if your DB policy requires it:
-- ALTER TABLE order_details
--     ADD CONSTRAINT fk_order_details_product_variant
--     FOREIGN KEY (product_variant_id)
--     REFERENCES product_variants (variant_id)
--     ON DELETE SET NULL;


-- ============================================================
-- 4. Cart Sessions — session_customer_id
-- ============================================================
-- Links a CartSession to a registered Customer so the cart
-- persists across devices for logged-in users.

ALTER TABLE cart_sessions
    ADD COLUMN IF NOT EXISTS session_customer_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_cart_sessions_customer_id
    ON cart_sessions (session_customer_id)
    WHERE session_customer_id IS NOT NULL;

-- NOTE: FK intentionally omitted (entity uses ON DELETE SET NULL).
-- Run manually if your DB policy requires it:
-- ALTER TABLE cart_sessions
--     ADD CONSTRAINT fk_cart_sessions_customer
--     FOREIGN KEY (session_customer_id)
--     REFERENCES customers (customer_id)
--     ON DELETE SET NULL;


-- ============================================================
-- 5. Product Variants — ensure all columns exist
-- ============================================================

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_sku VARCHAR(100) UNIQUE;

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_size VARCHAR(50);

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_color VARCHAR(50);

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_price_modifier INTEGER NOT NULL DEFAULT 0;

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_stock_current INTEGER NOT NULL DEFAULT 0;

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_stock_reserved INTEGER NOT NULL DEFAULT 0;

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_stock_critical INTEGER NOT NULL DEFAULT 5;

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_active BOOLEAN NOT NULL DEFAULT TRUE;


-- ============================================================
-- 6. Discount Codes — ensure all columns exist
-- ============================================================

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_code VARCHAR(50) UNIQUE NOT NULL;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_description VARCHAR(200);

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_type VARCHAR(20) NOT NULL;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_value INTEGER NOT NULL DEFAULT 0;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_max_uses INTEGER;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_use_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_max_uses_per_customer INTEGER;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_min_cart_value INTEGER;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_valid_from TIMESTAMP;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_valid_until TIMESTAMP;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS discount_active BOOLEAN NOT NULL DEFAULT TRUE;


-- ============================================================
-- 7. Stock Reservations — ensure all columns exist
-- ============================================================

ALTER TABLE stock_reservations
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(64) NOT NULL;

ALTER TABLE stock_reservations
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'RESERVED';

ALTER TABLE stock_reservations
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_stock_reservations_session_status
    ON stock_reservations (session_id, status)
    WHERE status = 'RESERVED';


-- ============================================================
-- 8. Seed Data — discount codes (optional)
-- ============================================================
-- Comment out or modify for production.

INSERT INTO discount_codes (discount_code, discount_description, discount_type,
    discount_value, discount_max_uses, discount_use_count, discount_max_uses_per_customer,
    discount_min_cart_value, discount_valid_from, discount_valid_until, discount_active)
VALUES
    ('WELCOME10', '10% off your first order',           'PERCENTAGE',    10, NULL, 0, 1, 50000,  '2026-01-01', '2026-12-31', TRUE),
    ('SUMMER20', '20% summer sale',                     'PERCENTAGE',    20, 500, 0, 2, NULL,    '2026-01-01', '2026-12-31', TRUE),
    ('FREESHIP',  'Free shipping on any order',         'FREE_SHIPPING', 0,  100, 0, NULL, NULL, '2026-01-01', '2026-12-31', TRUE),
    ('SAVE50K',   '50.000 VND off your order',           'FIXED_AMOUNT',  50000, 200, 0, 3, NULL, '2026-01-01', '2026-12-31', TRUE)
ON CONFLICT (discount_code) DO NOTHING;


-- ============================================================
-- 9. Seed Data — shipping methods (optional)
-- ============================================================

INSERT INTO shipping_methods (shipping_method_name, shipping_method_base_fee,
    shipping_method_free_shipping_threshold, shipping_method_estimated_days_min,
    shipping_method_estimated_days_max, shipping_method_active)
VALUES
    ('Giao hàng tiêu chuẩn', 25000, 200000, 3, 5, TRUE),
    ('Giao hàng nhanh',       45000, 350000, 1, 2, TRUE),
    ('Giao hàng hỏa tốc',    80000, NULL,   0, 0, TRUE)
ON CONFLICT (shipping_method_name) DO NOTHING;

-- ============================================================
-- 10. Orders — drop legacy status FK columns
-- ============================================================
-- The application now uses textual statuses:
--   - fulfillment_status
--   - payment_status
-- Legacy FK columns (order_status_id/payment_status_id) can break inserts
-- when left as NOT NULL in existing databases.
ALTER TABLE orders
    DROP COLUMN IF EXISTS order_status_id;

ALTER TABLE orders
    DROP COLUMN IF EXISTS payment_status_id;

-- ============================================================
-- 11. Promotion Rules — scope for pricing separation
-- ============================================================
ALTER TABLE promotion_rules
    ADD COLUMN IF NOT EXISTS rule_scope VARCHAR(20) NOT NULL DEFAULT 'CART';

COMMIT;

-- ============================================================
-- Verify (run separately if needed)
-- ============================================================
-- SELECT table_name, column_name, data_type
-- FROM information_schema.columns
-- WHERE table_name IN (
--     'orders','order_details','cart_sessions',
--     'product_variants','discount_codes',
--     'stock_reservations','shipping_methods'
-- )
-- ORDER BY table_name, ordinal_position;
