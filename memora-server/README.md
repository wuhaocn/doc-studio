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
- 关键操作审计、失败审计、汇总、导出与手动归档
- 文档受控分享与外部只读访问
- 机器主体、访问密钥与开放文档接口
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
- `seed-data.sql`
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
- `AuditLogArchive`
- `DocumentShareLink`
- `ServiceAccount`
- `ApiKey`
- `ApiKeyScope`

数据库脚本：

- [schema.sql](./memora-server-start/src/main/resources/db/schema.sql)
- [seed-data.sql](./memora-server-start/src/main/resources/db/seed-data.sql)

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
- `GET /api/v1/auth/sessions`
- `POST /api/v1/auth/sessions/revoke-others`
- `POST /api/v1/auth/sessions/{sessionId}/revoke`
- `GET /api/v1/auth/tenant-sessions`
- `POST /api/v1/auth/tenant-sessions/{sessionId}/revoke`
- `POST /api/v1/auth/tenant-sessions/users/{userId}/revoke`

### 运行时配置

- `GET /services/config`

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
- `POST /api/v1/audit-logs/retention/run`

### 受控分享

- `POST /api/v1/document-shares`
- `GET /api/v1/documents/{documentId}/shares`
- `POST /api/v1/document-shares/{shareId}/revoke`
- `GET /api/v1/public-shares/{token}`
- `POST /api/v1/public-shares/{token}/access`

### 机器主体 / 访问密钥

- `GET /api/v1/service-accounts`
- `POST /api/v1/service-accounts`
- `POST /api/v1/service-accounts/{serviceAccountId}/api-keys`
- `POST /api/v1/service-accounts/{serviceAccountId}/disable`
- `POST /api/v1/service-accounts/{serviceAccountId}/enable`
- `POST /api/v1/api-keys/{apiKeyId}/disable`
- `POST /api/v1/api-keys/{apiKeyId}/reveal`
- `POST /api/v1/api-keys/{apiKeyId}/revoke`
- `PUT /api/v1/api-keys/{apiKeyId}/scope`
- `POST /api/v1/api-keys/{apiKeyId}/rotate`

说明：

- `service account` 代表机器主体，可整体启停
- 同一主体下支持签发多把 key
- 每把 key 可按知识库分别配置 `READ / WRITE`
- 新签发或轮换的 key 会保留一份加密展示副本；管理页列表会直接返回仍可展示的当前密钥，单独再次展示动作仍会写入审计
- 历史旧 key 如果创建时没有展示副本，无法直接恢复明文，需要先重新生成
- 当前访问密钥只绑定 `service account`，不按用户逐个签发个人 key；如果后续需要个人程序接入，应单独设计 `Personal Access Token`

### 开放文档接口

- `GET /api/v1/open/knowledge-bases`
- `GET /api/v1/open/knowledge-bases/{knowledgeBaseId}/documents`
- `GET /api/v1/open/knowledge-bases/{knowledgeBaseId}/documents/by-source`
- `GET /api/v1/open/documents/search`
- `GET /api/v1/open/documents/{documentId}`
- `GET /api/v1/open/documents/{documentId}/consume`
- `GET /api/v1/open/documents/{documentId}/versions`
- `POST /api/v1/open/documents`
- `PUT /api/v1/open/documents/{documentId}`
- `POST /api/v1/open/documents/upsert`
- `POST /api/v1/open/documents/batch-upsert`
- `DELETE /api/v1/open/documents/{documentId}`
- `POST /api/v1/open/documents/{documentId}/restore`
- `POST /api/v1/open/documents/{documentId}/rollback/{versionId}`

说明：

- 工作区统一搜索 V1 复用 `GET /api/v1/documents?keyword=...`
- 统一搜索结果当前只返回当前会话可阅读的正文文档
- Open API 当前支持：列知识库、列文档、搜索、读取、查看版本、创建、更新、`upsert`、`batch-upsert`、删除、恢复、回滚
- Open API 创建/更新请求只接收源内容字段 `format + content (+ summary)`；`contentText` 由服务端统一派生，不再接受外部传入
- Open API 创建/更新也可落 `sourceExternalId / sourceRevision`；`upsert / batch-upsert` 进一步在知识库内按 `sourceExternalId` 收口持续同步语义，并返回 `CREATED / UPDATED / SKIPPED / CONFLICTED`
- Open API 已补“保存后消费”基线：可按 `sourceExternalId` 直接定位文档，也可按 `metadata / rendered / source / plain / summary` 视图消费文档
- `/services/config` 会暴露 Web 运行时标题、浏览器会话元数据和基础特性开关，供前端启动时读取

### 文档内容维护接口

- `POST /api/v1/documents/maintenance/normalize-content`

说明：

