-- MySQL 数据库表结构（当前多租户在线文档主链路基线）

CREATE TABLE IF NOT EXISTS `user_account` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(120) NOT NULL,
  `email` VARCHAR(160) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(120) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE UNIQUE INDEX `uk_user_account_username` ON `user_account` (`username`);
CREATE UNIQUE INDEX `uk_user_account_email` ON `user_account` (`email`);

CREATE TABLE IF NOT EXISTS `tenant` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(120) NOT NULL,
  `slug` VARCHAR(120) NOT NULL,
  `industry` VARCHAR(120),
  `plan_name` VARCHAR(60),
  `owner_user_id` BIGINT NOT NULL,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

CREATE UNIQUE INDEX `uk_tenant_slug` ON `tenant` (`slug`);
CREATE INDEX `idx_tenant_owner_user_id` ON `tenant` (`owner_user_id`);

CREATE TABLE IF NOT EXISTS `tenant_member` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `display_name` VARCHAR(120) NOT NULL,
  `role` VARCHAR(40) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `last_active_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_tenant_member_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户成员表';

CREATE UNIQUE INDEX `uk_tenant_member` ON `tenant_member` (`tenant_id`, `user_id`);
CREATE INDEX `idx_tenant_member_tenant_id` ON `tenant_member` (`tenant_id`);

CREATE TABLE IF NOT EXISTS `user_session` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `access_token` VARCHAR(255) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `expires_at` DATETIME NOT NULL,
  `last_active_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

CREATE UNIQUE INDEX `uk_user_session_access_token` ON `user_session` (`access_token`);
CREATE INDEX `idx_user_session_user_tenant` ON `user_session` (`user_id`, `tenant_id`);

CREATE TABLE IF NOT EXISTS `tenant_invite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `inviter_user_id` BIGINT NOT NULL,
  `invitee_email` VARCHAR(160) NOT NULL,
  `invitee_display_name` VARCHAR(120),
  `role` VARCHAR(40) NOT NULL,
  `invite_token` VARCHAR(120) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `expires_at` DATETIME NOT NULL,
  `accepted_by_user_id` BIGINT DEFAULT NULL,
  `accepted_at` DATETIME DEFAULT NULL,
  `revoked_by_user_id` BIGINT DEFAULT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_tenant_invite_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户邀请表';

CREATE UNIQUE INDEX `uk_tenant_invite_token` ON `tenant_invite` (`invite_token`);
CREATE INDEX `idx_tenant_invite_tenant_id` ON `tenant_invite` (`tenant_id`);
CREATE INDEX `idx_tenant_invite_email` ON `tenant_invite` (`invitee_email`);

CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `slug` VARCHAR(120) NOT NULL,
  `description` VARCHAR(500),
  `cover` VARCHAR(255),
  `user_id` BIGINT NOT NULL,
  `status` TINYINT DEFAULT 1,
  `document_count` INT DEFAULT 0,
  `view_count` INT DEFAULT 0,
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  `deleted_by` BIGINT DEFAULT NULL,
  CONSTRAINT `fk_knowledge_base_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

CREATE UNIQUE INDEX `uk_kb_tenant_slug` ON `knowledge_base` (`tenant_id`, `slug`);
CREATE INDEX `idx_kb_tenant_id` ON `knowledge_base` (`tenant_id`);
CREATE INDEX `idx_kb_user_id` ON `knowledge_base` (`user_id`);
CREATE INDEX `idx_kb_status` ON `knowledge_base` (`status`);
CREATE INDEX `idx_kb_created_at` ON `knowledge_base` (`created_at`);

