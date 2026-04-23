# Memora

Memora 当前正在重构为一个面向企业场景的在线文档系统，主线能力聚焦在：

- 多租户工作区
- 知识库管理
- 文档树管理
- 文档版本管理
- 本地文件同步
- Web 管理台

当前仓库不再把“资源库 / AI / 技能系统”作为主入口方向，这些能力后续是否接回，取决于在线文档系统主链路是否稳定。

---

## 当前状态

当前已经完成的主改造：

- 后端从单用户知识库 demo 重构为多租户基础模型
- 增加工作区、成员、知识库、文档树、版本、同步任务等核心模型
- 已补基于请求头上下文的最小租户隔离
- 已补基于 bearer token 的统一会话占位
- 已补租户成员角色控制和知识库级权限覆盖
- 已补知识库成员权限配置 API 与前端最小权限感知
- 已补 demo 登录页、登录接口和前端会话上下文分发
- Web 端重构为“工作台 + 知识库详情”双主视图
- 知识库支持本地目录绑定与同步任务记录

当前仍未完成：

- 真实认证和完整权限体系
- 知识库权限配置体验增强与生产化
- 高级在线编辑能力与协作体验
- Tauri 客户端真实本地扫描与增量同步
- 全量构建环境修复

当前判断：

- 已满足继续收口在线文档主链路
- 尚不满足继续扩展外围大模块

---

## 核心目标

本阶段目标不是做一个大而全的平台，而是先把下面这条主链路做扎实：

`租户 -> 知识库 -> 文档树 -> 编辑 -> 版本 -> 同步 -> 权限`

只有这条主链路稳定后，资源库、协作、AI、技能化能力才值得重新接入。

---

## 仓库结构

```text
memora-doc/
├── memora-server/        # Spring Boot 后端
├── memora-web-app/       # React Web 管理台
├── memora-client/        # Tauri 同步代理壳
├── doc/                  # 设计文档与开发文档
├── gradle/               # Gradle Wrapper
├── settings.gradle.kts
└── README.md
```

### 目录说明

#### `memora-server`

后端服务，当前核心模块包括：

- 工作区工作台聚合
- 知识库管理
- 文档树管理
- 文档版本管理
- 同步任务管理

关键模块：

- `memora-server-common`
- `memora-server-manager`
- `memora-server-start`

#### `memora-web-app`

当前 Web 端承担管理台角色，核心页面包括：

- 工作台首页
- 知识库详情页
- 文档树预览
- 同步任务展示

#### `memora-client`

当前已经收敛为最小同步代理壳，后续重点是：

- 本地目录扫描
- 文件变更检测
- 同步任务上报
- 冲突处理辅助

模块说明：

- [memora-client/README.md](./memora-client/README.md)

#### `doc`

当前最重要的设计文档都在这里，建议从下面三份开始读。

---

## 文档入口

### 总览文档

- [在线文档系统重构设计与计划](./doc/架构设计/在线文档系统重构设计与计划.md)

适合先了解：

- 当前目标
- 当前状态
- 当前问题
- 后续计划

### 产品文档

- [在线文档系统产品需求与范围](./doc/架构设计/在线文档系统产品需求与范围.md)

适合先了解：

- 做什么
- 不做什么
- 目标用户
- 核心流程
- 功能边界

### 技术文档

- [在线文档系统技术架构与接口设计](./doc/架构设计/在线文档系统技术架构与接口设计.md)
- [在线文档系统轻量化收敛评估](./doc/架构设计/在线文档系统轻量化收敛评估.md)
- [开源在线文档方案借鉴分析](./doc/架构设计/开源在线文档方案借鉴分析.md)
- [Memora与Outline_Docmost_AppFlowy对照与取舍](./doc/架构设计/Memora与Outline_Docmost_AppFlowy对照与取舍.md)

### 开发任务

- [重构任务拆解与里程碑](./doc/开发管理/重构任务拆解与里程碑.md)
- [在线文档系统主流程冒烟验收清单](./doc/开发管理/在线文档系统主流程冒烟验收清单.md)

适合先了解：

- 当前技术栈
- 核心模块边界
- 数据模型
- 关键接口
- 技术演进路线
- 当前人工验收主流程

---

## 当前功能列表

### 已纳入当前交付范围

- 多租户工作区模型
- 工作区工作台
- 租户成员列表
- demo 登录与当前会话占位接口
- 知识库列表与详情
- 知识库成员权限配置 API
- 知识库权限配置界面
- 文档树展示与维护
- 文档在线编辑
- 文档版本快照与回滚
- 同步任务记录
- 手动触发同步

