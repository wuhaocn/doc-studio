# Memora 文档索引

本文档定义 `doc/` 目录的唯一事实源结构、阅读顺序和文档边界。

目标：

- 每一类事实只保留一个 canonical 文档
- 产品、架构、计划、交付规范不互相重复
- 基线文档与参考文档清晰分层

---

## 1. Canonical 文档

### 1.1 产品基线

- [在线文档系统产品需求与范围](./architecture/product-requirements-and-scope.md)

负责：

- 当前产品定位
- 当前产品闭环
- 当前产品设计
- 目标产品设计
- 外部 AI 内容资产化边界
- 功能边界

### 1.2 技术基线

- [在线文档系统技术架构与接口设计](./architecture/technical-architecture-and-api-design.md)

负责：

- 当前系统架构
- 模块边界
- 核心数据模型
- 接口分层
- 权限与版本等关键约束

### 1.3 路线与实施计划

- [在线文档系统重构设计与计划](./architecture/refactor-design-and-plan.md)

负责：

- 阶段评估
- 里程碑
- 实施顺序
- 风险与依赖

### 1.4 行业参考

- [行业产品参考与设计取舍](./architecture/industry-product-references-and-trade-offs.md)

说明：

- 这是参考资料，不是当前产品基线
- 只记录外部产品能力、取舍结论与官方来源

### 1.5 开发与上线规范

- [开发规范](./development/development-standard.md)
- [上线规范](./development/release-standard.md)
- [重构任务拆解与里程碑](./development/refactor-task-breakdown-and-milestones.md)
- [主流程冒烟验收清单](./development/online-document-main-flow-smoke-checklist.md)
- [代码命名规范](./development/code-naming-standard.md)

---

## 2. 推荐阅读顺序

1. [在线文档系统产品需求与范围](./architecture/product-requirements-and-scope.md)
2. [在线文档系统技术架构与接口设计](./architecture/technical-architecture-and-api-design.md)
3. [在线文档系统重构设计与计划](./architecture/refactor-design-and-plan.md)
4. [行业产品参考与设计取舍](./architecture/industry-product-references-and-trade-offs.md)
5. [开发规范](./development/development-standard.md)
6. [上线规范](./development/release-standard.md)

---

## 3. 唯一性规则

- 产品事实只在产品文档定义。
- 技术事实只在架构文档定义。
- 阶段计划只在计划文档定义。
- 外部参考只放在行业参考文档。
- 根 README 和模块 README 只做入口摘要，不再承担平行事实源角色。

---

## 4. 当前整理结果

经过本轮整理后：

- 产品、架构、计划、行业参考已经分层清楚
- 重复的分析类文档已经合并或移除
- 旧资源库 / AI / 技能系统叙事不再属于当前活跃基线
- 桌面客户端不再属于当前仓库交付范围
- 后端开发与发布入口已统一收敛到仓库脚本，不再以零散 Gradle 命令作为规范入口

后续当系统行为变化时，应优先更新上面的 canonical 文档，而不是继续新增“总结类”平行文档。
