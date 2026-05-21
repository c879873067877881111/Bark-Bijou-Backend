-- =====================================================================
-- DDL: 完整 schema (CREATE TABLE / ALTER / CREATE INDEX)
-- =====================================================================
-- 此檔由 docker-compose 透過 docker-entrypoint-initdb.d 在 postgres
-- container **第一次起來且 volume 為空時**自動執行。
--
-- schema 改動 = 編輯本檔 + `docker-compose down -v && docker-compose up -d`
-- (測試資料會被洗掉,dml.sql 會重跑種子)
-- =====================================================================

BEGIN;

-- 清理現有 schema(只在重跑時生效;第一次起 container 時 schema 是空的)
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

-- ============================================================
-- 1. 無依賴表
-- ============================================================

CREATE TABLE vip_levels (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  minimum_points INTEGER DEFAULT 0,
  discount_percentage DECIMAL(5,2) DEFAULT 0,
  benefits TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE brand (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  logo_url VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE category (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  parent_id INTEGER REFERENCES category(id),
  image_url VARCHAR(255),
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_status (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(255),
  color VARCHAR(7),
  sort_order INTEGER DEFAULT 0
);

CREATE TABLE order_payment (
  id SERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(255),
  is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE product_tags (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. member(暫不加 vip_levels FK,稍後 ALTER)
-- ============================================================

CREATE TABLE member (
  id SERIAL PRIMARY KEY,
  role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN', 'SITTER')),
  username VARCHAR(255) NOT NULL UNIQUE,
  realname VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(10),
  gender VARCHAR(10) CHECK (gender IN ('male', 'female')),
  birth_date DATE,
  vip_levels_id INTEGER NOT NULL DEFAULT 1,
  image_url VARCHAR(255) NOT NULL DEFAULT '/member/member_images/user-img.svg',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  email_validated BOOLEAN NOT NULL DEFAULT FALSE,
  google_uid VARCHAR(255),
  image_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  reset_token VARCHAR(255),
  reset_token_expiry TIMESTAMP,
  reset_token_secret VARCHAR(255),
  google_name VARCHAR(255),
  city VARCHAR(50),
  address TEXT,
  zip VARCHAR(5),
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. article(暫不加 member/dogs FK,稍後 ALTER)
-- ============================================================

CREATE TABLE article (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL,
  member_username VARCHAR(255) NOT NULL,
  author VARCHAR(255),
  title VARCHAR(255) NOT NULL,
  dogs_id INTEGER,
  dogs_breed VARCHAR(255),
  dogs_images VARCHAR(255),
  content1 TEXT NOT NULL,
  content2 TEXT,
  created_date TIMESTAMP NOT NULL,
  created_id INTEGER NOT NULL,
  event_id INTEGER,
  valid INTEGER NOT NULL DEFAULT 1,
  article_images VARCHAR(255),
  category_name VARCHAR(255) NOT NULL
);

-- ============================================================
-- 4. product 及其子表
-- ============================================================

CREATE TABLE product (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10,2) NOT NULL,
  sale_price DECIMAL(10,2),
  sku VARCHAR(100) UNIQUE,
  stock_quantity INTEGER DEFAULT 0,
  brand_id INTEGER REFERENCES brand(id),
  category_id INTEGER REFERENCES category(id),
  is_active BOOLEAN DEFAULT TRUE,
  weight DECIMAL(8,2),
  dimensions VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL
);

CREATE TABLE product_images (
  id SERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL REFERENCES product(id) ON DELETE CASCADE,
  image_url VARCHAR(255) NOT NULL,
  alt_text VARCHAR(255),
  is_primary BOOLEAN DEFAULT FALSE,
  sort_order INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_specifications (
  id SERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL REFERENCES product(id),
  spec_name VARCHAR(100) NOT NULL,
  spec_value VARCHAR(255) NOT NULL,
  sort_order INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_tag_map (
  id SERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL REFERENCES product(id),
  tag_id INTEGER NOT NULL REFERENCES product_tags(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(product_id, tag_id)
);

-- ============================================================
-- 5. coupons(必須在 orders 之前)
-- ============================================================

CREATE TABLE coupons (
  id SERIAL PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  discount_type VARCHAR(20) CHECK (discount_type IN ('percentage', 'fixed')),
  discount_value DECIMAL(10,2) NOT NULL,
  minimum_amount DECIMAL(10,2) DEFAULT 0,
  maximum_uses INTEGER,
  used_count INTEGER DEFAULT 0,
  starts_at TIMESTAMP,
  expires_at TIMESTAMP,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. orders 及其子表
-- ============================================================

CREATE TABLE orders (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  order_number VARCHAR(50) NOT NULL UNIQUE,
  status_id INTEGER NOT NULL REFERENCES order_status(id),
  payment_id INTEGER REFERENCES order_payment(id),
  total_amount DECIMAL(10,2) NOT NULL,
  shipping_amount DECIMAL(10,2) DEFAULT 0,
  tax_amount DECIMAL(10,2) DEFAULT 0,
  discount_amount DECIMAL(10,2) DEFAULT 0,
  shipping_address TEXT,
  billing_address TEXT,
  recipient_name VARCHAR(100),
  recipient_phone VARCHAR(20),
  recipient_email VARCHAR(255),
  delivery_method VARCHAR(20),
  city VARCHAR(50),
  town VARCHAR(50),
  address TEXT,
  store_name VARCHAR(100),
  store_address TEXT,
  coupon_id INTEGER REFERENCES coupons(id),
  discount_type VARCHAR(20),
  discount_value DECIMAL(10,2),
  notes TEXT,
  -- 冪等鍵：用戶端帶來的 UUID（無冪等需求時可為 NULL）。
  -- 配 partial unique index 兜底 Redis 失效時的重複下單。
  idempotency_key VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL
);

CREATE TABLE order_items (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id INTEGER NOT NULL REFERENCES product(id),
  quantity INTEGER NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  total_price DECIMAL(10,2) NOT NULL,
  product_name VARCHAR(255),
  color VARCHAR(50),
  size VARCHAR(50),
  packing VARCHAR(50),
  items_group VARCHAR(50),
  image VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL
);

-- ============================================================
-- 7. 其餘業務表
-- ============================================================

CREATE TABLE cart_items (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  product_id INTEGER NOT NULL REFERENCES product(id),
  quantity INTEGER NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(member_id, product_id)
);

CREATE TABLE email_verification (
  id SERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  otp_token VARCHAR(10) NOT NULL,
  secret VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
  id SERIAL PRIMARY KEY,
  token VARCHAR(255) NOT NULL UNIQUE,
  user_id INTEGER NOT NULL REFERENCES member(id),
  expiry_date TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  revoked BOOLEAN DEFAULT FALSE
);

CREATE TABLE member_coupons (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  coupon_id INTEGER NOT NULL REFERENCES coupons(id),
  used_at TIMESTAMP,
  order_id INTEGER REFERENCES orders(id),
  acquired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  source VARCHAR(255)
);

CREATE TABLE dogs (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  name VARCHAR(255) NOT NULL,
  breed VARCHAR(255),
  age INTEGER,
  weight DECIMAL(5,2),
  gender VARCHAR(10) CHECK (gender IN ('male', 'female')),
  description TEXT,
  image_url VARCHAR(255),
  medical_notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_favorites (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  product_id INTEGER NOT NULL REFERENCES product(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(member_id, product_id)
);

CREATE TABLE article_favorites (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  article_id INTEGER NOT NULL REFERENCES article(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(member_id, article_id)
);

CREATE TABLE product_reviews (
  id SERIAL PRIMARY KEY,
  product_id INTEGER NOT NULL REFERENCES product(id),
  member_id INTEGER NOT NULL REFERENCES member(id),
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  title VARCHAR(255),
  content TEXT,
  is_verified_purchase BOOLEAN DEFAULT FALSE,
  helpful_votes INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE points (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  points INTEGER NOT NULL,
  description VARCHAR(255),
  reference_id INTEGER,
  reference_type VARCHAR(50),
  expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  type VARCHAR(50),
  is_read BOOLEAN DEFAULT FALSE,
  action_url VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sitters (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  name VARCHAR(255) NOT NULL,
  area VARCHAR(100) NOT NULL,
  service_time TEXT,
  experience TEXT,
  introduction TEXT,
  price DECIMAL(10,2) NOT NULL,
  avatar_url VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sitter_gallery (
  id SERIAL PRIMARY KEY,
  sitter_id INTEGER NOT NULL REFERENCES sitters(id) ON DELETE CASCADE,
  image_url VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sitter_reviews (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  sitter_id INTEGER NOT NULL REFERENCES sitters(id) ON DELETE CASCADE,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(member_id, sitter_id)
);

CREATE TABLE sitter_bookings (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  sitter_id INTEGER NOT NULL REFERENCES sitters(id),
  pet_id INTEGER NOT NULL REFERENCES dogs(id),
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recipients (
  id SERIAL PRIMARY KEY,
  member_id INTEGER NOT NULL REFERENCES member(id),
  name VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  city VARCHAR(50),
  town VARCHAR(50),
  address TEXT,
  is_default BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE article_comments (
  id SERIAL PRIMARY KEY,
  article_id INTEGER NOT NULL REFERENCES article(id),
  member_id INTEGER NOT NULL REFERENCES member(id),
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 8. 延遲外鍵約束
-- ============================================================

ALTER TABLE article ADD CONSTRAINT fk_article_member FOREIGN KEY (member_id) REFERENCES member(id);
ALTER TABLE article ADD CONSTRAINT fk_article_dog FOREIGN KEY (dogs_id) REFERENCES dogs(id);
ALTER TABLE member ADD CONSTRAINT fk_member_vip FOREIGN KEY (vip_levels_id) REFERENCES vip_levels(id);

-- ============================================================
-- 9. 索引
-- ============================================================

CREATE INDEX idx_member_email ON member(email);
CREATE INDEX idx_member_username ON member(username);
CREATE INDEX idx_product_category ON product(category_id);
CREATE INDEX idx_product_brand ON product(brand_id);
CREATE INDEX idx_product_active ON product(is_active);
CREATE INDEX idx_product_sku ON product(sku);
CREATE INDEX idx_cart_member ON cart_items(member_id);
CREATE INDEX idx_order_member ON orders(member_id);
CREATE INDEX idx_order_status ON orders(status_id);
-- 冪等性兜底：同一 member + 同一 idempotency_key 永遠只能存在一筆訂單。
-- 用 partial index 排除無冪等需求的舊資料 / null key，避免誤撞 unique。
CREATE UNIQUE INDEX idx_orders_member_idempotency_key
    ON orders(member_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_article_valid ON article(valid);
CREATE INDEX idx_sitters_member ON sitters(member_id);
CREATE INDEX idx_sitter_reviews_sitter ON sitter_reviews(sitter_id);
CREATE INDEX idx_sitter_bookings_member ON sitter_bookings(member_id);
CREATE INDEX idx_sitter_bookings_sitter ON sitter_bookings(sitter_id);
CREATE INDEX idx_recipients_member ON recipients(member_id);
CREATE INDEX idx_article_comments_article ON article_comments(article_id);
CREATE INDEX idx_article_comments_member ON article_comments(member_id);

-- ============================================================
-- 10. member 額外索引(原 V1 migration 的內容)
-- ============================================================
-- google_uid partial UNIQUE:OAuth2 race condition 防護的 DB 端保險;
-- 用 partial index (WHERE google_uid IS NOT NULL) 是因為大量未綁 Google
-- 的帳號 google_uid 為 NULL,partial index 更明確且省空間。
CREATE UNIQUE INDEX uniq_member_google_uid
    ON member (google_uid)
    WHERE google_uid IS NOT NULL;

-- reset_token 一般索引:findByResetToken 用,大多時候為 NULL,partial 省空間。
CREATE INDEX idx_member_reset_token
    ON member (reset_token)
    WHERE reset_token IS NOT NULL;

COMMIT;
