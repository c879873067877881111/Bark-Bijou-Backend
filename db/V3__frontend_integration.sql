-- V3: Add columns for frontend integration (recipient info, delivery, order item details)

-- orders: 收件人 + 配送 + 優惠券
ALTER TABLE orders ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS recipient_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS city VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS town VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS store_name VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS store_address TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_id INTEGER REFERENCES coupons(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_type VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_value DECIMAL(10,2);

-- order_items: 商品快照（下單時凍結名稱/規格/圖片）
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS color VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS size VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS packing VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS items_group VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS image VARCHAR(500);
