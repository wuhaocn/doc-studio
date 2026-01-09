# DocStudio 后端服务

基于 Spring Boot 3.2.0 + JDK 21 + Gradle 构建的文档管理系统后端服务。

## 技术栈

- **JDK**: 21
- **Spring Boot**: 3.2.0
- **构建工具**: Gradle 8.5+
- **数据库**: H2 Database (内存数据库，用于快速开发)
- **ORM**: MyBatis Plus 3.5.5

## 项目结构

```
doc-server/
├── doc-server-common/      # 公共模块（统一响应、异常等）
├── doc-server-manager/     # 管理模块（核心业务逻辑）
└── doc-server-start/       # 启动模块（应用入口）
```

## 快速开始

### 1. 环境要求

- JDK 21+
- Gradle 8.5+（或使用项目自带的 Gradle Wrapper）

**注意**: 项目使用 H2 内存数据库，无需安装和配置外部数据库，启动时会自动创建表结构和初始化数据。

### 2. 运行项目

使用 Gradle Wrapper：
```bash
./gradlew :doc-server-start:bootRun
```

或使用 IDE 直接运行 `DocStudioApplication` 类。

### 3. 访问接口

服务启动后，访问地址：`http://localhost:8080`

**H2 控制台**（可选）：
- 访问：`http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:doc_studio`
- 用户名: `sa`
- 密码: （留空）

### 4. API 接口示例

- 获取知识库列表：`GET http://localhost:8080/api/v1/knowledge-bases`
- 创建知识库：`POST http://localhost:8080/api/v1/knowledge-bases`
- 获取文档列表：`GET http://localhost:8080/api/v1/documents`
- 创建文档：`POST http://localhost:8080/api/v1/documents`

### 5. 数据库说明

- **H2 内存数据库**: 数据存储在内存中，应用重启后数据会丢失
- **自动建表**: 启动时会自动执行 `schema.sql` 创建表结构
- **初始化数据**: 启动时会自动执行 `data.sql` 插入测试数据
- **生产环境**: 如需持久化存储，可切换到 MySQL（修改 `application.yml` 配置）

## 重要说明

### ⚠️ 鉴权功能暂未实现

当前版本**暂不实现用户认证和授权功能**，所有接口可直接访问。

**临时方案**：
- `userId` 字段通过请求参数传递
- 创建知识库和文档时，需要在请求体中包含 `userId` 字段
- 后续版本将实现 JWT 认证，从 Token 中自动获取用户信息

**示例**：
```json
POST /api/v1/knowledge-bases
{
  "name": "我的知识库",
  "description": "描述",
  "userId": 1
}
```

## 已实现功能

- ✅ 知识库 CRUD 操作
- ✅ 文档 CRUD 操作
- ✅ 知识库-文档层级关系管理
- ✅ 统一响应格式
- ✅ 全局异常处理
- ✅ 跨域配置

## 待实现功能

- ❌ 用户认证和授权（JWT）
- ❌ 文档版本管理
- ❌ 知识库和文档搜索
- ❌ 知识库和文档分享
- ❌ 权限管理

## 开发计划

详见 `doc/项目设计文档.md`

