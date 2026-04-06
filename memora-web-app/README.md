# Memora Web App

`memora-web-app` 是 Memora 在线文档系统的 Web 管理台，当前定位不是完整协作办公前端，而是：

`继续写作工作台 + 知识库管理台 + 文档树浏览台 + 轻量文档阅读/编辑页`

---

## 当前职责

Web 端当前主要负责：

- 展示当前租户工作台与最近编辑文档
- 展示知识库矩阵
- 展示可折叠的成员与同步任务概况
- 展示知识库详情
- 展示文档树与文档摘要
- 提供知识库手动同步入口

当前还不承担：

- 实时协同编辑
- 完整文档编辑体验
- AI 主流程
- 资源库主流程

---

## 当前页面结构

```text
memora-web-app/
├── src/
│   ├── components/
│   │   └── Layout/                  # 全局布局、顶部导航、知识库侧栏
│   ├── pages/
│   │   ├── Home/                    # 工作台首页
│   │   ├── Document/                # 独立文档编辑页
│   │   ├── KnowledgeBase/           # 知识库详情
│   │   └── NotFound/                # 404 页面
│   ├── contexts/                    # 当前会话上下文
│   ├── router/                      # 主路由
│   ├── services/
│   │   ├── api/                     # 后端 API 封装
│   │   └── http/                    # Axios 实例
│   ├── styles/                      # 全局样式
│   └── utils/                       # demo 会话存储与辅助函数
```

当前主路由：

- `/login` 登录页
- `/` 工作台
- `/kb/:id` 知识库详情
- `/docs/:documentId` 独立文档阅读页
- `/docs/:documentId/edit` 独立文档编辑页

---

## 当前关键文件

### 布局与导航

- [Layout.jsx](./src/components/Layout/Layout.jsx)
- [Header.jsx](./src/components/Layout/Header.jsx)
- [Sidebar.jsx](./src/components/Layout/Sidebar.jsx)
- [AuthContext.jsx](./src/contexts/AuthContext.jsx)

### 页面

- [Home.jsx](./src/pages/Home/Home.jsx)
- [DocumentReaderPage.jsx](./src/pages/Document/DocumentReaderPage.jsx)
- [DocumentEditorPage.jsx](./src/pages/Document/DocumentEditorPage.jsx)
- [KnowledgeBaseDetail.jsx](./src/pages/KnowledgeBase/KnowledgeBaseDetail.jsx)

### API

- [workspaceApi.js](./src/services/api/workspaceApi.js)
- [knowledgeBaseApi.js](./src/services/api/knowledgeBaseApi.js)
- [documentApi.js](./src/services/api/documentApi.js)
- [authApi.js](./src/services/api/authApi.js)
- [axios.js](./src/services/http/axios.js)

---

## 当前界面设计方向

本轮重构的界面方向是：

- 继续编辑优先
- 首页只保留高频入口
- 知识库管理次级呈现
- 同步和成员信息默认折叠

当前首页聚焦：

- 上次编辑文档直达入口
- 统计卡片
- 最近编辑文档
- 知识库矩阵
- 默认折叠的同步信息
- 默认折叠的成员列表

当前知识库详情聚焦：

- 紧凑知识库上下文条
- 左侧文档树，支持目录折叠和一键展开/收起
- 中部正文阅读主舞台，主按钮直接提供“继续编辑 / 分享文档”
- 目录节点不再展示空白预览，而是直接给出“在当前目录新建文档 / 新建子目录”
- 可折叠的右侧版本记录与同步任务
- 文档专注模式

当前文档编辑页聚焦：

- 只展示当前文档标题、保存状态和高频动作
- 不展示其他知识库信息和知识库侧栏
- 顶栏直接提供“返回列表 / 阅读文档 / 分享文档 / 查看版本”
- 默认把页面空间优先让给正文编辑
- 内置租户内文档分享入口，可直接复制独立阅读链接

当前文档阅读页聚焦：

- 只保留阅读正文所需信息
- 顶栏直接提供“返回列表 / 分享文档 / 继续编辑”
- 分享后打开的是独立阅读页，不强制先回知识库查找

---

## 本地开发

安装依赖：

```bash
npm install
```

启动开发环境：

```bash
npm run dev
```

默认依赖后端地址：

- `http://localhost:8080`

Axios 基础配置：

- [axios.js](./src/services/http/axios.js)

当前会默认通过 bearer token 传递 demo 会话：

- `Authorization: Bearer demo:{tenantId}:{userId}`

当前演示登录账号：

- `admin / 123456`

