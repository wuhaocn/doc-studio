# 在线文档系统技术架构与接口设计

## 1. 文档定位

本文档是 Memora 当前阶段的技术基线文档。

它是以下事实的唯一来源：

- 当前系统架构
- 模块边界
- 核心领域模型
- 接口分层
- 权限、版本等关键约束

本文档只描述系统当前如何工作，以及后续应如何演进，不重新定义产品范围。

---

## 2. 技术目标

架构必须服务这条主业务链路：

`tenant -> knowledge base -> document tree -> reading/editing -> versions -> permissions`

指导原则：

- 在主链路尚未完全收敛前保持单体架构
- 租户边界必须显式
- 权限边界必须显式
- 版本恢复保持非破坏式
- Web 管理台优先于外围接入能力
- AI 生成内容视为外部输入源，而不是系统内置推理流程

---

## 3. 技术栈

### 3.1 Backend

- Java 21
- Spring Boot 3
- MyBatis Plus
- 开发环境 H2
- MySQL Schema 基线
- Gradle 多模块构建

后端模块：

- `memora-server-common`
- `memora-server-manager`
- `memora-server-start`

### 3.2 Web

- React 18
- React Router
- Axios
- CSS Modules
- Vite
- Tiptap

---

## 4. 系统上下文

```text
Web Console
  -> REST API
Spring Boot Monolith
  -> H2 / MySQL
```

职责分工：

- Web 端负责管理、阅读、编辑与配置
- 后端负责租户隔离、权限、版本和当前主链路约束

---

## 5. 模块边界

### 5.1 Auth / Session

职责：

- 解析当前会话
- 提供 Owner 注册、登录、登出、session 和 refresh
- 提供当前用户活跃会话列表、移除单个会话和退出其他设备入口
- 提供工作区切换后的新 session
- 提供工作区邀请与接受邀请
- 提供 Web 运行时配置 `/services/config`
- 解析当前用户与租户上下文
- 在 `user_session` 中保留浏览器 / 接口来源、脱敏 IP、最近活跃时间与撤销时间
- 为后续更强认证和更完整跨端会话治理保留清晰替换点

关键文件：

- [CurrentAccessContext.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/support/CurrentAccessContext.java)
- [AuthController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/AuthController.java)
- [WorkspaceAccessController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/WorkspaceAccessController.java)
- [AuthService.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/service/AuthService.java)

### 5.2 Workspace

职责：

- 聚合当前租户工作台
- 返回当前主体加入的工作区列表
- 返回成员、知识库、最近文档
- 只返回当前会话可见的数据

关键文件：

- [WorkspaceController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/WorkspaceController.java)
- [WorkspaceService.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/service/WorkspaceService.java)

### 5.3 Knowledge Base

职责：

- 管理知识库元数据
- 作为当前主要权限边界
- 提供成员权限配置
- 提供文档树入口
- 提供软删除与恢复基础

关键文件：

- [KnowledgeBaseController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/KnowledgeBaseController.java)
- [KnowledgeBaseService.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/service/KnowledgeBaseService.java)

### 5.4 Document

职责：

- 管理目录与文档节点
- 管理树结构与路径
- 管理文档正文
- 承接人工与外部 AI 生成的正文内容
- 提供工作区统一搜索 V1 结果
- 创建版本快照
- 支持批量移动、批量删除、排序
- 保留删除元数据与恢复语义

关键文件：

- [DocumentController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/DocumentController.java)
- [DocumentService.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/service/DocumentService.java)

路径规则：

- 文档路径唯一性以数据库约束 `knowledgeBaseId + path` 为准，回收站文档仍占用原路径，避免恢复时再次冲突。
- 人工创建、移动或改标识时，服务端必须先按父级目录和 slug 解析唯一 `path`，冲突时自动追加 `-2 / -3` 后缀。
- Open API 创建和 `upsert` 采用同一规则，外部 AI 或脚本提交同名 HTML / Markdown / H5 文档时不应直接暴露唯一键异常。

