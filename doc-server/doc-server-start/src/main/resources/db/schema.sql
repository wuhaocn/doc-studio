-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  cover VARCHAR(255),
  user_id BIGINT NOT NULL,
  status TINYINT DEFAULT 1,
  is_public TINYINT DEFAULT 0,
  document_count INT DEFAULT 0,
  view_count INT DEFAULT 0,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kb_user_id ON knowledge_base(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_status ON knowledge_base(status);
CREATE INDEX IF NOT EXISTS idx_kb_created_at ON knowledge_base(created_at);

-- 文档表
CREATE TABLE IF NOT EXISTS document (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  content CLOB,
  content_text CLOB,
  knowledge_base_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0,
  status TINYINT DEFAULT 1,
  is_public TINYINT DEFAULT 0,
  view_count INT DEFAULT 0,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_doc_kb_id ON document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_doc_user_id ON document(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_parent_id ON document(parent_id);
CREATE INDEX IF NOT EXISTS idx_doc_created_at ON document(created_at);

