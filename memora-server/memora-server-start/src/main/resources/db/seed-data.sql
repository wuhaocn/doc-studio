INSERT INTO user_account (id, username, email, password_hash, display_name, status)
VALUES
(1, 'admin', 'admin@memora.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '王晨', 1),
(2, 'editor', 'editor@memora.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '刘倩', 1),
(3, 'reviewer', 'reviewer@memora.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '陈立', 1),
(4, 'viewer', 'viewer@memora.local', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '赵敏', 1);

INSERT INTO tenant (id, name, slug, industry, plan_name, owner_user_id, status)
VALUES
(1, '华东制造知识中台', 'east-manufacturing-docs', '工业制造', 'ENTERPRISE', 1, 1),
(2, '华南售后协同中心', 'south-service-ops', '售后服务', 'TEAM', 1, 1);

INSERT INTO tenant_member (tenant_id, user_id, display_name, role, status)
VALUES
(1, 1, '王晨', 'OWNER', 1),
(1, 2, '刘倩', 'EDITOR', 1),
(1, 3, '陈立', 'REVIEWER', 1),
(1, 4, '赵敏', 'VIEWER', 1);

INSERT INTO tenant_member (tenant_id, user_id, display_name, role, status, joined_at, last_active_at)
VALUES
(2, 1, '王晨', 'OWNER', 1, '2024-01-01 09:00:00', '2024-01-01 09:00:00'),
(2, 2, '刘倩', 'ADMIN', 1, '2024-01-02 09:00:00', '2024-01-02 09:00:00');

INSERT INTO knowledge_base (
  id, tenant_id, name, slug, description, user_id, status, document_count, view_count, sort_order
)
VALUES
(1, 1, '设备交付知识库', 'delivery-playbook', '沉淀交付 SOP、现场排障和验收模板。', 1, 1, 3, 682, 0),
(2, 1, '售后维保知识库', 'maintenance-manuals', '面向售后团队的维保手册、巡检报告和标准工时说明。', 2, 1, 2, 415, 1),
(3, 1, '合规与培训中心', 'compliance-academy', '汇总质量合规、EHS 培训资料和现场作业指引。', 3, 1, 2, 251, 2),
(4, 2, '区域服务值班手册', 'regional-service-desk', '面向华南区域售后协同的值班、交接与升级手册。', 1, 1, 1, 128, 0);

INSERT INTO document (
  id, tenant_id, title, slug, doc_type, format, content, content_text, summary,
  knowledge_base_id, user_id, parent_id, path, depth, version_no, status, view_count, sort_order
)
VALUES
(1, 1, '交付总览', 'delivery-overview', 'FOLDER', 'MARKDOWN', '# 交付总览', '交付总览', '交付流程的总入口目录。', 1, 1, 0, '/delivery-overview', 0, 1, 1, 120, 0),
(2, 1, '项目启动清单', 'kickoff-checklist', 'DOC', 'MARKDOWN', '# 项目启动清单\n- 确认客户信息\n- 确认硬件版本', '项目启动清单 确认客户信息 确认硬件版本', '适用于制造项目实施启动阶段的标准清单。', 1, 1, 1, '/delivery-overview/kickoff-checklist', 1, 3, 1, 214, 0),
(3, 1, '现场问题升级流程', 'incident-escalation', 'DOC', 'MARKDOWN', '# 现场问题升级流程\n1. 一级响应\n2. 二级专家介入', '现场问题升级流程 一级响应 二级专家介入', '定义现场异常升级路径和责任边界。', 1, 2, 1, '/delivery-overview/incident-escalation', 1, 4, 1, 163, 1),
(4, 1, '验收材料模板', 'acceptance-templates', 'DOC', 'RICH_TEXT', '<h1>验收材料模板</h1><p>包含 FAT/SAT 模板与客户签字页。</p>', '验收材料模板 包含 FAT SAT 模板与客户签字页', '交付验收常用模板集合。', 1, 2, 1, '/delivery-overview/acceptance-templates', 1, 2, 1, 185, 2),
(5, 1, '售后维保手册', 'maintenance-root', 'FOLDER', 'MARKDOWN', '# 售后维保手册', '售后维保手册', '售后维保目录。', 2, 2, 0, '/maintenance-root', 0, 1, 1, 93, 0),
(6, 1, '季度巡检报告模板', 'inspection-template', 'DOC', 'MARKDOWN', '# 季度巡检报告模板\n## 巡检项', '季度巡检报告模板 巡检项', '巡检报告模板与填写要求。', 2, 2, 5, '/maintenance-root/inspection-template', 1, 2, 1, 141, 0),
(7, 1, '备件替换工时表', 'parts-labor-table', 'DOC', 'MARKDOWN', '# 备件替换工时表', '备件替换工时表', '标准备件替换工时与审批口径。', 2, 3, 5, '/maintenance-root/parts-labor-table', 1, 1, 1, 81, 1),
(8, 1, '合规培训地图', 'compliance-map', 'DOC', 'MARKDOWN', '# 合规培训地图', '合规培训地图', '覆盖质量、安全和现场作业的培训矩阵。', 3, 3, 0, '/compliance-map', 0, 1, 1, 97, 0),
(9, 1, '高空作业安全指引', 'height-safety-guide', 'DOC', 'MARKDOWN', '# 高空作业安全指引', '高空作业安全指引', '针对高空作业的审批、装备和旁站要求。', 3, 4, 0, '/height-safety-guide', 0, 1, 1, 154, 1),
(10, 2, '服务值班交接清单', 'service-handover-checklist', 'DOC', 'MARKDOWN', '# 服务值班交接清单\n- 更新值班手机\n- 确认升级联系人', '服务值班交接清单 更新值班手机 确认升级联系人', '用于区域服务团队每日交接和值班升级。', 4, 1, 0, '/service-handover-checklist', 0, 2, 1, 64, 0);

INSERT INTO document_version (document_id, version, title, format, content, content_text, user_id, remark)
VALUES
(2, 1, '项目启动清单', 'MARKDOWN', '# 项目启动清单\n- 确认客户信息', '项目启动清单 确认客户信息', 1, '首次创建'),
(2, 2, '项目启动清单', 'MARKDOWN', '# 项目启动清单\n- 确认客户信息\n- 确认硬件版本', '项目启动清单 确认客户信息 确认硬件版本', 1, '补充硬件核对项'),
(3, 1, '现场问题升级流程', 'MARKDOWN', '# 现场问题升级流程', '现场问题升级流程', 2, '标准化升级路径'),
(10, 1, '服务值班交接清单', 'MARKDOWN', '# 服务值班交接清单', '服务值班交接清单', 1, '首次创建');