同时仍兼容旧的请求头上下文：

- `X-User-Id`
- `X-Tenant-Id`

前端启动后会调用：

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/session`

用于登录系统并把本地会话补全为当前租户会话信息，再通过 `AuthContext` 分发给页面。

---

## 当前已知环境问题

当前机器上，Web 端已经完成下面验证：

- `npm run lint`
- `npm run build`

如果后续在跨架构机器上复用已有 `node_modules`，仍可能再次遇到 `esbuild` 平台架构不匹配问题。此时重新执行一次 `npm install` 即可。

---

## 当前实现状态

### 已完成

- 路由收敛到工作台和知识库主流程
- Layout 已修正为 `Outlet` 结构
- API 基础路径已修正
- 首页已改造成工作区工作台
- 知识库详情页已改造成文档树 + 同步面板
- 知识库详情页已改造成三栏布局，支持文档专注模式
- 顶部知识库信息区已收敛为紧凑上下文条，隐藏低频静态信息
- 知识库上下文条默认隐藏，只有在需要知识库级操作时才展开
- 右侧上下文栏已支持折叠，危险操作已降级到“更多操作”
- 文档编辑已切换为独立编辑页，不再在知识库详情页内嵌编辑
- 已补独立文档阅读页，用于分享后的直接阅读入口
- 已补文档分享抽屉，支持复制租户内阅读链接
- 已将知识库详情页的高频动作收敛为“新建文档 / 新建目录 / 继续编辑 / 分享文档”
- 已将目录节点正文区改为下一步操作引导，减少空白预览和管理噪音
- 已补侧边栏知识库列表的预览折叠和“展开更多”
- 已补文档树目录级折叠，以及一键展开/收起目录
- 已将全局头部收敛为轻导航条，去掉低价值的套餐、角色和租户副标题信息
- 已将“新建文档”表单收敛为标题优先，摘要和父级目录改为更多设置
- 已补知识库空态的“创建第一篇文档”直达入口
- 已补工作台最近编辑文档入口，可直接继续阅读或编辑
- 已将工作台第一屏改为“继续上次文档”优先，知识库检索降级到次屏
- 已将工作台的同步信息和协作成员默认折叠，减少首页干扰
- 侧边栏已改成租户下知识库导航
- 已补知识库创建 / 编辑 / 删除基础交互
- 已补知识库管理的基础表单校验与状态反馈
- 已补工作台与侧边栏联动刷新
- 已补文档树新增 / 重命名 / 移动 / 删除 / 上下排序基础交互
- 已补文档树同级拖拽排序基础交互
- 已补文档树批量选择、批量移动和批量删除基础交互
- 已补基于 Tiptap 的正文编辑基础交互和版本列表 / 回滚基础交互
- 已补当前版本与历史版本的基础行级对比视图
- 已补知识库成员权限配置界面基础版
- 已补 demo 登录页、受保护路由和前端统一会话上下文
- 已补知识库详情页的无权限 / 不存在 / 异常状态反馈
- 已清理未接入主路由的 Resource / MemoraAI 旧页面与旧 API 封装
- 已补最小 ESLint 配置并完成 lint 验证
- 已修复当前机器上的 `esbuild` 架构依赖并完成生产构建验证
- 已将主路由、知识库详情页和富文本编辑器改为懒加载，降低首屏主包体积

### 未完成

- 高级编辑能力和协作体验
- 更细粒度权限反馈与异常提示
- 同步冲突 UI
- 搜索与过滤体系

---

## 当前技术边界

Web 端当前是“管理台”，不是“最终协作终端”。

这意味着后续优先级应当是：

1. 在现有知识库管理基础上补齐校验、反馈和权限约束
2. 在现有基础编辑器上补齐高级编辑能力与协作体验
3. 把同步结果展示闭环做完整
4. 再补权限、搜索、发布和更细粒度的版本能力

---

## 推荐阅读顺序

如果你要继续看前端，建议顺序如下：

1. [根目录 README](../README.md)
2. [在线文档系统产品需求与范围](../doc/架构设计/在线文档系统产品需求与范围.md)
3. [在线文档系统技术架构与接口设计](../doc/架构设计/在线文档系统技术架构与接口设计.md)
4. [在线文档系统轻量化收敛评估](../doc/架构设计/在线文档系统轻量化收敛评估.md)
5. [router/index.jsx](./src/router/index.jsx)
6. [Home.jsx](./src/pages/Home/Home.jsx)
7. [KnowledgeBaseDetail.jsx](./src/pages/KnowledgeBase/KnowledgeBaseDetail.jsx)
