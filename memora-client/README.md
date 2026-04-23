# Memora Client

`memora-client` 是 Memora 在线文档系统的桌面同步代理壳，技术栈基于：

- Tauri
- Rust
- React

当前它已经从旧的“桌面知识库 UI / 文件管理器”方向收敛为最小同步代理基线，职责只保留：

- 本地文件系统能力承载
- 目录绑定和扫描能力入口
- 后续同步上报与冲突处理的桌面容器

它不再承担：

- 与 Web 管理台重复的知识库浏览
- 文档编辑器 UI
- 独立桌面端产品导航体系

---

## 当前实现

### 前端

当前 React 前端只保留一个最小同步代理壳页面，用于明确：

- 当前产品边界
- 同步链路阶段
- 已暴露的本地命令

对应文件：

- [App.jsx](./src/App.jsx)
- [global.css](./src/styles/global.css)

### Tauri / Rust

当前 Rust 侧已经暴露基础文件操作命令：

- `read_dir`
- `read_file`
- `write_file`
- `create_dir`
- `remove_file`
- `rename_file`

对应文件：

- [main.rs](./src-tauri/src/main.rs)

---

## 当前目录结构

```text
memora-client/
├── src/                  # 最小同步代理壳前端
├── src-tauri/            # 本地文件能力与后续同步能力
├── package.json
├── vite.config.js
└── index.html
```

---

## 当前缺口

当前客户端仍然没有完成真正的同步闭环，仍缺少：

- 本地目录绑定持久化
- 文件指纹和增量变更检测
- 同步任务上报
- 冲突识别和处理
- 同步日志和失败恢复

---

## 运行方式

安装依赖：

```bash
npm install
```

启动开发模式：

```bash
npm run tauri:dev
```

仅启动前端调试页：

```bash
npm run dev -- --port 1420
```

---

## 下一步建议

1. 先补目录绑定和本地扫描结果模型。
2. 再定义客户端与服务端的同步协议。
3. 再实现增量检测和同步上报。
4. 最后补冲突处理和自动同步。

---

## 推荐阅读

1. [根目录 README](../README.md)
2. [在线文档系统重构设计与计划](../doc/架构设计/在线文档系统重构设计与计划.md)
3. [在线文档系统技术架构与接口设计](../doc/架构设计/在线文档系统技术架构与接口设计.md)
4. [main.rs](./src-tauri/src/main.rs)
