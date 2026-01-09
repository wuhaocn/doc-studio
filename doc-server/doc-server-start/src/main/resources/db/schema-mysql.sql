-- MySQL 数据库表结构（生产环境使用）
-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
  `description` VARCHAR(500) COMMENT '知识库描述',
  `cover` VARCHAR(255) COMMENT '封面图片',
  `user_id` BIGINT NOT NULL COMMENT '创建者ID',
  `status` TINYINT DEFAULT 1 COMMENT '1:正常 0:删除 2:归档',
  `is_public` TINYINT DEFAULT 0 COMMENT '是否公开',
  `document_count` INT DEFAULT 0 COMMENT '文档数量',
  `view_count` INT DEFAULT 0 COMMENT '查看次数',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 文档表
CREATE TABLE IF NOT EXISTS `document` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `content` LONGTEXT COMMENT '文档内容HTML',
  `content_text` TEXT COMMENT '纯文本内容，用于搜索',
  `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
  `user_id` BIGINT NOT NULL COMMENT '创建者ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父文档ID，0表示根目录（同一知识库内）',
  `status` TINYINT DEFAULT 1 COMMENT '1:正常 0:删除',
  `is_public` TINYINT DEFAULT 0 COMMENT '是否公开',
  `view_count` INT DEFAULT 0 COMMENT '查看次数',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_knowledge_base_id` (`knowledge_base_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_parent_id` (`parent_id`),
  INDEX `idx_created_at` (`created_at`),
  FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

