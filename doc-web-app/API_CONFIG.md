# API 配置说明

## 环境变量配置

前端使用环境变量来配置后端API地址：

### 开发环境
创建 `.env.development` 文件（已创建）：
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### 生产环境
创建 `.env.production` 文件（已创建）：
```
VITE_API_BASE_URL=/api/v1
```

## API 接口说明

### 知识库接口
- `GET /knowledge-bases` - 获取知识库列表（分页）
- `GET /knowledge-bases/user/{userId}` - 获取用户的知识库列表（不分页）
- `GET /knowledge-bases/{id}` - 获取知识库详情
- `POST /knowledge-bases` - 创建知识库
- `PUT /knowledge-bases/{id}` - 更新知识库
- `DELETE /knowledge-bases/{id}` - 删除知识库
- `GET /knowledge-bases/{id}/documents` - 获取知识库下的文档列表

### 文档接口
- `GET /documents` - 获取文档列表（分页）
- `GET /documents/knowledge-base/{knowledgeBaseId}` - 获取知识库下的文档列表（不分页）
- `GET /documents/{id}` - 获取文档详情
- `POST /documents` - 创建文档
- `PUT /documents/{id}` - 更新文档
- `DELETE /documents/{id}` - 删除文档

## 数据格式

### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 分页响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "size": 20,
    "current": 1
  }
}
```

## 注意事项

1. **userId 临时处理**: 当前版本鉴权功能未实现，userId 使用固定值 1
2. **跨域配置**: 后端已配置跨域，支持前端访问
3. **错误处理**: 前端已配置统一的错误处理机制

