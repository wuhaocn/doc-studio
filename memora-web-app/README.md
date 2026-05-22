# Memora Web App

`memora-web-app` 是 Memora 的 Web 管理台。它当前不是一个全能协作套件，而是：

`继续写作入口 + 专用知识库工作区 + 阅读 / 编辑双模式`

---

## 当前职责

Web 端当前提供：

- Owner 注册页与登录页
- 邀请接受页
- 邀请生成、查看、列表与撤销
- 当前租户工作台与最近编辑文档入口
- 已加入工作区列表与工作区切换
- 工作区统一搜索页与头部全局搜索入口
- 工作区最近审计记录、汇总、CSV 导出与手动归档
- 知识库进入、创建与基础管理
- 知识库内文档树浏览与维护
- 知识库与当前节点最近审计记录
- 知识库与文档回收站 / 恢复
- 独立文档阅读页与编辑页
- 版本查看、回滚、阅读链接复制与受控分享
- 公开分享只读页
- 工作区管理页
- 工作区成员邀请、知识库回收站、开放接入、安全与会话、审计治理分区
- 机器主体启停、多密钥签发、逐知识库 `READ / WRITE` 作用域编辑
- 开放接入页：凭证治理、当前密钥直接展示、单文档保存/更新、按来源回读、`cURL / fetch / 浏览器书签脚本 / 对话框保存与打开示例片段`
- 浏览器捕获与对话框草稿落地页 `/capture/save`
- 知识库级权限管理
- 状态页与只读权限反馈

当前不以这些目标为主：

- 实时协同编辑
- 完整成熟的编辑器体验
- AI 优先工作流
- 资源库优先工作流

---

## 页面结构

```text
memora-web-app/
├── src/
│   ├── components/
│   │   ├── Audit/                   # 审计事件列表
│   │   ├── Feedback/                # 状态页与反馈组件
│   │   └── Layout/                  # 全局布局、头部、知识库侧栏
│   ├── pages/
│   │   ├── Home/                    # 工作台首页
│   │   ├── WorkspaceManage/         # 工作区治理页组
│   │   ├── Capture/                 # 浏览器捕获与对话框草稿保存页
│   │   ├── Search/                  # 工作区统一搜索页
│   │   ├── Document/                # 独立阅读页与编辑页
│   │   ├── Share/                   # 公开分享只读页
│   │   ├── KnowledgeBase/           # 知识库详情页
│   │   └── NotFound/                # 404 页面
│   ├── contexts/                    # 当前会话上下文
│   ├── hooks/                       # 页面级编排与状态控制
│   ├── router/                      # 主路由
│   ├── services/
│   │   ├── api/                     # 后端 API 封装
│   │   └── http/                    # Axios 实例
│   ├── styles/                      # 全局样式
│   └── utils/                       # 富文本清洗与纯工具
```

当前主路由：

- `/login`
- `/register`
- `/accept-invite`
- `/capture/save`
- `/share/:token`
- `/`
- `/search`
- `/workspace/manage`
- `/workspace/manage/members`
- `/workspace/manage/trash`
- `/workspace/manage/access`
- `/workspace/manage/security`
- `/workspace/manage/audit`
- `/kb/:id`
- `/docs/:documentId`
- `/docs/:documentId/edit`

---

## 关键文件

### 布局与导航

- [Layout.jsx](./src/components/Layout/Layout.jsx)
- [Header.jsx](./src/components/Layout/Header.jsx)
- [Sidebar.jsx](./src/components/Layout/Sidebar.jsx)
- [AuthContext.jsx](./src/contexts/AuthContext.jsx)

### 页面