CREATE TABLE IF NOT EXISTS `knowledge_base_member` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `knowledge_base_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `role` VARCHAR(30) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_kb_member_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_kb_member_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库成员权限表';

CREATE UNIQUE INDEX `uk_kb_member_user` ON `knowledge_base_member` (`knowledge_base_id`, `user_id`);
CREATE INDEX `idx_kb_member_kb_id` ON `knowledge_base_member` (`knowledge_base_id`);
CREATE INDEX `idx_kb_member_user_id` ON `knowledge_base_member` (`user_id`);

CREATE TABLE IF NOT EXISTS `document` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `slug` VARCHAR(160) NOT NULL,
  `doc_type` VARCHAR(30) DEFAULT 'DOC',
  `format` VARCHAR(30) DEFAULT 'MARKDOWN',
  `content` LONGTEXT,
  `content_text` LONGTEXT,
  `summary` VARCHAR(500),
  `knowledge_base_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `parent_id` BIGINT DEFAULT 0,
  `path` VARCHAR(500) NOT NULL,
  `depth` INT DEFAULT 0,
  `version_no` INT DEFAULT 1,
  `status` TINYINT DEFAULT 1,
  `view_count` INT DEFAULT 0,
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  `deleted_by` BIGINT DEFAULT NULL,
  CONSTRAINT `fk_document_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

CREATE UNIQUE INDEX `uk_doc_kb_path` ON `document` (`knowledge_base_id`, `path`);
CREATE INDEX `idx_doc_kb_id` ON `document` (`knowledge_base_id`);
CREATE INDEX `idx_doc_tenant_id` ON `document` (`tenant_id`);
CREATE INDEX `idx_doc_user_id` ON `document` (`user_id`);
CREATE INDEX `idx_doc_parent_id` ON `document` (`parent_id`);
CREATE INDEX `idx_doc_created_at` ON `document` (`created_at`);

CREATE TABLE IF NOT EXISTS `document_version` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `document_id` BIGINT NOT NULL,
  `version` INT NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `format` VARCHAR(30) DEFAULT 'MARKDOWN',
  `content` LONGTEXT,
  `content_text` LONGTEXT,
  `user_id` BIGINT NOT NULL,
  `remark` VARCHAR(255),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_document_version_doc` FOREIGN KEY (`document_id`) REFERENCES `document`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档版本表';

CREATE INDEX `idx_version_doc_id` ON `document_version` (`document_id`);
CREATE INDEX `idx_version_version` ON `document_version` (`version`);

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `knowledge_base_id` BIGINT DEFAULT NULL,
  `knowledge_base_name` VARCHAR(120) DEFAULT NULL,
  `actor_type` VARCHAR(30) NOT NULL,
  `actor_user_id` BIGINT DEFAULT NULL,
  `actor_display_name` VARCHAR(120) DEFAULT NULL,
  `actor_role` VARCHAR(40) DEFAULT NULL,
  `object_type` VARCHAR(40) NOT NULL,
  `object_id` BIGINT DEFAULT NULL,
  `object_title` VARCHAR(200) DEFAULT NULL,
  `action_type` VARCHAR(60) NOT NULL,
  `result_type` VARCHAR(30) NOT NULL,
  `detail` VARCHAR(500) DEFAULT NULL,
  `source_type` VARCHAR(60) DEFAULT NULL,
  `request_method` VARCHAR(20) DEFAULT NULL,
  `request_path` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_audit_log_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

CREATE INDEX `idx_audit_log_tenant_created_at` ON `audit_log` (`tenant_id`, `created_at`);
CREATE INDEX `idx_audit_log_kb_created_at` ON `audit_log` (`knowledge_base_id`, `created_at`);
CREATE INDEX `idx_audit_log_object_created_at` ON `audit_log` (`object_type`, `object_id`, `created_at`);