### 5.5 Version

职责：

- 存储历史快照
- 返回版本列表
- 支持回滚
- 为后续更强 diff 与审计提供基础

关键文件：

- [DocumentVersion.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/entity/DocumentVersion.java)
- [DocumentVersionMapper.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/mapper/DocumentVersionMapper.java)

### 5.6 Audit

职责：

- 记录主链路关键操作成功事件
- 记录当前主体快照、对象、动作、来源与请求路径
- 提供租户、知识库、对象三级最小查询
- 为失败审计、导出和手动归档保留统一模型

关键文件：

- [AuditLog.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/entity/AuditLog.java)
- [AuditLogController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/AuditLogController.java)
- [AuditLogService.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/service/AuditLogService.java)

### 5.7 Publishing / PublicSite

职责：

- 把知识库作为轻量站点容器对外发布
- 管理文档发布状态、公开 slug 与发布时间
- 提供公开站点首页、详情页和只读消费面
- 与受控分享分离，避免 token 分享承担正式发布语义

后续演进要求：

- `knowledge base` 应可开启或关闭站点发布
- `document` 应具备 `DRAFT / REVIEW / PUBLISHED / ARCHIVED` 之类的正式状态
- 公开消费优先读取服务端统一派生的 `renderedHtml`

### 5.8 Machine Access / Ingestion

职责：

- 管理 Service Account 与 API key 生命周期
- 为外部 AI、脚本和浏览器工具提供程序化写入入口
- 在知识库边界内提供 `upsert / batch-upsert / sourceRevision / 幂等` 等可持续同步语义

当前限制：

- 当前 Open API 已补 `upsert / batch-upsert / sourceExternalId / sourceRevision`，来源追踪和幂等写入已正式收口
- 浏览器 / 对话框入口已补到 Web 接入层：书签脚本与对话框草稿都先落到 `/capture/save`，再复用现有 `upsert` 与按来源回读链路；更细公开消费视图与格式协商仍未完成

---

## 6. 核心领域模型

### 6.1 Tenant

含义：

- 企业工作区
- 顶层隔离边界

关键字段：

- `id`
- `name`
- `slug`
- `ownerUserId`
- `status`

### 6.2 TenantMember

含义：

- 租户内的成员与角色

关键字段：

- `tenantId`
- `userId`
- `displayName`
- `role`
- `status`

### 6.3 KnowledgeBase

含义：

- 文档容器
- 当前主要权限边界

关键字段：

- `tenantId`
- `name`
- `slug`
- `description`
- `cover`
- `userId`
- `documentCount`
- `sortOrder`
- `deletedAt`
- `deletedBy`

后续发布字段：

- `siteEnabled`
- `siteSlug`
- `siteTitle`
- `siteDescription`

### 6.4 KnowledgeBaseMember

含义：

- 知识库级权限覆盖

关键字段：

- `knowledgeBaseId`
- `tenantId`
- `userId`
- `role`
- `status`

当前规则：

- 默认权限由租户角色决定
- 知识库成员配置用于做知识库级覆盖
- 文档级权限暂不作为主模型

### 6.5 Document

含义：

- 文档树节点
- 统一承载文件夹和正文文档

关键字段：

- `knowledgeBaseId`
- `parentId`
- `docType`
- `title`
- `summary`
- `content`
- `format`
- `contentText`
- `summary`
- `path`
- `depth`
- `sortOrder`
- `versionNo`
- `deletedAt`
- `deletedBy`

后续发布字段：

- `publishStatus`
- `publicSlug`
- `publishedAt`
- `renderedHtml`
- `renderChecksum`
- `sourceType`
- `sourceExternalId`
- `sourceRevision`
- `metadataJson`

当前规则：

