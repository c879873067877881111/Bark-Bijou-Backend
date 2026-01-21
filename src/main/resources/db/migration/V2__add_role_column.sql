-- 新增 role 欄位到 member 表
-- 用於支援基於角色的權限控制 (USER, ADMIN, SITTER)

ALTER TABLE member
ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- 添加約束檢查
ALTER TABLE member
ADD CONSTRAINT chk_member_role CHECK (role IN ('USER', 'ADMIN', 'SITTER'));

-- 將現有的 admin 用戶設為 ADMIN 角色
UPDATE member SET role = 'ADMIN' WHERE username = 'admin';