CREATE TABLE IF NOT EXISTS `audit_log_archive` (
  `id` BIGINT PRIMARY KEY,
  `tenant_id` BIGINT NOT NULL,
  `knowledge_base_id` BIGINT DEFAULT NULL,
  `knowledge_base_name` VARCHAR(120) DEFAULT NULL,
  `actor_type` VARCHAR(30) NOT NULL,
  `actor_user_id` BIGINT DEFAULT NULL,
  `actor_display_name` VARCHAR(120) DEFAULT NULL,
  `actor_role` VARCHAR(40) DEFAULT NULL,
  `object_type` VARCHAR(40) NOT NULL,
  `object_id` BIGINT DEFAULT NULL,
  `object_title` VARCHAR(200) DEFAULT NULL,
  `action_type` VARCHAR(60) NOT NULL,
  `result_type` VARCHAR(30) NOT NULL,
  `detail` VARCHAR(500) DEFAULT NULL,
  `source_type` VARCHAR(60) DEFAULT NULL,
  `request_method` VARCHAR(20) DEFAULT NULL,
  `request_path` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `archived_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_audit_log_archive_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计归档表';

CREATE INDEX `idx_audit_log_archive_tenant_created_at` ON `audit_log_archive` (`tenant_id`, `created_at`);
CREATE INDEX `idx_audit_log_archive_kb_created_at` ON `audit_log_archive` (`knowledge_base_id`, `created_at`);
CREATE INDEX `idx_audit_log_archive_object_created_at` ON `audit_log_archive` (`object_type`, `object_id`, `created_at`);
CREATE INDEX `idx_audit_log_archive_archived_at` ON `audit_log_archive` (`archived_at`);

CREATE TABLE IF NOT EXISTS `document_share_link` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `knowledge_base_id` BIGINT NOT NULL,
  `document_id` BIGINT NOT NULL,
  `share_token` VARCHAR(160) NOT NULL,
  `secret_hash` VARCHAR(255) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `expires_at` DATETIME NOT NULL,
  `access_code_hash` VARCHAR(255) DEFAULT NULL,
  `created_by_user_id` BIGINT NOT NULL,
  `revoked_by_user_id` BIGINT DEFAULT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `last_accessed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_document_share_link_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_share_link_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_share_link_document` FOREIGN KEY (`document_id`) REFERENCES `document`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档受控分享表';

CREATE UNIQUE INDEX `uk_document_share_link_token` ON `document_share_link` (`share_token`);
CREATE INDEX `idx_document_share_link_document` ON `document_share_link` (`document_id`, `created_at`);
CREATE INDEX `idx_document_share_link_tenant` ON `document_share_link` (`tenant_id`, `created_at`);

CREATE TABLE IF NOT EXISTS `service_account` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT DEFAULT 1,
  `created_by_user_id` BIGINT NOT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_service_account_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器主体 service account';

CREATE INDEX `idx_service_account_tenant` ON `service_account` (`tenant_id`, `created_at`);

CREATE TABLE IF NOT EXISTS `api_key` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `service_account_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `name` VARCHAR(120) NOT NULL,
  `key_prefix` VARCHAR(40) NOT NULL,
  `secret_hash` VARCHAR(255) NOT NULL,
  `status` TINYINT DEFAULT 1,
  `expires_at` DATETIME NOT NULL,
  `last_used_at` DATETIME DEFAULT NULL,
  `created_by_user_id` BIGINT NOT NULL,
  `revoked_by_user_id` BIGINT DEFAULT NULL,
  `revoked_at` DATETIME DEFAULT NULL,
  `rotated_from_key_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_api_key_service_account` FOREIGN KEY (`service_account_id`) REFERENCES `service_account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_api_key_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API key';

CREATE INDEX `idx_api_key_service_account` ON `api_key` (`service_account_id`, `created_at`);
CREATE UNIQUE INDEX `uk_api_key_prefix` ON `api_key` (`key_prefix`);

CREATE TABLE IF NOT EXISTS `api_key_scope` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `api_key_id` BIGINT NOT NULL,
  `tenant_id` BIGINT NOT NULL,
  `knowledge_base_id` BIGINT NOT NULL,
  `access_mode` VARCHAR(20) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_api_key_scope_key` FOREIGN KEY (`api_key_id`) REFERENCES `api_key`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_api_key_scope_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenant`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_api_key_scope_kb` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API key 作用域';

CREATE UNIQUE INDEX `uk_api_key_scope_kb` ON `api_key_scope` (`api_key_id`, `knowledge_base_id`);
CREATE INDEX `idx_api_key_scope_tenant` ON `api_key_scope` (`tenant_id`, `knowledge_base_id`);