- 用于管理员按工作区或知识库范围执行历史文档内容归一化
- 会修正文档与历史版本中的 `docType / format / contentText / summary`
- 读取链路也会兜底识别历史正文里的字面量 `\n / \r\n`，避免 Markdown / HTML 被当成单行脏文本直接透出到阅读、分享和 Open API
- 默认 `dryRun=true`，先返回命中与待修正数量；显式传 `dryRun=false` 才会真正落库

---

## 本地运行

### 启动

```bash
./start-backend.sh
```

本地开发如需种子账号、H2 控制台和 `demo` token 测试入口：

```bash
./start-backend-dev.sh
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

### 运行配置

默认运行配置使用持久化 H2 文件库：

- JDBC URL: `jdbc:h2:file:./var/memora-runtime/memora_doc`
- 通过 `./start-backend.sh` 启动时，会自动把文件库固定到启动脚本所在目录下的 `var/memora-runtime/memora_doc`，并自动创建目录
- 用户名：`sa`
- 密码：空
- 默认不会加载完整演示种子数据
- 默认会在本地 H2 文件库首次可用时自动引导一个最小管理员工作区：`admin / 123456`
- 默认不接受 `Bearer demo:{tenantId}:{userId}`
- 默认不输出 MyBatis SQL stdout
- 默认只接受显式允许的 Web Origin；可通过 `MEMORA_WEB_ALLOWED_ORIGINS` 覆盖

`dev` profile 使用 H2 内存库并加载种子数据：

- 启动命令：`./start-backend-dev.sh`
- H2 控制台：`http://localhost:8080/h2-console`
- 仅 `dev/test` profile 会开启 `demo` token，并加载完整演示账号与样例文档数据

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
- 关键操作审计、最小查询与手动归档
- 审计自动归档调度
- Owner 注册、登录、登出、刷新与 session
- 工作区邀请、邀请列表、撤销与接受邀请
- 真实 session 与浏览器 `HttpOnly Cookie`
- 设备级会话列表、移除其他设备会话与管理员工作区会话治理
- `/services/config` 运行时配置入口
- 面向企业知识库场景的开发 / 测试种子数据

### 未实现

- 更细粒度的生产级权限系统
- 更细跨端设备策略与更长周期会话策略
- 完整协作编辑模型
- 搜索索引化与更大规模检索治理
- 独立归档服务与更深自动化批量治理

### 当前会话策略

后端控制器已不再硬编码 tenant 或 user，主业务链路统一从 `CurrentAccessContext` 解析当前主体。

默认本地 H2 文件运行态会自动引导一个最小管理员账号：

- `admin / 123456`

完整演示账号仅在 `dev/test` profile 下可用：

- `admin / 123456`
- `editor / 123456`
- `reviewer / 123456`
- `viewer / 123456`

当前真实会话形式：

- Web 管理台：`HttpOnly Cookie`
- 直连或自动化客户端：`Authorization: Bearer session:{opaqueToken}`

当前安全收口：

- `UserSession.access_token` 仅保存哈希值，后端不再明文落库存储真实 session token
- 浏览器登录、注册、刷新、邀请接受和工作区切换默认下发 `HttpOnly Cookie`，响应体不再回传前端可读 token
- `user_session` 已补 `clientType / userAgent / ipAddress / revokedAt` 元数据，用于当前用户自助治理活跃设备会话；返回前只暴露脱敏后的 IP
- 用户密码、API key 和分享访问码新写入时统一使用强哈希；历史 `SHA-256` 数据在首次成功校验后自动升级
- 邀请 token 与公开分享 token 仅在创建当下明文返回一次，数据库与后续列表接口只保留哈希或脱敏结果
- 公开分享与邀请访问路径进入审计前会先做 token 脱敏
- 500 响应不再直接回显内部异常文本，只返回通用文案和 `requestId`

当前治理策略：

- `GET /api/v1/audit-logs/summary` 会返回自动归档开关、cron 和时区
- 默认启用定时自动归档，按 `memora.audit.retention-scheduler.*` 配置每日执行
- 当前仍保留 `POST /api/v1/audit-logs/retention/run` 供管理员手动触发

除公开分享与邀请读取入口外，主流程接口都要求有效会话；Web 端默认通过 Cookie 续期，非浏览器客户端继续使用 bearer token。

`Authorization: Bearer demo:{tenantId}:{userId}` 仅保留给 `dev/test` profile 和自动化验证，不属于默认运行态能力。

### 当前测试覆盖

仓库已具备在线文档主流程的最小集成测试基线，包括：

- 工作台接口
- 认证、邀请与工作区切换审计
- 知识库创建
- 文档树查询
- 知识库 / 文档关键动作审计写入与查询
- 失败登录审计、审计汇总、CSV 导出与手动归档
- 自动审计归档与设备级会话治理
- 文档受控分享生命周期与访问审计
- 机器主体 / 访问密钥 与开放文档接口
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
