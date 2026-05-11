CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_account_username ON user_account(username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_account_email ON user_account(email);

-- 租户表
CREATE TABLE IF NOT EXISTS tenant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  industry VARCHAR(120),
  plan_name VARCHAR(60),
  owner_user_id BIGINT NOT NULL,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_slug ON tenant(slug);
CREATE INDEX IF NOT EXISTS idx_tenant_owner_user_id ON tenant(owner_user_id);

-- 租户成员表
CREATE TABLE IF NOT EXISTS tenant_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  role VARCHAR(40) NOT NULL,
  status TINYINT DEFAULT 1,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_member ON tenant_member(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_tenant_member_tenant_id ON tenant_member(tenant_id);

CREATE TABLE IF NOT EXISTS user_session (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  access_token VARCHAR(255) NOT NULL,
  status TINYINT DEFAULT 1,
  expires_at TIMESTAMP NOT NULL,
  last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_session_access_token ON user_session(access_token);
CREATE INDEX IF NOT EXISTS idx_user_session_user_tenant ON user_session(user_id, tenant_id);

CREATE TABLE IF NOT EXISTS tenant_invite (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  inviter_user_id BIGINT NOT NULL,
  invitee_email VARCHAR(160) NOT NULL,
  invitee_display_name VARCHAR(120),
  role VARCHAR(40) NOT NULL,
  invite_token VARCHAR(120) NOT NULL,
  status TINYINT DEFAULT 1,
  expires_at TIMESTAMP NOT NULL,
  accepted_by_user_id BIGINT,
  accepted_at TIMESTAMP NULL,
  revoked_by_user_id BIGINT,
  revoked_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_invite_token ON tenant_invite(invite_token);
CREATE INDEX IF NOT EXISTS idx_tenant_invite_tenant_id ON tenant_invite(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_invite_email ON tenant_invite(invitee_email);

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  cover VARCHAR(255),
  user_id BIGINT NOT NULL,
  status TINYINT DEFAULT 1,
  document_count INT DEFAULT 0,
  view_count INT DEFAULT 0,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,
  deleted_by BIGINT,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_tenant_slug ON knowledge_base(tenant_id, slug);
CREATE INDEX IF NOT EXISTS idx_kb_tenant_id ON knowledge_base(tenant_id);
CREATE INDEX IF NOT EXISTS idx_kb_user_id ON knowledge_base(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_status ON knowledge_base(status);
CREATE INDEX IF NOT EXISTS idx_kb_created_at ON knowledge_base(created_at);

-- 知识库成员权限表
CREATE TABLE IF NOT EXISTS knowledge_base_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_base_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(30) NOT NULL,
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_member_user ON knowledge_base_member(knowledge_base_id, user_id);
CREATE INDEX IF NOT EXISTS idx_kb_member_kb_id ON knowledge_base_member(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_kb_member_user_id ON knowledge_base_member(user_id);

-- 文档表
CREATE TABLE IF NOT EXISTS document (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  slug VARCHAR(160) NOT NULL,
  doc_type VARCHAR(30) DEFAULT 'DOC',
  format VARCHAR(30) DEFAULT 'MARKDOWN',
  content CLOB,
  content_text CLOB,
  summary VARCHAR(500),
  knowledge_base_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT 0,
  path VARCHAR(500) NOT NULL,
  depth INT DEFAULT 0,
  version_no INT DEFAULT 1,
  status TINYINT DEFAULT 1,
  view_count INT DEFAULT 0,
  sort_order INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL,
  deleted_by BIGINT,
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_doc_kb_path ON document(knowledge_base_id, path);
CREATE INDEX IF NOT EXISTS idx_doc_kb_id ON document(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_doc_tenant_id ON document(tenant_id);
CREATE INDEX IF NOT EXISTS idx_doc_user_id ON document(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_parent_id ON document(parent_id);
CREATE INDEX IF NOT EXISTS idx_doc_created_at ON document(created_at);

-- 文档版本表
CREATE TABLE IF NOT EXISTS document_version (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  version INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  format VARCHAR(30) DEFAULT 'MARKDOWN',
  content CLOB,
  content_text CLOB,
  user_id BIGINT NOT NULL,
  remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_version_doc_id ON document_version(document_id);
CREATE INDEX IF NOT EXISTS idx_version_version ON document_version(version);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT,
  knowledge_base_name VARCHAR(120),
  actor_type VARCHAR(30) NOT NULL,
  actor_user_id BIGINT,
  actor_display_name VARCHAR(120),
  actor_role VARCHAR(40),
  object_type VARCHAR(40) NOT NULL,
  object_id BIGINT,
  object_title VARCHAR(200),
  action_type VARCHAR(60) NOT NULL,
  result_type VARCHAR(30) NOT NULL,
  detail VARCHAR(500),
  source_type VARCHAR(60),
  request_method VARCHAR(20),
  request_path VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_created_at ON audit_log(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_kb_created_at ON audit_log(knowledge_base_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_object_created_at ON audit_log(object_type, object_id, created_at);

CREATE TABLE IF NOT EXISTS audit_log_archive (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT,
  knowledge_base_name VARCHAR(120),
  actor_type VARCHAR(30) NOT NULL,
  actor_user_id BIGINT,
  actor_display_name VARCHAR(120),
  actor_role VARCHAR(40),
  object_type VARCHAR(40) NOT NULL,
  object_id BIGINT,
  object_title VARCHAR(200),
  action_type VARCHAR(60) NOT NULL,
  result_type VARCHAR(30) NOT NULL,
  detail VARCHAR(500),
  source_type VARCHAR(60),
  request_method VARCHAR(20),
  request_path VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_audit_log_archive_tenant_created_at ON audit_log_archive(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_archive_kb_created_at ON audit_log_archive(knowledge_base_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_archive_object_created_at ON audit_log_archive(object_type, object_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_archive_archived_at ON audit_log_archive(archived_at);

CREATE TABLE IF NOT EXISTS document_share_link (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  share_token VARCHAR(160) NOT NULL,
  secret_hash VARCHAR(255) NOT NULL,
  status TINYINT DEFAULT 1,
  expires_at TIMESTAMP NOT NULL,
  access_code_hash VARCHAR(255),
  created_by_user_id BIGINT NOT NULL,
  revoked_by_user_id BIGINT,
  revoked_at TIMESTAMP NULL,
  last_accessed_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
  FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_document_share_link_token ON document_share_link(share_token);
CREATE INDEX IF NOT EXISTS idx_document_share_link_document ON document_share_link(document_id, created_at);
CREATE INDEX IF NOT EXISTS idx_document_share_link_tenant ON document_share_link(tenant_id, created_at);

CREATE TABLE IF NOT EXISTS service_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500),
  status TINYINT DEFAULT 1,
  created_by_user_id BIGINT NOT NULL,
  revoked_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_service_account_tenant ON service_account(tenant_id, created_at);

CREATE TABLE IF NOT EXISTS api_key (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  service_account_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  key_prefix VARCHAR(40) NOT NULL,
  secret_hash VARCHAR(255) NOT NULL,
  status TINYINT DEFAULT 1,
  expires_at TIMESTAMP NOT NULL,
  last_used_at TIMESTAMP NULL,
  created_by_user_id BIGINT NOT NULL,
  revoked_by_user_id BIGINT,
  revoked_at TIMESTAMP NULL,
  rotated_from_key_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (service_account_id) REFERENCES service_account(id) ON DELETE CASCADE,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_api_key_service_account ON api_key(service_account_id, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_api_key_prefix ON api_key(key_prefix);

CREATE TABLE IF NOT EXISTS api_key_scope (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  api_key_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  access_mode VARCHAR(20) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (api_key_id) REFERENCES api_key(id) ON DELETE CASCADE,
  FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_api_key_scope_kb ON api_key_scope(api_key_id, knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_api_key_scope_tenant ON api_key_scope(tenant_id, knowledge_base_id);
