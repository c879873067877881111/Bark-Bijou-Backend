-- V4: Add recipients and article_comments tables

CREATE TABLE IF NOT EXISTS recipients (
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

CREATE TABLE IF NOT EXISTS article_comments (
  id SERIAL PRIMARY KEY,
  article_id INTEGER NOT NULL REFERENCES article(id),
  member_id INTEGER NOT NULL REFERENCES member(id),
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recipients_member ON recipients(member_id);
CREATE INDEX IF NOT EXISTS idx_article_comments_article ON article_comments(article_id);
CREATE INDEX IF NOT EXISTS idx_article_comments_member ON article_comments(member_id);
