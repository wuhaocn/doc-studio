# Memora

Memora 当前聚焦为一个面向企业知识管理的在线文档系统，现阶段主交付链路是：

- 多租户工作区
- 知识库管理
- 文档树管理
- 在线阅读与编辑
- 版本快照与回滚
- 权限与阅读入口边界

本仓库不再以旧资源库、AI、技能市场或桌面客户端为当前主线。在在线文档 Web 主流程稳定之前，这些方向都不作为当前产品基线。

---

## 当前状态

已完成：

- 后端已从单用户 demo 收敛为多租户基础模型。
- 核心模型已包含租户、租户成员、知识库、文档树、文档版本。
- Owner 注册、登录、登出、刷新、当前 session 与邀请接受已接通真实 session；Web 端默认走 `HttpOnly Cookie`，非浏览器客户端仍可使用 bearer token。
- 租户角色与知识库级权限覆盖已实现。
- 知识库成员权限接口与前端基础权限感知已接通。
- 跨知识库文档聚合查询已按知识库权限边界过滤。
- 知识库和文档的软删除元数据、回收站列表、恢复接口与 Web 端交互已补齐。
- 工作区邀请已支持生成、查看、列出和撤销；原始邀请链接仅在创建当下展示一次。
- 已加入工作区列表与当前工作区切换已接通真实 session。
- 工作区统一搜索 V1 已接通，并按知识库权限边界过滤结果。
- 知识库详情页、文档阅读页、文档编辑页已补状态页与更细权限反馈。
- 审计日志模型、关键操作追踪、失败审计、汇总、CSV 导出与手动归档已接通，Web 已展示最近记录与治理汇总。
- 受控文档分享已接通：创建、列出、撤销、外部只读访问、访问码、过期与访问审计均已落地；原始分享链接仅在创建当下展示一次。
- Service Account / API key 与开放文档 API 已接通，支持知识库作用域、轮换、禁用、吊销与并发版本保护。
- Web 端已形成工作台首页 + 知识库详情页 + 独立阅读页 + 独立编辑页主结构。

未完成：

- 更完整的会话治理与跨端一致性
- 更细粒度的生产级权限模型
- 更成熟的在线编辑与协作能力
- 独立审计归档服务、自动化批量治理与更细开放接口范围

当前判断：

- 仓库适合继续围绕在线文档 Web 主流程做闭环
- 不适合再扩展大块外围模块

---

## 当前目标

当前目标不是做一个大而全的平台，而是先把这条链路做稳定：

`tenant -> knowledge base -> document tree -> editing -> versions -> permissions`

当前已经补齐 `P2` 基础版能力：多工作区切换、统一搜索、状态页和更细权限反馈。
当前也已经补齐 `P3` 到 `P5` 基线能力：失败审计、最小导出、手动归档、受控分享、Service Account / API key 与开放文档操作。

下一阶段重点不再是“有没有入口”，而是继续收口会话治理、权限细化、搜索质量和协作体验。

---

## 仓库结构

```text
memora-doc/
├── memora-server/        # Spring Boot 后端
├── memora-web-app/       # React Web 管理台
├── ai-context/           # 共享技能事实源
├── .claude/              # Claude 项目级技能 / 命令连接层
├── .codex/               # Codex 项目级技能连接层
├── .cursor/              # Cursor 项目级技能连接层
├── doc/                  # 产品、架构、开发文档
├── scripts/              # 项目脚本与技能校验脚本
├── gradle/               # Gradle Wrapper 文件
├── settings.gradle.kts
└── README.md
```

### 目录说明

#### `memora-server`

后端当前覆盖：

- 工作台聚合
- 知识库管理
- 文档树管理
- 文档版本管理
- 权限与访问控制基础能力

关键模块：

- `memora-server-common`
- `memora-server-manager`
- `memora-server-start`

#### `memora-web-app`

Web 端当前是主交付载体，核心页面包括：

- 工作台首页
- 工作区统一搜索页
- 知识库详情页
- 文档阅读页
- 文档编辑页
- 权限配置入口

#### `doc`

这里存放当前 canonical 文档，从 [doc/README.md](./doc/README.md) 开始阅读。

#### `scripts`

当前项目脚本入口统一收敛在这里：

- `./start-backend.sh`：以默认运行配置启动后端
- `./start-backend-dev.sh`：以 `dev` profile 启动后端并加载种子数据
- `./scripts/backend-test.sh`：执行后端验证
- `./scripts/backend-build.sh`：构建后端产物
- `./scripts/release-build.sh`：构建本轮发布目录

如需强制改用系统 Gradle，可通过环境变量覆盖：

```bash
MEMORA_GRADLE_CMD=gradle ./scripts/backend-test.sh
```

#### `ai-context` / `.claude` / `.codex` / `.cursor`

本仓库采用共享技能分层模型：