### 当前成熟度判断

- 已成型能力：多租户基础边界、多知识库、文档树、基础编辑、版本能力
- 已建立最小闭环但未产品化：工作台、知识库管理、最小权限控制、知识库级权限覆盖、知识库权限配置界面
- 仅有骨架未闭环：真实同步、生产级认证、审计、搜索、发布

### 当前不作为主交付目标

- 资源库主流程
- AI 问答主流程
- 技能市场
- 会议 / 任务协作
- 实时协同编辑

---

## 快速查看关键实现

### 后端

- [WorkspaceController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/WorkspaceController.java)
- [AuthController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/AuthController.java)
- [KnowledgeBaseController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/KnowledgeBaseController.java)
- [DocumentController.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/DocumentController.java)
- [WorkspaceService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/WorkspaceService.java)
- [KnowledgeBaseService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/KnowledgeBaseService.java)
- [DocumentService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/DocumentService.java)
- [SyncJobService.java](./memora-server/memora-server-manager/src/main/java/com/memora/manager/service/SyncJobService.java)
- [schema.sql](./memora-server/memora-server-start/src/main/resources/db/schema.sql)
- [data.sql](./memora-server/memora-server-start/src/main/resources/db/data.sql)

### Web 端

- [router/index.jsx](./memora-web-app/src/router/index.jsx)
- [AuthContext.jsx](./memora-web-app/src/contexts/AuthContext.jsx)
- [Layout.jsx](./memora-web-app/src/components/Layout/Layout.jsx)
- [Home.jsx](./memora-web-app/src/pages/Home/Home.jsx)
- [KnowledgeBaseDetail.jsx](./memora-web-app/src/pages/KnowledgeBase/KnowledgeBaseDetail.jsx)
- [knowledgeBaseApi.js](./memora-web-app/src/services/api/knowledgeBaseApi.js)
- [workspaceApi.js](./memora-web-app/src/services/api/workspaceApi.js)

---

## 运行说明

### 后端

理论入口：

```bash
./gradlew :memora-server-start:bootRun
```

当前已知问题：

- 当前环境下 `gradlew` 可能因网络限制无法下载 wrapper 依赖
- 仓库自带 `gradle-8.5` 在部分本机环境存在 native 兼容问题

### Web 端

```bash
cd memora-web-app
npm install
npm run dev
```

当前已知问题：

- 当前机器上 Web 端 `npm run lint` 和 `npm run build` 已通过
- 如果跨架构复用已有前端依赖，仍可能需要重新执行 `npm install`

---

## 当前已知问题

- 当前已从固定请求头上下文升级为 bearer token 会话占位，但真实认证和租户解析尚未接入
- 当前权限只完成最小租户隔离、租户级角色控制、知识库级权限覆盖和最小权限配置界面，未完成完整权限体系
- 编辑能力已形成基础闭环，但未达到高阶协作编辑水平
- 同步能力目前还是任务流雏形，不是真实文件同步
- Mermaid 图表能力仍是当前前端最重依赖之一，但已通过懒加载从主入口首屏拆出

---

## 下一步计划

### P0

- 接入真实用户上下文和租户上下文
- 补主链路和权限最小测试
- 清理旧入口和历史噪音模块
- 继续评估 Mermaid 是否需要进一步降级或替换

### P1

- 完善知识库创建 / 编辑 / 删除闭环
- 增强知识库权限配置体验
- 完善文档树新增 / 重命名 / 移动 / 排序闭环
- 增强现有编辑器和版本 diff 体验

### P2

- 在当前 `memora-client` 最小壳上补目录绑定和增量同步
- 实现本地扫描和增量上报
- 增加冲突识别和恢复机制
- 实现文件映射和冲突识别

### P3

- 推进权限生产化与审计能力
- 接入搜索
- 接入分享、发布和审计

---

## 当前结论

Memora 现在最重要的不是继续扩模块，而是先把“在线文档系统”做成一个稳定的基础平台。

如果你要继续推进开发，建议按下面顺序进入：

1. 先看 [在线文档系统重构设计与计划](./doc/架构设计/在线文档系统重构设计与计划.md)
2. 再看 [在线文档系统产品需求与范围](./doc/架构设计/在线文档系统产品需求与范围.md)
3. 最后看 [在线文档系统技术架构与接口设计](./doc/架构设计/在线文档系统技术架构与接口设计.md)
