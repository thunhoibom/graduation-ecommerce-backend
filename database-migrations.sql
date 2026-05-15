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

-- ============================================================
-- 12. User behavior events (storefront personalization)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_behavior_events (
    event_id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    device_id VARCHAR(64) NOT NULL,
    customer_id BIGINT,
    event_type VARCHAR(40) NOT NULL,
    payload TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ube_device_created
    ON user_behavior_events (device_id, created_at DESC);

-- ============================================================
-- 13. Persons — person_id_number optional (OAuth / no national ID)
-- ============================================================
ALTER TABLE persons
    ALTER COLUMN person_id_number DROP NOT NULL;

-- ============================================================
-- 14. Blog posts — publishing/SEO baseline
-- ============================================================
CREATE TABLE IF NOT EXISTS blog_posts (
    post_id BIGSERIAL PRIMARY KEY,
    post_title VARCHAR(200) NOT NULL,
    post_slug VARCHAR(200) NOT NULL UNIQUE,
    post_summary VARCHAR(500),
    post_content TEXT NOT NULL,
    post_thumbnail_url VARCHAR(1000),
    post_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    author_user_id BIGINT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_title VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_slug VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_summary VARCHAR(500);
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_content TEXT NOT NULL DEFAULT '';
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_thumbnail_url VARCHAR(1000);
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS post_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS author_user_id BIGINT;
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE blog_posts
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

CREATE UNIQUE INDEX IF NOT EXISTS idx_blog_posts_slug
    ON blog_posts (post_slug);
CREATE INDEX IF NOT EXISTS idx_blog_posts_published
    ON blog_posts (post_status, published_at DESC);

ALTER TABLE blog_posts
    ALTER COLUMN post_content TYPE TEXT
    USING post_content::text;

UPDATE blog_posts bp
SET post_content = convert_from(lo_get(bp.post_content::oid), 'UTF8')
WHERE bp.post_content ~ '^[0-9]+$'
  AND EXISTS (
      SELECT 1
      FROM pg_largeobject_metadata lom
      WHERE lom.oid = bp.post_content::oid
  );

-- ============================================================
-- 15. stock_adjustments — reason CHECK must match Java enum
-- ============================================================
-- Older DBs only allowed a subset of reasons; PO receipt / transfers /
-- stock count use values that must be listed here or inserts fail with:
--   violates check constraint "stock_adjustments_stock_adjustment_reason_check"

ALTER TABLE stock_adjustments
    DROP CONSTRAINT IF EXISTS stock_adjustments_stock_adjustment_reason_check;

ALTER TABLE stock_adjustments
    ADD CONSTRAINT stock_adjustments_stock_adjustment_reason_check
    CHECK (stock_adjustment_reason IN (
        'RESERVATION_CREATED',
        'RESERVATION_RELEASED',
        'PAYMENT_CONFIRMED',
        'PAYMENT_ABORTED',
        'RETURN_RESTORED',
        'MANUAL_ADJUSTMENT',
        'STOCK_RECOUNT',
        'ORDER_CANCELLED',
        'ORDER_REJECTED',
        'PURCHASE_ORDER_RECEIPT',
        'TRANSFER_OUTBOUND',
        'TRANSFER_INBOUND',
        'STOCK_COUNT_VARIANCE'
    ));

-- ============================================================
-- Contact inquiries — storefront contact form submissions
-- ============================================================
-- Run as the same DB role as spring.datasource.username (or after migration run:
--   ALTER TABLE contact_inquiries OWNER TO your_app_user;
-- ) so Hibernate ddl-auto and migrations agree on ownership.

CREATE TABLE IF NOT EXISTS contact_inquiries (
    contact_inquiry_id       BIGSERIAL PRIMARY KEY,
    contact_inquiry_created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    contact_inquiry_name     VARCHAR(200) NOT NULL,
    contact_inquiry_email    VARCHAR(254) NOT NULL,
    contact_inquiry_phone    VARCHAR(40),
    contact_inquiry_subject  VARCHAR(64) NOT NULL,
    contact_inquiry_message  TEXT NOT NULL,
    contact_inquiry_submitter_ip VARCHAR(64),
    contact_inquiry_user_agent VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_contact_inquiries_created_at
    ON contact_inquiries (contact_inquiry_created_at DESC);

-- App JDBC user must be able to INSERT (and read sequence for BIGSERIAL).
-- Replace nguyendd if spring.datasource.username differs. Run as table owner or superuser.
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE contact_inquiries TO nguyendd;
GRANT USAGE, SELECT ON SEQUENCE contact_inquiries_contact_inquiry_id_seq TO nguyendd;

-- ============================================================
-- Admin permissions — shipping-methods:* (Spring @PreAuthorize)
-- ============================================================
-- Older seeds only define shippers:* ; the shipping-methods API expects
-- shipping-methods:create|update|delete. DataShippingMethodsController also
-- accepts shippers:* as an alias; this block adds explicit codes for JWT clarity.

WITH seed_rows AS (
    SELECT v.code, v.permission_desc AS description
    FROM (VALUES
        ('shipping-methods:read', 'Read shipping methods'),
        ('shipping-methods:create', 'Create shipping methods'),
        ('shipping-methods:update', 'Update shipping methods'),
        ('shipping-methods:delete', 'Delete shipping methods')
    ) AS v(code, permission_desc)
    WHERE NOT EXISTS (
        SELECT 1
        FROM app_permissions ap
        WHERE ap.permission_code = v.code
    )
),
next_id AS (
    SELECT COALESCE(MAX(permission_id), 0) AS base_id
    FROM app_permissions
),
numbered_rows AS (
    SELECT
        seed_rows.code,
        seed_rows.description,
        next_id.base_id + ROW_NUMBER() OVER (ORDER BY seed_rows.code) AS permission_id
    FROM seed_rows
    CROSS JOIN next_id
)
INSERT INTO app_permissions (permission_id, permission_code, permission_description)
SELECT permission_id, code, description
FROM numbered_rows;

SELECT setval(
    pg_get_serial_sequence('app_permissions', 'permission_id'),
    (SELECT COALESCE(MAX(permission_id), 1) FROM app_permissions)
);

INSERT INTO app_user_role_permissions (permission_id, user_role_id)
SELECT p.permission_id, ur.user_role_id
FROM app_permissions p
CROSS JOIN app_user_roles ur
WHERE p.permission_code LIKE 'shipping-methods:%'
  AND ur.user_role_name IN ('ADMIN', 'Administrator')
  AND NOT EXISTS (
    SELECT 1 FROM app_user_role_permissions urp
    WHERE urp.permission_id = p.permission_id AND urp.user_role_id = ur.user_role_id
  );

-- ============================================================
-- Return requests — warehouse QC
-- ============================================================
-- Physical receipt (RECEIVED) no longer restores stock immediately.
-- Stock is added back only after QC passes.

ALTER TABLE return_requests
    ADD COLUMN IF NOT EXISTS return_request_qc_status VARCHAR(32);

ALTER TABLE return_requests
    ADD COLUMN IF NOT EXISTS return_request_qc_notes VARCHAR(2000);

ALTER TABLE return_requests
    ADD COLUMN IF NOT EXISTS return_request_qc_photo_urls VARCHAR(2048);

ALTER TABLE return_requests
    ADD COLUMN IF NOT EXISTS return_request_qc_completed_at TIMESTAMPTZ;

UPDATE return_requests
SET return_request_qc_status = 'PASSED'
WHERE return_request_status IN ('RECEIVED', 'REFUND_PROCESSING', 'REFUND_COMPLETED')
  AND return_request_qc_status IS NULL;

-- ============================================================
-- 14. Canonical customer email (dedupe + unique normalized email)
-- ============================================================

UPDATE persons
SET person_email = LOWER(BTRIM(person_email))
WHERE person_email IS NOT NULL;

CREATE TEMP TABLE tmp_duplicate_customers ON COMMIT DROP AS
WITH ranked_customers AS (
    SELECT
        c.customer_id,
        ROW_NUMBER() OVER (
            PARTITION BY LOWER(BTRIM(p.person_email))
            ORDER BY
                CASE WHEN u.user_id IS NOT NULL THEN 0 ELSE 1 END,
                c.customer_id
        ) AS row_number,
        FIRST_VALUE(c.customer_id) OVER (
            PARTITION BY LOWER(BTRIM(p.person_email))
            ORDER BY
                CASE WHEN u.user_id IS NOT NULL THEN 0 ELSE 1 END,
                c.customer_id
        ) AS canonical_customer_id
    FROM customers c
    JOIN persons p ON p.person_id = c.person_id
    LEFT JOIN users u ON u.person_id = p.person_id
    WHERE p.person_email IS NOT NULL
      AND BTRIM(p.person_email) <> ''
)
SELECT
    customer_id AS duplicate_customer_id,
    canonical_customer_id
FROM ranked_customers
WHERE row_number > 1;

UPDATE orders o
SET customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE o.customer_id = d.duplicate_customer_id;

UPDATE cart_sessions cs
SET session_customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE cs.session_customer_id = d.duplicate_customer_id;

UPDATE guest_sessions gs
SET customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE gs.customer_id = d.duplicate_customer_id;

UPDATE loyalty_points_ledger l
SET loyalty_customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE l.loyalty_customer_id = d.duplicate_customer_id;

DELETE FROM discount_usages du
USING tmp_duplicate_customers d
WHERE du.discount_usage_customer_id = d.duplicate_customer_id
  AND EXISTS (
      SELECT 1
      FROM discount_usages existing
      WHERE existing.discount_usage_customer_id = d.canonical_customer_id
        AND existing.discount_usage_discount_id = du.discount_usage_discount_id
  );

UPDATE discount_usages du
SET discount_usage_customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE du.discount_usage_customer_id = d.duplicate_customer_id;

UPDATE product_reviews pr
SET customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE pr.customer_id = d.duplicate_customer_id;

UPDATE product_review_replies prr
SET customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE prr.customer_id = d.duplicate_customer_id;

UPDATE user_behavior_events ube
SET customer_id = d.canonical_customer_id
FROM tmp_duplicate_customers d
WHERE ube.customer_id = d.duplicate_customer_id;

DELETE FROM customers c
USING tmp_duplicate_customers d
WHERE c.customer_id = d.duplicate_customer_id;

CREATE TEMP TABLE tmp_duplicate_persons ON COMMIT DROP AS
WITH ranked_persons AS (
    SELECT
        p.person_id,
        ROW_NUMBER() OVER (
            PARTITION BY LOWER(BTRIM(p.person_email))
            ORDER BY
                CASE WHEN u.user_id IS NOT NULL THEN 0 ELSE 1 END,
                CASE WHEN c.customer_id IS NOT NULL THEN 0 ELSE 1 END,
                p.person_id
        ) AS row_number,
        FIRST_VALUE(p.person_id) OVER (
            PARTITION BY LOWER(BTRIM(p.person_email))
            ORDER BY
                CASE WHEN u.user_id IS NOT NULL THEN 0 ELSE 1 END,
                CASE WHEN c.customer_id IS NOT NULL THEN 0 ELSE 1 END,
                p.person_id
        ) AS canonical_person_id
    FROM persons p
    LEFT JOIN users u ON u.person_id = p.person_id
    LEFT JOIN customers c ON c.person_id = p.person_id
    WHERE p.person_email IS NOT NULL
      AND BTRIM(p.person_email) <> ''
)
SELECT
    person_id AS duplicate_person_id,
    canonical_person_id
FROM ranked_persons
WHERE row_number > 1;

UPDATE users u
SET person_id = d.canonical_person_id
FROM tmp_duplicate_persons d
WHERE u.person_id = d.duplicate_person_id;

UPDATE customers c
SET person_id = d.canonical_person_id
FROM tmp_duplicate_persons d
WHERE c.person_id = d.duplicate_person_id;

DELETE FROM persons p
USING tmp_duplicate_persons d
WHERE p.person_id = d.duplicate_person_id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_persons_email_normalized
    ON persons (LOWER(BTRIM(person_email)))
    WHERE person_email IS NOT NULL AND BTRIM(person_email) <> '';

-- ============================================================
-- Admin permissions — params:* (Settings / system parameters)
-- ============================================================

WITH seed_rows AS (
    SELECT v.code, v.permission_desc AS description
    FROM (VALUES
        ('params:read', 'Read system parameters'),
        ('params:create', 'Create system parameters'),
        ('params:update', 'Update system parameters'),
        ('params:delete', 'Delete system parameters')
    ) AS v(code, permission_desc)
    WHERE NOT EXISTS (
        SELECT 1
        FROM app_permissions ap
        WHERE ap.permission_code = v.code
    )
),
next_id AS (
    SELECT COALESCE(MAX(permission_id), 0) AS base_id
    FROM app_permissions
),
numbered_rows AS (
    SELECT
        seed_rows.code,
        seed_rows.description,
        next_id.base_id + ROW_NUMBER() OVER (ORDER BY seed_rows.code) AS permission_id
    FROM seed_rows
    CROSS JOIN next_id
)
INSERT INTO app_permissions (permission_id, permission_code, permission_description)
SELECT permission_id, code, description
FROM numbered_rows;

SELECT setval(
    pg_get_serial_sequence('app_permissions', 'permission_id'),
    (SELECT COALESCE(MAX(permission_id), 1) FROM app_permissions)
);

INSERT INTO app_user_role_permissions (permission_id, user_role_id)
SELECT p.permission_id, ur.user_role_id
FROM app_permissions p
CROSS JOIN app_user_roles ur
WHERE p.permission_code LIKE 'params:%'
  AND ur.user_role_name IN ('ADMIN', 'Administrator')
  AND NOT EXISTS (
    SELECT 1 FROM app_user_role_permissions urp
    WHERE urp.permission_id = p.permission_id AND urp.user_role_id = ur.user_role_id
  );

-- ============================================================
-- Default app_params seeds for admin Settings
-- ============================================================

WITH seed_rows AS (
    SELECT v.category, v.name, v.value
    FROM (VALUES
        ('company', 'name', 'Mono Studio'),
        ('company', 'description', 'Thương mại điện tử tích hợp — đồ án tốt nghiệp Mono Studio'),
        ('company', 'bannerImageURL', 'https://fakeimg.pl/1200x400'),
        ('company', 'bannerImageURLs', '["https://fakeimg.pl/1200x400","https://fakeimg.pl/1200x400/cccccc/"]'),
        ('company', 'logoImageURL', 'https://fakeimg.pl/250'),
        ('payment', 'cod_enabled', 'true'),
        ('payment', 'payos_enabled', 'true'),
        ('payment', 'payment_timeout_minutes', '30'),
        ('system', 'support_email', 'support@monostudio.local'),
        ('system', 'support_phone', '1900 0000'),
        ('system', 'maintenance_mode', 'false'),
        ('personalization', 'category_boost', '1.2'),
        ('personalization', 'cart_boost', '1.5'),
        ('personalization', 'purchase_boost', '2.0'),
        ('personalization', 'fallback_boost', '0.5'),
        ('variant_option_set', 'size', 'XS,S,M,L,XL,XXL,XXXL'),
        ('variant_option_set', 'color', 'Đen,Trắng,Xanh,Đỏ,Vàng,Be,Nâu,Xám,Hồng,Tím')
    ) AS v(category, name, value)
    WHERE NOT EXISTS (
        SELECT 1
        FROM app_params p
        WHERE p.param_category = v.category
          AND p.param_name = v.name
    )
),
next_id AS (
    SELECT COALESCE(MAX(param_id), 0) AS base_id
    FROM app_params
),
numbered_rows AS (
    SELECT
        seed_rows.category,
        seed_rows.name,
        seed_rows.value,
        next_id.base_id + ROW_NUMBER() OVER (ORDER BY seed_rows.category, seed_rows.name) AS param_id
    FROM seed_rows
    CROSS JOIN next_id
)
INSERT INTO app_params (param_id, param_category, param_name, param_value)
SELECT param_id, category, name, value
FROM numbered_rows;

SELECT setval(
    pg_get_serial_sequence('app_params', 'param_id'),
    (SELECT COALESCE(MAX(param_id), 1) FROM app_params)
);

-- ============================================================
-- Admin permissions — users:* and user_roles:*
-- ============================================================

WITH seed_rows AS (
    SELECT v.code, v.permission_desc AS description
    FROM (VALUES
        ('users:read', 'Read admin users'),
        ('users:create', 'Create admin users'),
        ('users:update', 'Update admin users'),
        ('users:delete', 'Delete admin users'),
        ('user_roles:read', 'Read user roles'),
        ('user_roles:create', 'Create user roles'),
        ('user_roles:update', 'Update user roles'),
        ('user_roles:delete', 'Delete user roles')
    ) AS v(code, permission_desc)
    WHERE NOT EXISTS (
        SELECT 1
        FROM app_permissions ap
        WHERE ap.permission_code = v.code
    )
),
next_id AS (
    SELECT COALESCE(MAX(permission_id), 0) AS base_id
    FROM app_permissions
),
numbered_rows AS (
    SELECT
        seed_rows.code,
        seed_rows.description,
        next_id.base_id + ROW_NUMBER() OVER (ORDER BY seed_rows.code) AS permission_id
    FROM seed_rows
    CROSS JOIN next_id
)
INSERT INTO app_permissions (permission_id, permission_code, permission_description)
SELECT permission_id, code, description
FROM numbered_rows;

SELECT setval(
    pg_get_serial_sequence('app_permissions', 'permission_id'),
    (SELECT COALESCE(MAX(permission_id), 1) FROM app_permissions)
);

INSERT INTO app_user_role_permissions (permission_id, user_role_id)
SELECT p.permission_id, ur.user_role_id
FROM app_permissions p
CROSS JOIN app_user_roles ur
WHERE (p.permission_code LIKE 'users:%' OR p.permission_code LIKE 'user_roles:%')
  AND ur.user_role_name IN ('ADMIN', 'Administrator')
  AND NOT EXISTS (
    SELECT 1
    FROM app_user_role_permissions urp
    WHERE urp.permission_id = p.permission_id
      AND urp.user_role_id = ur.user_role_id
  );

-- ============================================================
-- Product categories — display order
-- ============================================================
ALTER TABLE product_categories
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;

UPDATE product_categories
SET display_order = product_category_id
WHERE display_order = 0;

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
