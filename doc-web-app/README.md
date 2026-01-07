# DocStudio - 在线文档管理系统前端

## 项目简介

DocStudio 是一个类似飞书风格的在线文档管理系统前端应用，支持知识库和文档的管理与编辑。

## 技术栈

- **React 18**: UI框架
- **React Router v6**: 路由管理
- **Arco Design**: UI组件库
- **Tiptap**: 富文本编辑器
- **Vite**: 构建工具

## 项目结构

```
doc-web-app/
├── src/
│   ├── components/          # 通用组件
│   │   └── Layout/          # 布局组件
│   ├── pages/               # 页面组件
│   │   ├── Home/           # 首页（知识库列表）
│   │   ├── KnowledgeBase/  # 知识库详情页
│   │   ├── Document/       # 文档编辑页
│   │   └── NotFound/        # 404页面
│   ├── services/           # 服务层
│   │   ├── api/           # API服务
│   │   └── mock/          # Mock数据
│   ├── styles/            # 全局样式
│   ├── router/            # 路由配置
│   ├── App.jsx           # 根组件
│   └── main.jsx          # 入口文件
```

## 开发

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## 功能说明

### 已实现功能

- ✅ 知识库列表页（首页）
- ✅ 知识库详情页（文档列表）
- ✅ 文档编辑页（使用Tiptap编辑器）
- ✅ Mock数据服务
- ✅ 基础布局（Header + Sidebar）

### 待实现功能

- ⏳ 登录注册功能
- ⏳ 知识库创建/编辑
- ⏳ 文档删除
- ⏳ 搜索功能
- ⏳ 文档分享
- ⏳ 更多功能...

## 注意事项

当前版本使用Mock数据，所有数据存储在内存中，刷新页面后会重置。

