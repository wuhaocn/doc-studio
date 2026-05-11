# API 配置说明

## 基础地址

前端 `axios` 实例在 [axios.js](./src/services/http/axios.js) 中定义：

- 默认 `baseURL`: `http://localhost:8080`
- 接口路径统一由各 API 文件补全 `/api/v1/...`

如果需要通过环境变量覆盖，请使用：

```env
VITE_API_BASE_URL=http://localhost:8080
```

不要把 `/api/v1` 写进 `VITE_API_BASE_URL`，否则会和接口路径重复拼接。

当前 Web 端还会固定带上：

- `X-Memora-Client: memora-web-app`

该请求头当前用于后端记录审计来源；未显式传入时，后端会把来源记为 `DIRECT_API`。

---

## 当前会话方式

当前 Web 端已经切到真实 session：

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/session`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`

登录、注册、接受邀请、刷新或切换工作区成功后：

- 后端会通过 `Set-Cookie` 下发 `HttpOnly` session cookie
- `sessionStorage` 只保存当前用户与工作区快照，不保存 bearer token

Web 端请求默认开启 `withCredentials`；当会话过期且本地仍有当前用户快照时，会先尝试一次 `refresh`，失败后再清空本地会话。

本地种子账号：

- `admin / 123456`
- `editor / 123456`
- `reviewer / 123456`
- `viewer / 123456`

说明：

- 上述账号仅在后端以 `dev` profile 启动时用于本地联调。
- 从空环境进入产品应优先走 `Owner 注册 -> 邀请成员 -> 接受邀请`。
- `/login` 默认只要求 `username + password`；`tenantSlug` 只在多工作区显式指定时展开。
- `/register` 默认自动生成 `tenantSlug`；只有需要固定工作区地址时才手动展开填写。
- `/accept-invite` 会优先从链接读取 `token`，邀请读取成功后会锁定受邀邮箱，避免与邀请记录不一致。
- 邀请链接和文档分享链接都只在创建当下展示一次；如果丢失，需要撤销后重新创建。
- 根页面默认声明 `Referrer-Policy: no-referrer`，公开分享页不应把 token 当作来源信息发给第三方站点。

---

## 当前主要接口

### 身份与工作区

- `POST /api/v1/auth/register-owner`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/session`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/workspaces/current/dashboard`

### 工作区邀请

- `POST /api/v1/tenants/current/invites`
- `GET /api/v1/tenants/current/invites`
- `POST /api/v1/tenants/current/invites/{id}/revoke`
- `GET /api/v1/invites/{token}`
- `POST /api/v1/invites/accept`

### 知识库

- `GET /api/v1/knowledge-bases`
- `GET /api/v1/knowledge-bases/tenant/{tenantId}`
- `GET /api/v1/knowledge-bases/user/{userId}`
- `GET /api/v1/knowledge-bases/{id}`
- `POST /api/v1/knowledge-bases`
- `PUT /api/v1/knowledge-bases/{id}`
- `DELETE /api/v1/knowledge-bases/{id}`
- `GET /api/v1/knowledge-bases/trash`
- `POST /api/v1/knowledge-bases/{id}/restore`
- `GET /api/v1/knowledge-bases/{id}/members`
- `PUT /api/v1/knowledge-bases/{id}/members`
- `GET /api/v1/knowledge-bases/{id}/documents`
- `GET /api/v1/knowledge-bases/{id}/document-tree`

### 文档

- `GET /api/v1/documents`
- `GET /api/v1/documents/knowledge-base/{knowledgeBaseId}`
- `GET /api/v1/documents/knowledge-base/{knowledgeBaseId}/tree`
- `GET /api/v1/documents/{id}`
- `POST /api/v1/documents`
- `PUT /api/v1/documents/{id}`
- `DELETE /api/v1/documents/{id}`
- `GET /api/v1/documents/trash`
- `POST /api/v1/documents/{id}/restore`
- `POST /api/v1/documents/batch-move`
- `POST /api/v1/documents/batch-delete`
- `PUT /api/v1/documents/sort`
- `GET /api/v1/documents/{id}/versions`
- `GET /api/v1/documents/versions/{versionId}`
- `POST /api/v1/documents/{id}/rollback/{versionId}`

### 审计

- `GET /api/v1/audit-logs`
- `GET /api/v1/audit-logs/summary`
- `GET /api/v1/audit-logs/export`
- `POST /api/v1/audit-logs/retention/run`

查询参数：

- `page`
- `size`
- `knowledgeBaseId`
- `objectType`
- `objectId`
- `resultType`
- `storageScope`：导出时支持 `ACTIVE`、`ARCHIVED`、`ALL`

当前约束：

- 不带对象过滤时，工作区级最近事件查询要求当前角色具备租户管理权限
- `knowledgeBaseId` 查询要求当前角色具备该知识库管理权限
- `objectType + objectId` 当前支持 `KNOWLEDGE_BASE`、`DOCUMENT`、`DOCUMENT_SHARE_LINK`、`SERVICE_ACCOUNT`、`API_KEY`

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

---

## 响应格式

统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

## 当前说明

1. Web 主链路已不再依赖 `demo:{tenantId}:{userId}` 占位 token。
2. Web 端真实会话默认通过 `HttpOnly Cookie` 维持，前端不再持有可读 bearer token。
3. 邀请链路已支持生成、查看、列出和撤销，但原始邀请链接只在创建时展示一次。
4. 工作台首页已展示最近审计记录、汇总、活跃/归档导出和手动归档入口，知识库上下文面板继续展示最近记录。
5. 文档受控分享、公开分享页、Service Account / API key 与开放文档接口已属于当前基线能力，原始分享链接同样只在创建时展示一次。