- `docType` 包含 `DOC` 与 `FOLDER`
- 树结构变更必须保持 `parentId`、`path`、`depth` 一致
- 删除、移动、排序都必须校验树一致性
- 软删除必须保留恢复所需元数据
- 恢复时必须校验所属知识库和父级目录状态
- `format` 当前已经存在于模型中，但下一阶段必须收口为正式格式语义
- `content` 应表达该格式下的源内容，而不是模糊的“任意 HTML 缓存”
- `contentText`、`summary` 应服务搜索、摘要和 diff，长期应从统一渲染链路派生
- `publicSlug` 不应复用内部阅读路由语义
- `renderedHtml` 应作为公开消费的 canonical 渲染产物，而不是临时前端结果

### 6.6 DocumentVersion

含义：

- 文档历史版本

关键字段：

- `documentId`
- `version`
- `title`
- `format`
- `content`
- `contentText`
- `userId`
- `remark`
- `createdAt`

当前规则：

- 保存正文时生成快照
- 回滚不应破坏原历史链

### 6.7 AuditLog

含义：

- 主链路治理基线
- 关键操作追踪记录

关键字段：

- `tenantId`
- `knowledgeBaseId`
- `knowledgeBaseName`
- `actorType`
- `actorUserId`
- `actorDisplayName`
- `actorRole`
- `objectType`
- `objectId`
- `objectTitle`
- `actionType`
- `resultType`
- `detail`
- `sourceType`
- `requestMethod`
- `requestPath`
- `createdAt`

当前规则：

- 当前阶段只记录成功事件
- 工作区级查询要求租户管理权限
- 知识库或对象级查询要求对应知识库管理权限
- Web 端通过 `X-Memora-Client` 标记来源，未携带时记为 `DIRECT_API`

### 6.8 当前模型边界

当前持久化模型已经不再包含：

- 本地同步任务表
- 知识库本地目录绑定字段
- 文档来源路径字段

这些能力不属于当前 Web 主链路基线，也不再作为后端内部保留骨架继续维护。

## 7. 接口分层

### 7.1 控制器层

职责：

- 接收请求
- 参数校验
- 返回统一响应
- 明确接口边界

不负责：

- 堆叠复杂业务逻辑
- 直接硬编码当前用户或租户

### 7.2 服务层

职责：

- 承载业务流程
- 做租户边界与权限判断
- 保证树结构、版本等核心约束

### 7.3 持久层

职责：

- 数据读写
- 基础查询与更新

不负责：

- 承担业务规则

---

## 8. 关键约束

### 8.1 权限约束

- 所有主流程接口都必须基于当前会话解析 tenant 与 user
- 不能依赖前端隐藏按钮做权限控制
- 阅读链接只能作为直达入口，不能等价于放宽权限
- 跨知识库聚合查询必须继承知识库权限边界
- 统一搜索只能返回当前会话可阅读的正文文档

### 8.2 版本约束

- 保存正文时必须明确何时创建版本
- 回滚必须可追溯
- 历史版本不能被破坏性覆盖

### 8.3 删除与恢复约束

- 删除默认采用软删除
- 删除后的对象必须保留 `deletedAt` 与 `deletedBy`
- 恢复不能绕过知识库权限边界
- 恢复子文档前必须保证父级目录已可用

### 8.4 审计约束

- 审计写入不得绕过租户边界
- 审计查询不得向无管理权限主体暴露工作区关键操作
- 审计对象过滤必须显式提供 `objectType + objectId`
- 失败审计、导出、手动归档与归档导出已属于当前已交付基线，但长期留存仍未上升到独立归档服务

### 8.5 内容格式与展示约束

- `RICH_TEXT`、`MARKDOWN`、`HTML` 应被视为正式文档格式
- 不可信 HTML 不能直接按生产能力渲染
- Markdown 渲染、HTML 白名单净化、富文本输出必须走统一展示链路
- 阅读页、知识库预览、公开分享页、开放 API 不能各自定义一套格式语义

### 8.6 发布与公开消费约束

