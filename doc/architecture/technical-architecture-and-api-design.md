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
- 提供工作区切换后的新 session
- 提供工作区邀请与接受邀请
- 解析当前用户与租户上下文
- 为后续更强认证和更完整会话治理保留清晰替换点

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
- 提供工作区统一搜索 V1 结果
- 创建版本快照
- 支持批量移动、批量删除、排序
- 保留删除元数据与恢复语义

关键文件：

- [DocumentController.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/DocumentController.java)
- [DocumentService.java](../../memora-server/memora-server-manager/src/main/java/com/memora/manager/service/DocumentService.java)

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
- `path`
- `depth`
- `sortOrder`
- `versionNo`
- `deletedAt`
- `deletedBy`

当前规则：

- `docType` 包含 `DOC` 与 `FOLDER`
- 树结构变更必须保持 `parentId`、`path`、`depth` 一致
- 删除、移动、排序都必须校验树一致性
- 软删除必须保留恢复所需元数据
- 恢复时必须校验所属知识库和父级目录状态

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

---

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

### 8.5 富文本约束

- 不可信 HTML 不能直接按生产能力渲染
- 展示能力与安全能力必须分开表述

---

## 9. 演进方向

以下能力属于后续阶段演进方向，不应被表述为当前已交付范围：

优先演进顺序：

1. 更完整的会话治理与跨端一致性
2. 搜索质量、排序策略与索引化
3. 独立归档服务与自动化保留策略
4. 更强的版本 diff 与恢复体验
5. 受控阅读与发布模型
6. 更细 API key 作用域与外部文档操作边界
7. 更稳定的 Web 信息架构与交互分层

---

## 10. 相关文档

- [在线文档系统产品需求与范围](./product-requirements-and-scope.md)
- [在线文档系统重构设计与计划](./refactor-design-and-plan.md)
- [行业产品参考与设计取舍](./industry-product-references-and-trade-offs.md)
