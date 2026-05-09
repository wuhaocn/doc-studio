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
- 工作区最近审计记录、汇总与 CSV 导出
- 知识库进入、创建与基础管理
- 知识库内文档树浏览与维护
- 知识库与当前节点最近审计记录
- 知识库与文档回收站 / 恢复
- 独立文档阅读页与编辑页
- 版本查看、回滚、阅读链接复制与受控分享
- 公开分享只读页
- Service Account / API key 管理弹层
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
- `/share/:token`
- `/`
- `/search`
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
- 使用轻量上下文顶栏，而不是大 Hero
- 三栏结构：左侧文档树、中间文档舞台、右侧上下文面板
- 阅读与编辑共用纸面感中心舞台
- 蓝灰白工作区视觉
- 高频动作占主 CTA，低频信息默认收起

### 工作台首页重点

- 上次编辑文档快捷入口
- 最近文档
- 顶栏工作区切换与统一搜索
- 轻量概览条
- 知识库入口列表
- 默认折叠的成员信息
- 对管理员显示最近工作区审计记录
- 对管理员提供 API key 管理入口

### 知识库详情页重点

- 紧凑的知识库上下文栏，可展开描述
- 左侧文档树支持展开收起与轻量批量动作
- 中央阅读舞台突出标题与主操作
- 文件夹节点引导下一步动作，而不是空白预览
- 版本信息按需展开
- 右侧上下文面板只展示知识库与当前节点信息
- 管理角色可直接查看知识库和当前节点最近审计记录
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

当前真实会话 token 形式：

- `Authorization: Bearer session:{opaqueToken}`

本地种子账号：

- `admin / 123456`
- `editor / 123456`

说明：

- 上述种子账号仅用于本地联调；真实进入产品应优先走 Owner 注册和邀请接受。
- Axios 在受保护请求遇到 `401` 时会先尝试一次 `refresh`，失败后再清空本地会话。
- Axios 默认附带 `X-Memora-Client: memora-web-app`，用于后端审计来源标记。

前端启动后会调用：

- `POST /api/v1/auth/register-owner`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/session`
- `GET /api/v1/workspaces/joined`
- `POST /api/v1/workspaces/{tenantId}/switch`
- `POST /api/v1/tenants/current/invites`
- `GET /api/v1/tenants/current/invites`
- `POST /api/v1/tenants/current/invites/{id}/revoke`
- `GET /api/v1/invites/{token}`
- `POST /api/v1/invites/accept`
- `GET /api/v1/documents?keyword=...`
- `GET /api/v1/audit-logs`

---

## 环境说明

在当前机器上，Web 端已经通过：

- `npm run lint`
- `npm run test:unit`
- `npm run build`

如果跨 CPU 架构复用依赖，需要重新执行 `npm install`，避免 `esbuild` 平台不匹配。
