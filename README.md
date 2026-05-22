# Memora

Memora 当前聚焦为一个面向团队与个人知识管理场景的在线文档系统。

当前主线不是内置 AI 推理或对话，而是把人工或外部 AI 生成的内容沉淀为可管理、可展示、可分享、可消费的文档资产。现阶段主交付链路是：

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
- 机器主体、访问密钥与开放文档 API 已接通，支持主体启停、多密钥、逐知识库 `READ/WRITE` 作用域、重新生成、暂停、删除与并发版本保护；当前访问密钥只面向机器主体，不作为用户个人凭证。
- Open API 已补 `sourceExternalId / sourceRevision / upsert / batch-upsert` 持续同步语义；外部脚本或 AI 写入可在知识库边界内按来源幂等创建、更新、跳过或冲突返回，不再只能依赖单条 CRUD。
- Open API 已补按来源定位与按视图消费的读取闭环：可按 `sourceExternalId` 解析文档，并按 `metadata / rendered / source / plain / summary` 视图提供稳定消费结果。
- Web 端“开放接入”页已从纯说明升级为凭证治理 + 导入验证工具：可直接查看仍可展示的当前密钥、复制使用、选择知识库与父目录、执行保存/更新、按来源回读，并生成 `cURL / fetch / 浏览器书签脚本 / 对话框保存与打开示例片段`。
- 已补浏览器捕获落地页 `/capture/save`，书签脚本会先把当前网页内容带到 Memora Web，再由该页代持访问密钥发起开放接口请求，避免直接跨站调用后端 API。
- `/capture/save` 已支持浏览器捕获参数和对话框预生成草稿双入口：外部工具可直接把 Markdown / HTML 草稿、来源标识和版本号带到页内，再复用现有保存/更新链路写入文档。
- 已补文档内容归一化维护入口，用于修正历史 `docType / format / contentText / summary` 数据。
- 已补轻发布站点基线：知识库可配置公开站点，文档可正式发布到稳定 `siteSlug / publicSlug` 路由，Web 已提供公开页和治理侧栏发布入口。
- 已引入服务端 canonical `renderedHtml / renderChecksum`，Markdown / HTML / 富文本内容在阅读、公开分享和公开站点里不再各自维护不同渲染事实源。
- 已补 `/services/config` 运行时配置入口，Web 启动阶段会读取运行时标题、认证元数据与特性开关。
- 浏览器登录、退出登录和工作区切换已补跨标签页会话同步，不再只更新当前标签页快照。
- 当前工作区已支持设备级会话治理，并补到管理员工作区会话概览：查看活跃会话、移除单个其他设备会话、批量退出其他设备、按成员逐设备清理和批量清理成员会话。
- 审计保留策略已从手动归档扩展为“手动执行 + 定时自动归档”双通道，汇总页会显示自动归档配置。
- 统一搜索已补服务端相关度排序，标题精确命中不再被单纯的最近更新时间覆盖。
- Web 端已形成工作台首页（继续工作优先，治理按需展开）+ 知识库详情页 + 独立阅读页 + 独立编辑页主结构。

未完成：

- 更细粒度的跨端会话策略
- 更细粒度的生产级权限模型
- 更成熟的在线编辑与协作能力
- 独立审计归档服务、自动化批量治理与更细开放接口范围
- 搜索索引化与更大规模下的检索质量治理

当前判断：

- 仓库适合继续围绕在线文档 Web 主流程做闭环
- 不适合再扩展大块外围模块

---

## 当前目标

当前目标不是做一个大而全的平台，而是先把这条链路做稳定：

`tenant -> knowledge base -> document tree -> editing -> versions -> permissions`

下一阶段内容主线：

- 不内置 AI 对话或模型推理能力
- 支持外部 AI 生成的 `Markdown`、`HTML`、富文本内容直接进入知识库
- 在阅读、分享、开放 API、版本和搜索中保持一致语义

当前已经补齐 `P2` 基础版能力：多工作区切换、统一搜索、状态页和更细权限反馈。
当前也已经补齐 `P3` 到 `P5` 基线能力：失败审计、最小导出、手动归档、受控分享、机器主体与访问密钥生命周期、开放文档操作。
当前也已经补齐 `P7` 最小能力：知识库公开站点、文档正式发布、稳定公开路由和服务端统一渲染产物。

下一阶段重点不再是“有没有入口”，而是继续收口 `P8` 的剩余部分：更成熟的对话框接入封装、批量同步保护、权限细化、搜索索引化、跨端会话策略和协作体验；当前已经进入“主链路产品化 + 内容资产公开消费”阶段。

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
- 工作区管理页
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

直接从 IDE 运行 `com.memora.MemoraApplication`，或执行默认启动脚本 `./start-backend.sh` 时，默认本地 H2 文件库会自动引导一个最小管理员工作区，可使用 `admin / 123456` 登录；它只补最小管理员与默认工作区，不加载完整演示文档数据，也不开放 `demo` token。

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
- 机器主体、访问密钥与开放文档接口

### 当前成熟度

- 基础较稳：租户边界、多知识库模型、文档树、基础编辑、版本能力、删除恢复语义基础
- 可演示闭环：身份进入产品、邀请生命周期、工作台、工作区切换、统一搜索、知识库管理、知识库级权限、删除恢复、审计、受控分享、开放写入
- 尚未生产化：更细权限模型、搜索索引化与更大规模检索治理、独立审计归档服务与更深治理自动化

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