- `受控分享` 与 `正式发布` 必须是两套不同产品模型
- 正式发布必须使用稳定 slug，不得依赖一次性 token
- 公开站点不得绕过知识库边界直接发布未授权内容
- 公开消费应优先依赖服务端统一 `renderedHtml`，而不是每个前端各自渲染
- 当前基线支持 `HTML 文档` 安全发布，不支持脚本执行、iframe 宿主或源码级应用运行时

---

## 9. 当前代码对应的改造点

### 9.1 数据模型与服务端语义

相关文件：

- `memora-server/memora-server-manager/src/main/java/com/memora/manager/entity/Document.java`
- `memora-server/memora-server-manager/src/main/java/com/memora/manager/entity/DocumentVersion.java`
- `memora-server/memora-server-manager/src/main/java/com/memora/manager/service/DocumentService.java`
- `memora-server/memora-server-manager/src/main/java/com/memora/manager/service/OpenApiDocumentService.java`

当前实现：

- `format` 已收口为 `RICH_TEXT`、`MARKDOWN`、`HTML` 三种正式格式
- `content` 表示格式对应的源内容，不再默认解释为“任意 HTML 缓存”
- `contentText`、`summary` 由服务端统一派生，调用方不再主导正文搜索语义
- 版本快照会保留 `format / content / contentText`，回滚后重新校正摘要
- 新写入与读取链路只认显式 `docType / format / content / contentText / summary` 语义；不再通过正文猜测历史格式或修正字面量换行

### 9.2 Web 创建与编辑链路

相关文件：

- `memora-web-app/src/components/Document/DocumentRichEditor.jsx`
- `memora-web-app/src/components/Document/DocumentActionModal.jsx`
- `memora-web-app/src/pages/Document/DocumentEditorPage.jsx`
- `memora-web-app/src/hooks/useKnowledgeBaseDetailController.js`
- `memora-web-app/src/utils/documentExamples.js`
- `memora-web-app/src/components/Workspace/OpenApiWorkbench.jsx`

当前实现：

- 富文本编辑器显式写入 `RICH_TEXT`
- `MARKDOWN`、`HTML` 使用独立源码编辑器与实时预览
- 新建文档入口已显式选择格式，不再默认把所有新文档写成 `MARKDOWN`
- 新建文档弹窗复用 `documentExamples.js` 提供富文本帖子、Markdown 操作规范、H5 点击小程序三类差异化快速示例；示例只生成首版标题、摘要和源内容，不新增模板或标签模型
- 开放接入调试台复用同一示例事实源填充 Markdown / H5 写入 payload，便于外部 AI 或脚本快速创建可预览文档
- H5 示例按 HTML 源码保存声明式事件规范，由 `DocumentRenderedContent` 渲染安全点击演示；正文仍不保存或执行任意 `script / onclick`
- 保存接口会明确携带 `format`，避免出现“名义格式与实际内容不一致”的新数据

### 9.3 Web 渲染链路

相关文件：

- `memora-web-app/src/utils/documentContent.js`
- `memora-web-app/src/components/KnowledgeBase/KnowledgeBaseDocumentPanel.jsx`
- `memora-web-app/src/pages/Document/DocumentReaderPage.jsx`
- `memora-web-app/src/pages/Share/PublicSharePage.jsx`

当前实现：

- `documentContent.js` 已按 `format` 提供统一渲染与净化能力
- 知识库预览、阅读页、公开分享页共用一条格式渲染链路
- Markdown 已升级为正式渲染能力，不依赖正文猜测
- HTML 文档继续走安全白名单，不引入脚本执行

### 9.4 开放 API 与机器接入

相关文件：

- `memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/OpenApiController.java`
- `memora-server/memora-server-manager/src/main/java/com/memora/manager/dto/OpenApiDocumentCreateDTO.java`
- `memora-server/memora-server-manager/src/main/java/com/memora/manager/dto/OpenApiDocumentUpdateDTO.java`
- `memora-server/memora-server-manager/src/main/java/com/memora/manager/service/OpenApiDocumentService.java`