- `ai-context/share-skills/`：唯一事实源
- `.claude/skills/`、`.codex/skills/`、`.cursor/skills/`：项目级连接层
- `.claude/commands/opsx/`：项目级命令连接层

约束：

- 共享技能内容只在 `ai-context/share-skills/` 维护。
- 项目级目录使用相对链接连接到共享事实源。
- 技能、命令文档和脚本中不应出现绝对路径。

刷新项目级连接层：

```bash
node scripts/tools/sync-share-skills.js --project-only
```

校验共享技能结构：

```bash
./scripts/check-share-skills.sh
```

---

## 文档入口

### 文档索引

- [doc/README.md](./doc/README.md)

用于理解：

- canonical 文档边界
- 推荐阅读顺序
- 当前唯一事实源文件

### 产品基线

- [在线文档系统产品需求与范围](./doc/architecture/product-requirements-and-scope.md)

### 技术基线

- [在线文档系统技术架构与接口设计](./doc/architecture/technical-architecture-and-api-design.md)
- [行业产品参考与设计取舍](./doc/architecture/industry-product-references-and-trade-offs.md)

### 本地运行与验证

后端入口：

```bash
./start-backend.sh
```

默认会把运行态 H2 文件库固定到项目目录下的 `var/memora-runtime/memora_doc`，并自动创建目录。

本地联调如需种子账号和开发态数据：

```bash
./start-backend-dev.sh
```

后端验证：

```bash
./scripts/backend-test.sh
```

Web 验证：

```bash
cd memora-web-app && npm run lint && npm run test:unit && npm run build
```

安全运行基线：

- 默认运行态不再输出 MyBatis SQL 到 stdout，避免 session / 邀请 / 分享 token 进入控制台日志。
- 浏览器侧当前使用 `HttpOnly Cookie` 持有真实 session；`sessionStorage` 只保留当前用户与工作区快照，不再保存 bearer token。
- API 默认只允许显式配置的 Web Origin 访问；本地默认值为 `http://localhost:8800` 和 `http://127.0.0.1:8800`。
- 运行态 H2 文件目录 `var/` 与 `*.mv.db`/`*.lock.db` 已加入忽略规则，不应提交到仓库。
- 历史遗留的运行态 H2 文件已从版本控制中移除，后续仅保留本地运行用途。

发布构建：

```bash
./scripts/release-build.sh
```

发布目录会统一生成：

- `release/backend/`
- `release/web/`
- `release/BUILD_INFO.txt`
- `release/README.md`

### 计划与交付规范

- [在线文档系统重构设计与计划](./doc/architecture/refactor-design-and-plan.md)
- [开发规范](./doc/development/development-standard.md)
- [上线规范](./doc/development/release-standard.md)
- [代码命名规范](./doc/development/code-naming-standard.md)
- [主流程冒烟验收清单](./doc/development/online-document-main-flow-smoke-checklist.md)

---

## 当前功能清单

### 当前范围内

- 多租户工作区模型
- 工作台首页
- 租户成员列表
- Owner 注册、登录、登出与当前 session
- 已加入工作区列表与工作区切换
- 工作区成员邀请与接受邀请
- 工作区邀请列表与撤销
- 知识库列表与详情
- 知识库成员权限接口
- 知识库权限管理界面
- 工作区统一搜索 V1
- 文档树展示与维护
- 在线文档编辑
- 文档快照与回滚
- 文档回收站 / 恢复交互
- 知识库回收站 / 恢复交互
- 状态页与只读权限反馈
- 关键操作审计、失败记录、汇总、导出与手动归档
- 文档受控分享与外部只读访问
- Service Account、API key 与开放文档接口

### 当前成熟度

- 基础较稳：租户边界、多知识库模型、文档树、基础编辑、版本能力、删除恢复语义基础
- 可演示闭环：身份进入产品、邀请生命周期、工作台、工作区切换、统一搜索、知识库管理、知识库级权限、删除恢复、审计、受控分享、开放写入
- 尚未生产化：更完整的会话治理、更细权限模型、搜索质量与索引化、独立审计归档服务与自动化治理

### 当前明确不做

- 资源库优先工作流
- AI 问答主工作流
- 技能市场
- 会议 / 任务协作系统
- 实时协同编辑
- 桌面同步客户端

---

## 关键实现参考

### 后端

- [WorkspaceController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/WorkspaceController.java)
- [WorkspaceAccessController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/WorkspaceAccessController.java)
- [AuthController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/AuthController.java)
- [KnowledgeBaseController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/KnowledgeBaseController.java)
- [DocumentController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/DocumentController.java)
- [AuditLogController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/AuditLogController.java)
- [WorkspaceService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/WorkspaceService.java)
- [KnowledgeBaseService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/KnowledgeBaseService.java)
- [DocumentService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/DocumentService.java)
- [AuditLogService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/AuditLogService.java)
- [schema.sql](./memora-server/memora-server-start/src/main/resources/db/schema.sql)
