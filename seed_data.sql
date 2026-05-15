-- Update schema to support category images
ALTER TABLE product_categories ADD COLUMN IF NOT EXISTS image_id BIGINT;

-- Delete existing data in reverse order of relationships
DELETE FROM variant_images;
DELETE FROM product_images;
DELETE FROM images;
DELETE FROM product_variants;
DELETE FROM product_reviews;
DELETE FROM products;
DELETE FROM product_categories;
DELETE FROM users;
DELETE FROM app_user_roles;
DELETE FROM persons;

-- 1. Roles
INSERT INTO app_user_roles (user_role_name) VALUES
('ADMIN'),
('CUSTOMER');

-- 2. Persons
INSERT INTO persons (person_first_name, person_last_name, person_email, person_phone1) VALUES
('Admin', 'User', 'admin@monostudio.com', '0901234567'),
('Nguyen', 'Van A', 'vana@gmail.com', '0911111111');

-- 3. Users
INSERT INTO users (user_name, user_password, person_id, user_role_id) VALUES
('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qnO6CkS6EK7ZLBRvpy.O17.3Yh.tG.Pny2',
 (SELECT person_id FROM persons WHERE person_email = 'admin@monostudio.com'),
 (SELECT user_role_id FROM app_user_roles WHERE user_role_name = 'ADMIN')),
('vana', '$2a$10$8.UnVuG9HHgffUDAlk8qnO6CkS6EK7ZLBRvpy.O17.3Yh.tG.Pny2',
 (SELECT person_id FROM persons WHERE person_email = 'vana@gmail.com'),
 (SELECT user_role_id FROM app_user_roles WHERE user_role_name = 'CUSTOMER'));

-- 4. Images (Including new Category Banners)
INSERT INTO images (image_code, image_filename, image_url, image_alt_text, image_mime_type) VALUES
-- Category Banners
('cat-ban-men', 'men-banner.jpg', 'https://images.unsplash.com/photo-1490578474895-699cd4e2cf59?q=80&w=2000', 'Men Collection Banner', 'image/jpeg'),
('cat-ban-women', 'women-banner.jpg', 'https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=2000', 'Women Collection Banner', 'image/jpeg'),
('cat-ban-new', 'new-banner.jpg', 'https://images.unsplash.com/photo-1441984904996-e0b6ba687e04?q=80&w=2000', 'New Arrivals Banner', 'image/jpeg'),
('cat-ban-sale', 'sale-banner.jpg', 'https://images.unsplash.com/photo-1472851294608-062f824d29cc?q=80&w=2000', 'Season Sale Banner', 'image/jpeg'),

