-- =====================================================================
-- DML: 種子資料 (INSERT 預設值與測試帳號)
-- =====================================================================
-- 此檔由 docker-compose 透過 docker-entrypoint-initdb.d 在 ddl.sql 之後
-- 自動執行(`docker-entrypoint-initdb.d` 按檔名字母順序跑,所以 docker-compose
-- 把它掛成 `02-dml.sql` 確保排在 `01-ddl.sql` 後面)。
--
-- 改種子資料 = 編輯本檔 + `docker-compose down -v && docker-compose up -d`
-- =====================================================================

BEGIN;

-- ============================================================
-- 1. VIP 等級
-- ============================================================
INSERT INTO vip_levels (name, minimum_points, discount_percentage, benefits) VALUES
('Bronze', 0, 0, '基礎會員'),
('Silver', 1000, 5, '5%折扣,生日優惠'),
('Gold', 5000, 10, '10%折扣,免運費,優先客服'),
('Platinum', 15000, 15, '15%折扣,專屬優惠,VIP活動');

-- ============================================================
-- 2. 訂單狀態與付款方式
-- ============================================================
INSERT INTO order_status (name, description, color, sort_order) VALUES
('pending', '待處理', '#ffc107', 1),
('confirmed', '已確認', '#17a2b8', 2),
('processing', '處理中', '#007bff', 3),
('shipped', '已出貨', '#fd7e14', 4),
('delivered', '已送達', '#28a745', 5),
('cancelled', '已取消', '#dc3545', 6),
('refunded', '已退款', '#6c757d', 7);

INSERT INTO order_payment (name, description) VALUES
('信用卡', '信用卡線上支付'),
('綠界付款', '綠界第三方支付'),
('轉帳', '銀行轉帳'),
('貨到付款', '送達時付款');

-- ============================================================
-- 3. 品牌 / 分類
-- ============================================================
-- 圖片使用 placehold.co 公開 placeholder,dev 環境免準備本地檔。
-- 真實上線時應替換為 /uploads/... 或 CDN URL。
INSERT INTO brand (name, description, logo_url) VALUES
('PetCare',     '專業寵物護理品牌', 'https://placehold.co/200x100/cccccc/333333?text=PetCare'),
('DogLife',     '狗狗生活用品',     'https://placehold.co/200x100/cccccc/333333?text=DogLife'),
('NutriPet',    '營養寵物食品',     'https://placehold.co/200x100/cccccc/333333?text=NutriPet'),
('PlayTime',    '寵物玩具專家',     'https://placehold.co/200x100/cccccc/333333?text=PlayTime'),
('HealthyPaws', '寵物健康護理',     'https://placehold.co/200x100/cccccc/333333?text=HealthyPaws');

INSERT INTO category (name, description, image_url) VALUES
('食品', '寵物食品類',   'https://placehold.co/300x200/f0c060/333333?text=Food'),
('玩具', '寵物玩具類',   'https://placehold.co/300x200/f0c060/333333?text=Toys'),
('護理', '寵物護理用品', 'https://placehold.co/300x200/f0c060/333333?text=Care'),
('服飾', '寵物服飾配件', 'https://placehold.co/300x200/f0c060/333333?text=Clothing'),
('健康', '寵物健康用品', 'https://placehold.co/300x200/f0c060/333333?text=Health');

-- ============================================================
-- 4. 商品 + 商品圖
-- ============================================================
INSERT INTO product (name, description, price, sale_price, sku, stock_quantity, brand_id, category_id, weight) VALUES
('高級狗糧 2kg',  '營養均衡的高品質狗糧,適合成犬', 899.00, 799.00, 'DOG-FOOD-001',    50, 3, 1, 2.0),
('貓咪互動玩具',  '智能互動球,讓貓咪自己玩耍',     299.00, NULL,   'CAT-TOY-001',     30, 4, 2, 0.2),
('寵物洗毛精',    '溫和配方,適合敏感肌膚',         199.00, 179.00, 'PET-SHAMPOO-001', 25, 1, 3, 0.5),
('狗狗雨衣',      '防水透氣,多種尺寸',             399.00, NULL,   'DOG-CLOTH-001',   15, 2, 4, 0.3),
('維生素補充劑',  '增強免疫力,天然成分',           599.00, 549.00, 'PET-VIT-001',     40, 5, 5, 0.1);

INSERT INTO product_images (product_id, image_url, alt_text, is_primary, sort_order) VALUES
(1, 'https://placehold.co/400x400/dddddd/333333?text=Product+1-1', '高級狗糧主圖',   TRUE,  1),
(1, 'https://placehold.co/400x400/dddddd/333333?text=Product+1-2', '狗糧包裝背面',   FALSE, 2),
(2, 'https://placehold.co/400x400/dddddd/333333?text=Product+2-1', '貓咪玩具主圖',   TRUE,  1),
(3, 'https://placehold.co/400x400/dddddd/333333?text=Product+3-1', '寵物洗毛精',     TRUE,  1),
(4, 'https://placehold.co/400x400/dddddd/333333?text=Product+4-1', '狗狗雨衣',       TRUE,  1),
(5, 'https://placehold.co/400x400/dddddd/333333?text=Product+5-1', '維生素補充劑',   TRUE,  1);

-- ============================================================
-- 5. 測試帳號 (密碼: password)
-- ============================================================
INSERT INTO member (role, username, realname, email, password, phone, gender, email_validated, vip_levels_id) VALUES
('USER',  'testuser', '測試用戶',    'test@example.com',  '$2b$10$eDcdZCnC2/nCYZeOgbiVlOxbafGhbcwhUYrWxcJr51cheE/g5IC5a', '0912345678', 'male',   TRUE, 1),
('ADMIN', 'admin',    '系統管理員',  'admin@example.com', '$2b$10$eDcdZCnC2/nCYZeOgbiVlOxbafGhbcwhUYrWxcJr51cheE/g5IC5a', '0987654321', 'female', TRUE, 4);

COMMIT;
