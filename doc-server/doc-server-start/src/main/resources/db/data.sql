-- 初始化测试数据
-- 注意：user_id 暂时使用固定值，后续鉴权功能实现后需要调整

-- 插入测试知识库
INSERT INTO knowledge_base (name, description, user_id, status, is_public, document_count, view_count, sort_order) 
VALUES 
('产品文档', '产品相关的所有文档和资料', 1, 1, 0, 3, 120, 0),
('技术文档', '技术开发相关的文档和规范', 1, 1, 0, 2, 256, 1),
('团队协作', '团队协作和会议记录', 1, 1, 1, 1, 89, 2);

-- 插入测试文档 - 产品文档知识库
INSERT INTO document (title, content, content_text, knowledge_base_id, user_id, parent_id, status, is_public, view_count, sort_order)
VALUES
('产品需求文档', '<h1>产品需求文档</h1><p>这是产品需求文档的内容，包含了详细的产品功能需求说明。</p><h2>核心功能</h2><ul><li>用户管理</li><li>文档编辑</li><li>协作功能</li></ul>', '产品需求文档 这是产品需求文档的内容，包含了详细的产品功能需求说明。核心功能 用户管理 文档编辑 协作功能', 1, 1, 0, 1, 0, 45, 0),
('用户调研报告', '<h1>用户调研报告</h1><p>用户调研的详细内容，包括用户画像、需求分析等。</p><h2>调研方法</h2><p>采用问卷调查和深度访谈相结合的方式。</p>', '用户调研报告 用户调研的详细内容，包括用户画像、需求分析等。调研方法 采用问卷调查和深度访谈相结合的方式。', 1, 1, 0, 1, 0, 32, 1),
('产品规划', '<h1>产品规划</h1><p>产品规划相关内容，包括产品路线图和里程碑计划。</p>', '产品规划 产品规划相关内容，包括产品路线图和里程碑计划。', 1, 1, 0, 1, 0, 28, 2);

-- 插入测试文档 - 技术文档知识库
INSERT INTO document (title, content, content_text, knowledge_base_id, user_id, parent_id, status, is_public, view_count, sort_order)
VALUES
('API接口文档', '<h1>API接口文档</h1><p>API接口的详细说明，包括请求参数、响应格式等。</p><h2>接口列表</h2><ul><li>GET /api/v1/knowledge-bases</li><li>POST /api/v1/documents</li></ul>', 'API接口文档 API接口的详细说明，包括请求参数、响应格式等。接口列表 GET /api/v1/knowledge-bases POST /api/v1/documents', 2, 1, 0, 1, 0, 156, 0),
('开发规范', '<h1>开发规范</h1><p>代码规范和开发流程，包括编码标准、Git工作流等。</p>', '开发规范 代码规范和开发流程，包括编码标准、Git工作流等。', 2, 1, 0, 1, 0, 98, 1);

-- 插入测试文档 - 团队协作知识库
INSERT INTO document (title, content, content_text, knowledge_base_id, user_id, parent_id, status, is_public, view_count, sort_order)
VALUES
('周会记录', '<h1>周会记录</h1><p>本周会议内容，包括讨论议题和决策事项。</p><h2>议题</h2><ul><li>项目进度回顾</li><li>下周计划</li></ul>', '周会记录 本周会议内容，包括讨论议题和决策事项。议题 项目进度回顾 下周计划', 3, 1, 0, 1, 1, 45, 0);

-- 更新知识库的文档数量
UPDATE knowledge_base SET document_count = 3 WHERE id = 1;
UPDATE knowledge_base SET document_count = 2 WHERE id = 2;
UPDATE knowledge_base SET document_count = 1 WHERE id = 3;