- [Home.jsx](./src/pages/Home/Home.jsx)
- [SearchPage.jsx](./src/pages/Search/SearchPage.jsx)
- [DocumentReaderPage.jsx](./src/pages/Document/DocumentReaderPage.jsx)
- [DocumentEditorPage.jsx](./src/pages/Document/DocumentEditorPage.jsx)
- [KnowledgeBaseDetail.jsx](./src/pages/KnowledgeBase/KnowledgeBaseDetail.jsx)
- [KnowledgeBaseTreePanel.jsx](./src/components/KnowledgeBase/KnowledgeBaseTreePanel.jsx)
- [KnowledgeBaseDocumentPanel.jsx](./src/components/KnowledgeBase/KnowledgeBaseDocumentPanel.jsx)
- [KnowledgeBaseContextPanel.jsx](./src/components/KnowledgeBase/KnowledgeBaseContextPanel.jsx)
- [AuditEventList.jsx](./src/components/Audit/AuditEventList.jsx)

### 页面控制与工具

- [PageState.jsx](./src/components/Feedback/PageState.jsx)
- [useKnowledgeBaseDetailController.js](./src/hooks/useKnowledgeBaseDetailController.js)
- [knowledgeBaseTree.js](./src/utils/knowledgeBaseTree.js)
- [workspaceSearch.js](./src/utils/workspaceSearch.js)
- [documentContent.js](./src/utils/documentContent.js)

### API

- [workspaceApi.js](./src/services/api/workspaceApi.js)
- [knowledgeBaseApi.js](./src/services/api/knowledgeBaseApi.js)
- [documentApi.js](./src/services/api/documentApi.js)
- [authApi.js](./src/services/api/authApi.js)
- [auditApi.js](./src/services/api/auditApi.js)
- [axios.js](./src/services/http/axios.js)

---

## 当前产品与体验方向

最新 UI 阶段记录：

- [workspace-ui-status-2026-04-29.md](./docs/workspace-ui-status-2026-04-29.md)

说明：

- 该文件用于记录最近一轮 Web 收口结果。
- 当前 Web 的 canonical 范围仍以仓库根 README 和 `doc/` 文档为准。

当前方向是：

- 优先继续编辑，而不是优先管理面板
- 采用知识工作区布局，而不是运营后台布局
- 使用轻量上下文顶栏，只保留当前页面语义、搜索、工作区切换和账号动作
- 身份页优先服务“完成进入动作”，而不是同时解释整套系统模型
- 登录页默认只保留登录动作本身，工作区地址作为可选字段直接内联展示
- 注册页默认只收集开通工作区的必要信息，自定义工作区地址按需展开
- 邀请接受页优先从链接读取邀请，并锁定受邀邮箱，减少无效填写
- 三栏结构：左侧文档树、中间文档舞台、右侧上下文面板
- 阅读与编辑共用纸面感中心舞台
- 蓝灰白工作区视觉
- 高频动作占主 CTA，低频信息默认收起

### 工作台首页重点

- 默认首屏只保留“继续编辑 + 知识库列表”
- 顶栏只保留当前页面上下文、统一搜索、工作区切换和账号动作
- 工作区摘要只保留当前名称、知识库数量和当前角色
- 邀请、回收站、开放接入、安全与审计全部从顶栏“工作区管理”进入
- 首页不再承载治理区块，避免和写作入口混层
- 开放接入页先给“凭证治理”，导入验证工具按需展开，避免把调用说明和主体管理混在同一层

### 知识库详情页重点

- 顶部只保留知识库标题、角色边界和少量主动作，不再在树面板重复堆入口
- 左侧文档树聚焦导航、筛选和批量整理，不再承载重复的新建按钮
- 中央阅读舞台继续承担主操作，文档和目录都给出明确下一步动作
- 右侧上下文面板默认只显示知识库边界、当前节点和权限边界
- 管理角色的知识库审计和节点审计改为按需展开，避免默认噪音
- 显式展示当前角色能做什么、不能做什么
- 支持阅读专注模式

### 文档编辑页重点

- 只保留标题、保存状态与高频动作
- 编辑页不再保留知识库侧栏
- 继续沿用轻量顶部栏风格
- 主动作包括：返回知识库、阅读文档、复制阅读链接、查看版本
- 管理角色可从编辑页直接创建受控分享

### 文档阅读页重点

