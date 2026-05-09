# Memora Server

`memora-server` 是 Memora 的后端服务，当前职责聚焦为：

`多租户工作区 + 知识库 + 文档树 + 版本`

它不再是旧的单用户知识库 demo，也不再围绕资源库、AI 或技能系统展开。

---

## 当前职责

后端当前提供：

- 工作台聚合接口
- 注册、登录、邀请与访问上下文解析
- 已加入工作区列表与工作区切换
- 知识库管理
- 文档树管理
- 工作区统一搜索 V1
- 文档版本快照与回滚
- 文档与知识库软删除恢复接口
- 关键操作审计、失败审计、汇总与导出
- 文档受控分享与外部只读访问
- Service Account、API key 与开放文档接口
- 初始化种子数据

---

## 模块结构

```text
memora-server/
├── memora-server-common/   # 通用响应、异常、基础工具
├── memora-server-manager/  # 核心业务逻辑
└── memora-server-start/    # 应用启动、配置、数据库脚本、测试
```

### 模块说明

#### `memora-server-common`

提供：

- 统一响应结构
- 业务异常
- 通用基础工具

#### `memora-server-manager`

当前核心服务包括：

- `WorkspaceController / WorkspaceService`
- `WorkspaceAccessController`
- `KnowledgeBaseController / KnowledgeBaseService`
- `DocumentController / DocumentService`

#### `memora-server-start`

提供：

- Spring Boot 启动入口
- `application.yml`
- `schema.sql`
- `data.sql`
- 集成测试

---

## 核心模型

当前主模型：

- `Tenant`
- `TenantMember`
- `UserAccount`
- `UserSession`
- `TenantInvite`
- `KnowledgeBase`
- `KnowledgeBaseMember`
- `Document`
- `DocumentVersion`
- `AuditLog`
- `DocumentShareLink`
- `ServiceAccount`
- `ApiKey`
- `ApiKeyScope`

数据库脚本：

- [schema.sql](./memora-server-start/src/main/resources/db/schema.sql)
- [data.sql](./memora-server-start/src/main/resources/db/data.sql)

---

## 接口范围

### 工作台

- `GET /api/v1/workspaces/current/dashboard`
- `GET /api/v1/workspaces/joined`
- `POST /api/v1/workspaces/{tenantId}/switch`

### 认证与会话

- `POST /api/v1/auth/register-owner`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/session`

### 成员邀请

- `POST /api/v1/tenants/current/invites`
- `GET /api/v1/tenants/current/invites`
- `POST /api/v1/tenants/current/invites/{id}/revoke`
- `GET /api/v1/invites/{token}`
- `POST /api/v1/invites/accept`

### 知识库

- `POST /api/v1/knowledge-bases`
- `PUT /api/v1/knowledge-bases/{id}`
- `DELETE /api/v1/knowledge-bases/{id}`
- `GET /api/v1/knowledge-bases/trash`
- `POST /api/v1/knowledge-bases/{id}/restore`
- `GET /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases/tenant/{tenantId}`
- `GET /api/v1/knowledge-bases/user/{userId}`
- `GET /api/v1/knowledge-bases/{id}`
- `GET /api/v1/knowledge-bases/{id}/members`
- `PUT /api/v1/knowledge-bases/{id}/members`
- `GET /api/v1/knowledge-bases/{id}/documents`
- `GET /api/v1/knowledge-bases/{id}/document-tree`

### 文档

- `POST /api/v1/documents`
- `PUT /api/v1/documents/{id}`
- `DELETE /api/v1/documents/{id}`
- `GET /api/v1/documents/trash`
- `POST /api/v1/documents/{id}/restore`
- `POST /api/v1/documents/batch-move`
- `POST /api/v1/documents/batch-delete`
- `PUT /api/v1/documents/sort`
- `GET /api/v1/documents`
- `GET /api/v1/documents/{id}`
- `GET /api/v1/documents/knowledge-base/{knowledgeBaseId}`
- `GET /api/v1/documents/knowledge-base/{knowledgeBaseId}/tree`
- `GET /api/v1/documents/{id}/versions`
- `GET /api/v1/documents/versions/{versionId}`
- `POST /api/v1/documents/{id}/rollback/{versionId}`

### 审计

- `GET /api/v1/audit-logs`
- `GET /api/v1/audit-logs/summary`
- `GET /api/v1/audit-logs/export`

### 受控分享

- `POST /api/v1/document-shares`
- `GET /api/v1/documents/{documentId}/shares`
- `POST /api/v1/document-shares/{shareId}/revoke`
- `GET /api/v1/public-shares/{token}`
- `POST /api/v1/public-shares/{token}/access`

### Service Account / API key

- `GET /api/v1/service-accounts`
- `POST /api/v1/service-accounts`
- `POST /api/v1/api-keys/{apiKeyId}/disable`
- `POST /api/v1/api-keys/{apiKeyId}/revoke`
- `POST /api/v1/api-keys/{apiKeyId}/rotate`

### 开放文档接口

- `GET /api/v1/open/knowledge-bases`
- `GET /api/v1/open/knowledge-bases/{knowledgeBaseId}/documents`
- `GET /api/v1/open/documents/{documentId}`
- `GET /api/v1/open/documents/{documentId}/versions`
- `POST /api/v1/open/documents`
- `PUT /api/v1/open/documents/{documentId}`

说明：

- 工作区统一搜索 V1 复用 `GET /api/v1/documents?keyword=...`
- 统一搜索结果当前只返回当前会话可阅读的正文文档

---

## 本地运行

### 启动

```bash
./start-backend.sh
```

### 验证

```bash
./scripts/backend-test.sh
```

### 构建

```bash
./scripts/backend-build.sh
```

如果需要绕过 wrapper，强制改用系统 Gradle：

```bash
MEMORA_GRADLE_CMD=gradle ./scripts/backend-test.sh
```

也可以直接从 IDE 启动：

- `com.memora.MemoraApplication`

### 开发数据库

默认开发环境使用 H2 内存库：

- JDBC URL: `jdbc:h2:mem:memora_doc`
- 用户名：`sa`
- 密码：空

H2 控制台：

- `http://localhost:8080/h2-console`