当前实现：

- Open API 已对输入 `format` 做显式校验
- 机器主体支持整体启停、删除、同主体多 key 和逐知识库 `READ / WRITE` 作用域
- 停用主体期间不签发新 key，恢复后才允许继续新增密钥
- 删除机器主体会级联移除其密钥与作用域，旧 key 立即无效，审计记录继续保留
- 当前 `API key` 只绑定到机器主体，不和用户登录态或个人凭证混用
- 外部 AI 或脚本可直接创建 / 更新 / 搜索 / 删除 / 恢复 / 回滚 Markdown 与 HTML 文档
- Open API 已提供单条 `upsert` 与 `batch-upsert`，并以 `knowledgeBaseId + sourceExternalId` 作为持续同步锚点
- `sourceRevision` 会参与幂等和冲突判定；同 revision 默认跳过，旧 revision 返回冲突，新 revision 才会推进更新
- Open API 已补按 `sourceExternalId` 定位文档和按 `metadata / rendered / source / plain / summary` 协商消费视图，外部工具不必自行推断应读取哪个字段
- `contentText` 不再作为外部权威输入，而是服务端从源内容统一派生
- 主链路与 Open API 的创建 / 更新 DTO 已移除 `contentText` 这类无效兼容字段
- 当前 Open API 仍只暴露文档源内容与元数据，不提供可执行 HTML 应用运行时

### 9.5 搜索、版本与测试护栏

相关文件：

- `memora-web-app/src/utils/documentDiff.js`
- `memora-server/memora-server-start/src/test/java/com/memora/OnlineDocumentApiIntegrationTest.java`
- `memora-server/memora-server-start/src/main/resources/db/schema.sql`
- `memora-server/memora-server-start/src/main/resources/db/schema-mysql.sql`

当前实现：

- 版本 diff 和搜索继续依赖统一派生的 `contentText`
- 自动化测试已把 Markdown / HTML 的创建、读取、分享与 Open API 一致性纳入正式基线
- schema 默认值与 seed 数据已同步到新的格式语义
- 新项目不保留文档内容维护归一化接口；如需一次性导入或修正，应通过独立迁移脚本和专项验收处理
- 搜索页已支持知识库范围过滤、命中高亮和结果相关度排序
- 后端统一搜索与 Open API 搜索已补多关键词相关度排序，不再单纯按更新时间截断结果

后续治理：

- 如需做数据修正，应同时补 schema 迁移说明、回填脚本和专项验收
- 可以继续增强版本 diff 的格式感知与历史渲染稳定性
- 后续仍应引入索引化或专门搜索引擎，以支撑更大规模知识库

### 9.6 发布与接入当前缺口

当前还缺：

- 更成熟的对话框直连保存 / 打开封装
- 更大规模接入下的批量治理与限流策略

下一步改造应遵循：

1. 继续把浏览器 / 对话框入口落到现有 Open API，而不是新开平行写入通道。
2. 公开消费继续复用 `renderedHtml`，不要再引入前端二次渲染事实源。
3. 再补浏览器 / 对话框调用封装、导入治理和批量保护策略。

---

## 10. 演进方向

以下能力属于后续阶段演进方向，不应被表述为当前已交付范围：

优先演进顺序：

1. 浏览器 / 对话框入口接入与调用封装
2. 更细外部文档操作边界与自动化治理
3. 更完整的跨端会话治理与管理员级策略
4. 搜索索引化与更大规模检索质量治理
5. 独立归档服务与更深自动化保留策略
6. 更强的版本 diff 与恢复体验
7. 更稳定的 Web 信息架构与交互分层

---

## 11. 相关文档

- [在线文档系统产品需求与范围](./product-requirements-and-scope.md)
- [在线文档系统重构设计与计划](./refactor-design-and-plan.md)
- [行业产品参考与设计取舍](./industry-product-references-and-trade-offs.md)