- 只展示阅读所需信息
- 提供返回知识库、复制阅读链接、继续编辑
- 管理角色可从阅读页直接创建受控分享
- 阅读页始终是独立路由，不强制绕回知识库
- 当前角色只读时不再暴露误导性的编辑入口

---

## 交互规则

### 页面角色

- 工作台首页是继续工作的入口，不是管理后台首页
- 搜索页是跨知识库的工作区入口，不是局部筛选弹层
- 知识库详情页是文档工作区，不是后端详情页
- 编辑页只服务编辑
- 阅读页只服务阅读，并继续受当前认证会话控制

### 命名口径

- 进入知识库使用 `Enter Knowledge Base`
- 创建知识库和文档统一使用 `New`
- 文档正文查看使用 `Read Document`
- 文档主动作使用 `Continue Editing / Copy Reading Link / View Versions`
- 返回工作区使用 `Back to Knowledge Base`

### 低频信息

- 版本、成员、权限默认按需展示
- 只有核心工作流动作占用主按钮位
- 弹窗和抽屉沿用产品语言，不使用后台接口式措辞
- 浏览器书签脚本不直接跨站打开放接口，而是跳到 Memora 自己的 `/capture/save` 页面再继续执行

---

## 本地开发

安装依赖：

```bash
npm install
```

启动开发服务器：

```bash
npm run dev
```

默认后端地址：

- `http://localhost:8080`

Axios 基础配置：

- [axios.js](./src/services/http/axios.js)

当前浏览器真实会话形式：

- `HttpOnly Cookie`

当前浏览器本地保存内容：

- `sessionStorage` 只保存当前用户与工作区快照，不保存 bearer token

本地种子账号：

- `admin / 123456`
- `editor / 123456`

说明：

- 上述种子账号仅在后端以 `dev` profile 启动时可用；真实进入产品应优先走 Owner 注册和邀请接受。
- 登录页会根据 `GET /services/config` 的真实运行态提示当前是否允许种子账号登录；默认运行态不会再暗示 `admin / 123456` 可用。
- Axios 在受保护请求遇到 `401` 时会先尝试一次 `refresh`，失败后再清空本地会话。
- Axios 默认附带 `X-Memora-Client: memora-web-app`，用于后端审计来源标记。
- 启动阶段会先读取 `GET /services/config`，同步运行时标题、浏览器会话元数据和基础特性开关。
- 前端会优先把旧 `localStorage` 会话迁移到 `sessionStorage`，并清除其中遗留 bearer token。
- 登录、退出登录和工作区切换会通过 `localStorage` 广播同步到其他标签页，而当前会话快照仍只保存在 `sessionStorage`。
- “工作区管理 / 安全与会话”页会展示当前工作区的活跃会话，并支持移除其他设备会话；管理员还可查看工作区设备概览、逐设备移除成员会话和按成员批量清理会话。
- “工作区管理 / 开放接入”管理的是机器主体和访问密钥，不是用户个人凭证。
- 根页面已补 `Referrer-Policy: no-referrer`，减少公开分享 token 因外链资源被带出站外。
- 邀请链接和文档分享链接都只在创建当下展示一次，历史列表只保留状态与治理动作。

前端启动后会调用：

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
- `GET /services/config`
- `GET /api/v1/workspaces/joined`
- `POST /api/v1/workspaces/{tenantId}/switch`
- `POST /api/v1/tenants/current/invites`
- `GET /api/v1/tenants/current/invites`
- `POST /api/v1/tenants/current/invites/{id}/revoke`
- `GET /api/v1/invites/{token}`
- `POST /api/v1/invites/accept`
- `GET /api/v1/documents?keyword=...`
- `GET /api/v1/audit-logs`
- `POST /api/v1/audit-logs/retention/run`

---

## 环境说明

在当前机器上，Web 端已经通过：

- `npm run lint`
- `npm run test:unit`
- `npm run build`

如果跨 CPU 架构复用依赖，需要重新执行 `npm install`，避免 `esbuild` 平台不匹配。