-- Product Images
('img-men-tsh-w', 'men-tshirt-white.jpg', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=1000', 'Men White Tshirt', 'image/jpeg'),
('img-men-tsh-b', 'men-tshirt-black.jpg', 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?q=80&w=1000', 'Men Black Heavy Tshirt', 'image/jpeg'),
('img-men-pol-n', 'men-polo-navy.jpg', 'https://images.unsplash.com/photo-1617137968427-85924c800a22?q=80&w=1000', 'Men Navy Polo', 'image/jpeg'),
('img-men-pan-b', 'men-chino-beige.jpg', 'https://images.unsplash.com/photo-1473966968600-fa804b86967b?q=80&w=1000', 'Men Beige Chino', 'image/jpeg'),
('img-women-tsh-c', 'women-crop-top.jpg', 'https://images.unsplash.com/photo-1554568218-0f1715e72254?q=80&w=1000', 'Women Black Crop Top', 'image/jpeg'),
('img-women-skr-m', 'women-midi-skirt.jpg', 'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?q=80&w=1000', 'Women Satin Skirt', 'image/jpeg');

-- 5. Product Categories (With Image Mapping)
-- Level 1
INSERT INTO product_categories (product_category_code, product_category_name, image_id, parent_product_category_id) VALUES
('men', 'Men', (SELECT image_id FROM images WHERE image_code = 'cat-ban-men'), NULL),
('women', 'Women', (SELECT image_id FROM images WHERE image_code = 'cat-ban-women'), NULL),
('new', 'New Arrivals', (SELECT image_id FROM images WHERE image_code = 'cat-ban-new'), NULL),
('sale', 'Sale', (SELECT image_id FROM images WHERE image_code = 'cat-ban-sale'), NULL);

-- Level 2 (Men)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('men-ao-thun', 'Áo thun', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men')),
('men-ao-polo', 'Áo polo', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men')),
('men-quan', 'Quần', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men')),
('men-phu-kien', 'Phụ kiện', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men'));

-- Level 3 (Men -> Áo thun)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('men-ao-thun-tron', 'Áo thun cổ tròn', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-ao-thun')),
('men-ao-thun-v', 'Áo thun cổ V', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-ao-thun')),
('men-ao-thun-oversize', 'Áo thun oversize', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-ao-thun'));

-- Level 3 (Men -> Áo polo)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('men-ao-polo-dung', 'Áo polo cổ đứng', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-ao-polo')),
('men-ao-polo-classic', 'Áo polo classic', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-ao-polo')),
('men-ao-polo-pique', 'Áo polo pique', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-ao-polo'));

-- Level 3 (Men -> Quần)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('men-quan-jogger', 'Quần jogger', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-quan')),
('men-quan-dai', 'Quần dài', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-quan')),
('men-quan-short', 'Quần short', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'men-quan'));

-- Level 2 (Women)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('women-ao-thun', 'Áo thun', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women')),
('women-ao-polo', 'Áo polo', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women')),
('women-quan', 'Quần', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women')),
('women-phu-kien', 'Phụ kiện', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women'));

-- Level 3 (Women -> Áo thun)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('women-ao-thun-tron', 'Áo thun cổ tròn', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-ao-thun')),
('women-ao-thun-v', 'Áo thun cổ V', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-ao-thun')),
('women-ao-thun-crop', 'Áo thun crop', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-ao-thun'));

-- Level 3 (Women -> Quần)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('women-quan-jogger', 'Quần jogger', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-quan')),
('women-quan-dai', 'Quần dài', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-quan')),
('women-quan-short', 'Quần short', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-quan')),
('women-quan-chan-vay', 'Chân váy', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'women-quan'));

-- Level 2 (New Arrivals)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('new-men', 'Men — Mùa mới', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'new')),
('new-women', 'Women — Mùa mới', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'new')),
('new-capsule', 'Bộ sưu tập Capsule', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'new'));

-- Level 3 (Capsule)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('mono-noir', 'Mono Noir', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'new-capsule')),
('urban-linen', 'Urban Linen', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'new-capsule')),
('weekend-core', 'Weekend Core', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'new-capsule'));

-- Level 2 (Sale)
INSERT INTO product_categories (product_category_code, product_category_name, parent_product_category_id) VALUES
('sale-men', 'Nam giảm giá', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'sale')),
('sale-women', 'Nữ giảm giá', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'sale')),
('sale-deal', 'Khung giờ sale', (SELECT product_category_id FROM (SELECT * FROM product_categories) pc WHERE pc.product_category_code = 'sale'));

-- 6. Products
-- Men's T-shirts
INSERT INTO products (product_name, product_code, product_description, product_price, product_stock_current, product_stock_critical, product_category_id, product_status) VALUES
('Áo thun Basic Cotton White', 'men-tsh-001', 'Áo thun nam classic cotton 100%, màu trắng tinh tế.', 290000, 100, 10, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-ao-thun-tron'), 'PUBLISHED'),
('Áo thun Heavyweight Black', 'men-tsh-002', 'Vải cotton dày dặn, form đứng dáng cực đẹp.', 350000, 80, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-ao-thun-oversize'), 'PUBLISHED'),
('Áo thun V-Neck Grey', 'men-tsh-003', 'Thiết kế cổ V trẻ trung, tôn dáng người mặc.', 290000, 50, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-ao-thun-v'), 'PUBLISHED');

-- Men's Polos
INSERT INTO products (product_name, product_code, product_description, product_price, product_stock_current, product_stock_critical, product_category_id, product_status) VALUES
('Áo Polo Pique Navy', 'men-pol-001', 'Áo polo chất liệu vải Pique cao cấp, thoáng mát.', 450000, 50, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-ao-polo-pique'), 'PUBLISHED'),
('Áo Polo Classic White', 'men-pol-002', 'Form polo classic lịch sự, phù hợp công sở.', 420000, 40, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-ao-polo-classic'), 'PUBLISHED');

-- Men's Pants
INSERT INTO products (product_name, product_code, product_description, product_price, product_stock_current, product_stock_critical, product_category_id, product_status) VALUES
('Quần Chino Slim Fit Beige', 'men-pan-001', 'Quần dài Chino form dáng hiện đại, màu beige dễ phối đồ.', 550000, 40, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-quan-dai'), 'PUBLISHED'),
('Quần Jogger Tech Black', 'men-pan-002', 'Chất liệu vải tech co giãn, cạp chun thoải mái.', 590000, 30, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'men-quan-jogger'), 'PUBLISHED');

-- Women's Products
INSERT INTO products (product_name, product_code, product_description, product_price, product_stock_current, product_stock_critical, product_category_id, product_status) VALUES
('Áo thun Crop Top Ribbed', 'women-tsh-001', 'Chất vải thun gân ôm sát, độ dài crop trẻ trung.', 250000, 60, 10, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'women-ao-thun-crop'), 'PUBLISHED'),
('Chân váy Midi Satin Black', 'women-skr-001', 'Chân váy lụa midi sang trọng, mềm mại.', 490000, 30, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'women-quan-chan-vay'), 'PUBLISHED'),
('Áo thun Oversize Women Grey', 'women-tsh-002', 'Form rộng thoải mái, phong cách streetwear.', 320000, 45, 5, (SELECT product_category_id FROM product_categories WHERE product_category_code = 'women-ao-thun-tron'), 'PUBLISHED');

-- 7. Product Variants
-- T-shirt Basic White (S, M, L, XL)
INSERT INTO product_variants (variant_sku, variant_size, variant_color, variant_price_modifier, variant_stock_current, variant_stock_critical, variant_stock_reserved, variant_version, variant_active, product_id) VALUES
('men-tsh-001-w-s', 'S', 'White', 0, 20, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-001')),
('men-tsh-001-w-m', 'M', 'White', 0, 30, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-001')),
('men-tsh-001-w-l', 'L', 'White', 0, 25, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-001')),
('men-tsh-001-w-xl', 'XL', 'White', 0, 15, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-001'));

-- Heavyweight Black
INSERT INTO product_variants (variant_sku, variant_size, variant_color, variant_price_modifier, variant_stock_current, variant_stock_critical, variant_stock_reserved, variant_version, variant_active, product_id) VALUES
('men-tsh-002-b-m', 'M', 'Black', 0, 20, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-002')),
('men-tsh-002-b-l', 'L', 'Black', 0, 30, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-002')),
('men-tsh-002-b-xl', 'XL', 'Black', 0, 20, 2, 0, 0, TRUE, (SELECT product_id FROM products WHERE product_code = 'men-tsh-002'));

-- 8. Product Images Mapping (Assigning Primary images)
INSERT INTO product_images (image_id, product_id, sort_order, is_primary) VALUES
((SELECT image_id FROM images WHERE image_code = 'img-men-tsh-w'), (SELECT product_id FROM products WHERE product_code = 'men-tsh-001'), 0, TRUE),
((SELECT image_id FROM images WHERE image_code = 'img-men-tsh-b'), (SELECT product_id FROM products WHERE product_code = 'men-tsh-002'), 0, TRUE),
((SELECT image_id FROM images WHERE image_code = 'img-men-pol-n'), (SELECT product_id FROM products WHERE product_code = 'men-pol-001'), 0, TRUE),
((SELECT image_id FROM images WHERE image_code = 'img-men-pan-b'), (SELECT product_id FROM products WHERE product_code = 'men-pan-001'), 0, TRUE),
((SELECT image_id FROM images WHERE image_code = 'img-women-tsh-c'), (SELECT product_id FROM products WHERE product_code = 'women-tsh-001'), 0, TRUE),
((SELECT image_id FROM images WHERE image_code = 'img-women-skr-m'), (SELECT product_id FROM products WHERE product_code = 'women-skr-001'), 0, TRUE);

-- 9. Product Reviews
INSERT INTO product_reviews (review_rating, review_title, review_body, review_approved, review_verified_purchase, customer_id, product_id, review_created_at, review_updated_at) VALUES
(5, 'Tuyệt vời', 'Chất vải rất mát, form rộng đúng ý mình luôn.', TRUE, TRUE, (SELECT customer_id FROM customers LIMIT 1), (SELECT product_id FROM products WHERE product_code = 'men-tsh-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'Đẹp nhưng hơi dài', 'Áo đẹp, vải dày dặn nhưng size M hơi dài so với mình.', TRUE, TRUE, (SELECT customer_id FROM customers LIMIT 1), (SELECT product_id FROM products WHERE product_code = 'men-tsh-002'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'Đáng tiền', 'Mua sale nên giá cực hời, giao hàng nhanh.', TRUE, TRUE, (SELECT customer_id FROM customers LIMIT 1), (SELECT product_id FROM products WHERE product_code = 'men-tsh-001'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 10. Review Images Support
CREATE TABLE IF NOT EXISTS review_images (
    review_image_id SERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    image_id BIGINT NOT NULL,
    CONSTRAINT fk_review FOREIGN KEY (review_id) REFERENCES product_reviews(review_id),
    CONSTRAINT fk_image FOREIGN KEY (image_id) REFERENCES images(image_id)
);

-- 11. Review Replies Support
CREATE TABLE IF NOT EXISTS product_review_replies (
    reply_id SERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    customer_id BIGINT,
    user_id BIGINT,
    reply_body TEXT NOT NULL,
    reply_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reply_review FOREIGN KEY (review_id) REFERENCES product_reviews(review_id),
    CONSTRAINT fk_reply_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CONSTRAINT fk_reply_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Seed some review images
INSERT INTO review_images (review_id, image_id) VALUES 
(1, (SELECT image_id FROM images WHERE image_code = 'img-men-tsh-b')),
(1, (SELECT image_id FROM images WHERE image_code = 'img-men-tsh-w')),
(2, (SELECT image_id FROM images WHERE image_code = 'img-women-tsh-c'));

-- Seed some review replies
INSERT INTO product_review_replies (review_id, user_id, reply_body) VALUES 
(1, 1, 'Dạ cảm ơn bạn đã ủng hộ Mono Studio ạ! Rất vui vì bạn hài lòng.'),
(2, 1, 'Chào bạn, kích thước oversize nên sẽ hơi dài một chút, bạn có thể inbox để shop tư vấn size chuẩn hơn nhé!');