---

## 已知环境问题

当前本地环境已知问题：

- `./start-backend.sh`、`./scripts/backend-test.sh`、`./scripts/backend-build.sh` 都会优先使用仓库内的 `./gradlew`
- 如需临时切换到系统 Gradle，可通过 `MEMORA_GRADLE_CMD=gradle` 覆盖
- 使用 wrapper 时，脚本会默认把 `GRADLE_USER_HOME` 设置到仓库内，减少对用户主目录写权限的依赖
- 如果本地没有已缓存的 wrapper 分发包，仍然依赖可访问的 Gradle 下载源
- 当前机器上的后端测试仍可能受 macOS aarch64 的 Gradle native 库问题阻塞

这些属于环境问题，不是当前主业务逻辑回归。

---

## 当前实现状态

### 已实现

- 多租户基础模型
- 统一 `CurrentAccessContext`
- 工作台聚合接口
- 已加入工作区列表与工作区切换
- 知识库核心模型
- 知识库成员权限接口
- 文档树核心模型
- 工作区统一搜索 V1
- 版本快照与回滚
- 删除元数据与恢复接口
- 关键操作审计与最小查询
- Owner 注册、登录、登出、刷新与 session
- 工作区邀请、邀请列表、撤销与接受邀请
- bearer token 真实 session
- 面向企业知识库场景的种子数据

### 未实现

- 更细粒度的生产级权限系统
- 更完整的会话治理与跨端一致性
- 完整协作编辑模型
- 更成熟的搜索质量、索引和回归覆盖
- 更长期的归档型审计留存与批量治理

### 当前会话策略

后端控制器已不再硬编码 tenant 或 user，主业务链路统一从 `CurrentAccessContext` 解析当前主体。

当前本地种子账号：

- `admin / 123456`
- `editor / 123456`
- `reviewer / 123456`
- `viewer / 123456`

当前真实会话 token 形式：

- `Authorization: Bearer session:{opaqueToken}`

除 `POST /api/v1/auth/login` 外，主流程接口都要求有效 bearer token。

### 当前测试覆盖

仓库已具备在线文档主流程的最小集成测试基线，包括：

- 工作台接口
- 认证、邀请与工作区切换审计
- 知识库创建
- 文档树查询
- 知识库 / 文档关键动作审计写入与查询
- 失败登录审计、审计汇总与 CSV 导出
- 文档受控分享生命周期与访问审计
- Service Account / API key 与开放文档接口
- 跨租户拒绝
- 角色拒绝
- 知识库级权限覆盖
- 知识库成员权限接口
- bearer token 会话场景
- 全局文档列表权限过滤
- 文档回收站与恢复
- 知识库回收站与恢复

测试文件：

- [OnlineDocumentApiIntegrationTest.java](./memora-server-start/src/test/java/com/memora/OnlineDocumentApiIntegrationTest.java)

---

## 技术边界

后端当前仍有意保持单体结构。

原因：

- 领域模型仍在收敛
- 当前最高优先级仍是把主流程做稳
