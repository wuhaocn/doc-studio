# 业界 Skill 使用及配置指南

## 1. 概述

业界 Skills（如 TRAE/Claude Skills）是 AI 编程工具的扩展能力，用于标准化开发流程，提升开发效率。本文档将详细介绍如何在主流 AI 编程工具中配置和使用 Skills，包括 TRAE、Cursor、Claude、QCode 和 Anthropics 等，并提供符合官方最佳实践的技能开发规范。

## 2. 核心概念

### 2.1 Skills 定义

Skills 是 AI 编程工具的扩展能力，允许开发者通过安装和配置外部技能包，扩展 AI 工具的功能，标准化开发流程，提升开发效率。Skills 通常以模块化方式设计，专注于特定功能领域。

### 2.2 OpenSkills

OpenSkills 是一个开源项目，用于在非 Claude Code 项目中使用 Skills。它允许开发者在各种 AI 编程工具中安装和使用官方或第三方 Skills，提供了跨平台的 Skills 支持。

### 2.3 AGENTS.md 文件

AGENTS.md 是 AI 编程工具使用 Skills 的指导文件，包含了项目中可用的 Skills 定义和说明。该文件是 AI 工具识别和使用 Skills 的核心配置文件。

### 2.4 Cursor 特有技能目录

Cursor 支持从多个位置自动加载技能：

| 位置 | 作用域 |
| :--- | :--- |
| `.cursor/skills/` | 项目级 |
| `.claude/skills/` | 项目级（兼容 Claude） |
| `.codex/skills/` | 项目级（兼容 Codex） |
| `~/.cursor/skills/` | 用户级（全局） |
| `~/.claude/skills/` | 用户级（全局，兼容 Claude） |
| `~/.codex/skills/` | 用户级（全局，兼容 Codex） |

每个技能应为一个包含 `SKILL.md` 文件的文件夹。

## 3. 配置方式

### 3.1 安装 OpenSkills

OpenSkills 是在各种 AI 编程工具中使用 Skills 的基础，首先需要安装：

```bash
npm i -g openskills
```

### 3.2 安装官方 Skills

安装 Anthropic 官方 Skills，可以选择安装到当前项目或全局：

**安装到当前项目**：
```bash
openskills install anthropics/skills
```

**全局安装（推荐）**：
```bash
openskills install anthropics/skills --global
```

安装成功后，会在项目中生成 `.claude/skills` 文件夹，包含官方技能的实现和配置。

### 3.3 创建 AGENTS.md 文件并同步 Skills

在项目根目录创建 AGENTS.md 文件，然后运行：

```bash
openskills sync
```

选择要写入 AGENTS.md 的 Skills，完成后该文件将作为 AI 编程工具使用 Skills 的指导文件。

### 3.4 安装第三方 Skills

除了官方 Skills，还可以安装第三方 Skills：

```bash
openskills install your-org/custom-skills
```

## 4. 在不同 AI 工具中使用 Skills

### 4.1 TRAE

TRAE 是字节跳动推出的 AI 原生 IDE，支持 Skills 功能：

#### 4.1.1 配置步骤

1. 确保已安装 OpenSkills 和官方 Skills
2. 在项目根目录创建 AGENTS.md 文件并同步 Skills
3. 打开 TRAE IDE，即可自动识别 AGENTS.md 中的 Skills

#### 4.1.2 使用方法

**自动调用**：
```
开发一个视频剪辑软件的SaaS介绍页
```
TRAE 会自动调用 frontend-design skills，生成符合要求的 HTML、CSS 代码。

**手动调用**：
```
调用 frontend-design skills，用HTML开发一个视频剪辑软件的SaaS介绍页
```

### 4.2 Cursor

Cursor 是基于 VS Code 的 AI 编程工具，支持 Skills 功能：

#### 4.2.1 配置步骤

1. 确保已安装 OpenSkills 和官方 Skills
2. 在项目根目录创建 AGENTS.md 文件并同步 Skills
3. 打开 Cursor，它会自动识别 AGENTS.md 中的 Skills

#### 4.2.2 使用方法

**自动调用**：
```
创建一个 React 组件，实现用户登录功能
```
Cursor 会自动调用相关 Skills，生成符合要求的 React 组件。

**手动调用**：
```
调用 react-component skills，创建一个用户登录组件
```

**使用斜杠命令调用**：
```
/skill-name 参数1 参数2
```

#### 4.2.3 查看已安装技能

要查看已发现的技能：

1. 打开 Cursor Settings（Mac 上为 Cmd+Shift+J，Windows/Linux 上为 Ctrl+Shift+J）
2. 前往 Rules
3. 技能会显示在 Agent Decides 部分中

#### 4.2.4 从 GitHub 安装技能

可以从 GitHub 仓库导入技能：

1. 打开 Cursor Settings → Rules
2. 在 Project Rules 部分，点击 Add Rule
3. 选择 Remote Rule (Github)
4. 输入 GitHub 仓库的 URL

#### 4.2.5 将规则和命令迁移到技能

Cursor 在 2.4 中内置了一个 `/migrate-to-skills` 技能，帮助你将现有的动态规则和斜杠命令转换为技能。

该迁移技能会转换：

- **Dynamic rules**：使用 "Apply Intelligently" 配置的规则——即 alwaysApply: false（或未定义）且未定义 globs 模式的规则。这些会被转换为标准技能。
- **Slash commands**：用户级和工作区级命令都会被转换为带有 disable-model-invocation: true 的技能，从而保留其显式调用行为。

迁移步骤：

1. 在 Agent 聊天中输入 `/migrate-to-skills`
2. Agent 会识别符合条件的规则和命令并将其转换为技能
3. 在 `.cursor/skills/` 中查看生成的技能

### 4.3 Claude

Claude 是 Anthropic 推出的 AI 助手，支持 Skills 功能：

#### 4.3.1 配置步骤

1. 访问 Claude 控制台
2. 进入 Skills 管理页面
3. 启用或安装所需的 Skills
4. 在 Claude Code 项目中自动可用

#### 4.3.2 使用方法

**自动调用**：
```
编写一个 Python 脚本，实现文件批量重命名功能
```
Claude 会自动调用相关 Skills，生成符合要求的 Python 脚本。

**手动调用**：
```
调用 python-script skills，编写一个文件批量重命名脚本
```

### 4.4 QCode

QCode 是一款 AI 编程工具，支持 Skills 功能：

#### 4.4.1 配置步骤

1. 确保已安装 OpenSkills 和官方 Skills
2. 在项目根目录创建 AGENTS.md 文件并同步 Skills
3. 打开 QCode，它会自动识别 AGENTS.md 中的 Skills

#### 4.4.2 使用方法

**自动调用**：
```
创建一个 RESTful API，实现用户管理功能
```
QCode 会自动调用相关 Skills，生成符合要求的 API 代码。

**手动调用**：
```
调用 rest-api skills，创建一个用户管理 API
```

### 4.5 Anthropics

Anthropic 是 Claude 的开发者，提供了官方 Skills 支持：

#### 4.5.1 配置步骤

1. 访问 Anthropics 开发者控制台
2. 进入 Skills 管理页面
3. 创建或启用所需的 Skills
4. 在 Anthropics 平台上使用

#### 4.5.2 使用方法

**自动调用**：
```
分析这个代码库的架构，并生成架构文档
```
Anthropic 会自动调用相关 Skills，生成符合要求的架构文档。

**手动调用**：
```
调用 code-analysis skills，分析这个代码库的架构
```

## 4. 技能开发规范（官方最佳实践）

### 4.1 技能设计原则

根据 Anthropics 官方最佳实践，技能开发应遵循以下原则：

1. **单一职责原则**：每个 Skill 应专注于一个特定功能领域，避免功能过于复杂
2. **模块化设计**：技能应采用模块化结构，便于维护和扩展
3. **清晰的输入输出**：明确定义技能的输入参数和输出格式
4. **良好的错误处理**：实现完善的错误处理机制，提供有用的错误信息
5. **可测试性**：技能应易于测试，便于验证功能正确性
6. **文档完整性**：提供详细的使用文档和示例

### 4.2 技能目录结构

官方推荐的技能目录结构如下：

```
skill-name/
├── SKILL.md              # 技能元数据和使用说明
├── manifest.json          # 技能清单文件（可选）
├── src/
│   ├── index.js          # 技能入口文件
│   └── utils.js          # 辅助工具函数
├── scripts/              # Agents 可以运行的可执行代码
├── references/           # 按需加载的附加文档
├── assets/               # 模板、图片或数据文件等静态资源
├── examples/             # 使用示例
│   └── example1.md
└── tests/                # 测试用例
    └── test1.js
```

#### 4.2.1 可选目录说明

*   **scripts/**：包含可由代理运行的可执行代码。在 SKILL.md 文件中使用相对于技能根目录的相对路径引用这些脚本。
*   **references/**：包含按需加载的附加文档，用于存放详细参考资料，避免主 SKILL.md 文件过于冗长。
*   **assets/**：包含模板、图片或数据文件等静态资源，用于技能运行时使用。

这些可选目录可以让主 SKILL.md 文件保持简洁，更高效地利用上下文，因为 Agents 会按需逐步加载资源——只在需要时才加载。

### 4.3 SKILL.md 规范

SKILL.md 是技能的核心文档，包含 YAML 前置信息（frontmatter）和技能说明。

#### 4.3.1 YAML 前置信息

YAML 前置信息是 SKILL.md 文件的头部，包含技能的元数据：

```yaml
---
name: skill-name
# 技能标识符。仅限小写字母、数字和连字符。必须与父文件夹名称一致。
description: "技能描述，说明技能的作用及其使用场景。"
# 许可证名称或对随附许可证文件的引用
license: MIT
# 运行环境要求（系统软件包、网络访问等）
compatibility: "需要 Node.js 16+"
# 用于额外元数据的任意键值映射
metadata:
  author: "Your Name"
  version: "1.0.0"
# 当为 true 时，该技能仅会在通过 /skill-name 显式调用时才会被使用
# 代理不会基于上下文自动调用它
# disable-model-invocation: true
---
```

#### 4.3.2 技能说明内容

YAML 前置信息之后，SKILL.md 应包含以下内容：

```markdown
# 技能名称

## Description
简短描述技能的功能和用途

## Usage
技能的使用方式

## Parameters
技能接受的参数列表

## Example
技能使用示例

## Output
技能的输出格式

## Scope
技能的适用范围
```

### 4.4 manifest.json 规范

manifest.json 是技能的清单文件，包含技能的元数据：

```json
{
  "name": "skill-name",
  "version": "1.0.0",
  "description": "技能描述",
  "author": "作者信息",
  "license": "许可证",
  "main": "src/index.js",
  "dependencies": {},
  "keywords": [],
  "anthropic": {
    "skills": {
      "skill-name": {
        "type": "tool",
        "description": "技能描述",
        "inputSchema": {},
        "outputSchema": {}
      }
    }
  }
}
```

### 4.5 技能开发示例

以下是一个符合官方最佳实践的技能开发示例：

**SKILL.md**：
```markdown
---
name: code-analysis
description: "分析代码库架构并生成架构文档"
license: MIT
compatibility: "需要 Node.js 16+"
metadata:
  author: "Your Name"
  version: "1.0.0"
disable-model-invocation: false
---

# code-analysis

## Description
分析代码库架构并生成架构文档

## Usage
```
调用 code-analysis skills，分析这个代码库的架构
```

## Parameters
- `codebase_path`: 代码库路径
- `analysis_type`: 分析类型（component, dependency, workflow）

## Example
```
调用 code-analysis skills，分析 ./my-project 目录，分析类型为 component
```

## Output
结构化的架构分析文档，包括组件关系、依赖图和工作流

## Scope
代码库架构分析
```

**src/index.js**：
```javascript
const fs = require('fs');
const path = require('path');

module.exports = {
  async execute({ codebase_path, analysis_type }) {
    // 验证参数
    if (!codebase_path) {
      throw new Error('codebase_path is required');
    }

    if (!analysis_type) {
      analysis_type = 'component';
    }

    // 检查目录是否存在
    if (!fs.existsSync(codebase_path)) {
      throw new Error(`Directory ${codebase_path} does not exist`);
    }

    // 执行分析逻辑
    let result;
    switch (analysis_type) {
      case 'component':
        result = await this.analyzeComponents(codebase_path);
        break;
      case 'dependency':
        result = await this.analyzeDependencies(codebase_path);
        break;
      case 'workflow':
        result = await this.analyzeWorkflow(codebase_path);
        break;
      default:
        throw new Error(`Invalid analysis_type: ${analysis_type}`);
    }

    return {
      success: true,
      data: result,
      message: `Successfully analyzed ${codebase_path} with type ${analysis_type}`
    };
  },

  async analyzeComponents(codebase_path) {
    // 组件分析逻辑
    return {
      type: 'component',
      components: [],
      relationships: []
    };
  },

  async analyzeDependencies(codebase_path) {
    // 依赖分析逻辑
    return {
      type: 'dependency',
      dependencies: [],
      dependencyGraph: {}
    };
  },

  async analyzeWorkflow(codebase_path) {
    // 工作流分析逻辑
    return {
      type: 'workflow',
      workflows: [],
      flowcharts: {}
    };
  }
};
```

**manifest.json**：
```json
{
  "name": "code-analysis",
  "version": "1.0.0",
  "description": "Codebase architecture analysis skill",
  "author": "Your Name",
  "license": "MIT",
  "main": "src/index.js",
  "dependencies": {},
  "keywords": ["code", "analysis", "architecture"],
  "anthropic": {
    "skills": {
      "code-analysis": {
        "type": "tool",
        "description": "Analyze codebase architecture and generate documentation",
        "inputSchema": {
          "type": "object",
          "properties": {
            "codebase_path": {
              "type": "string",
              "description": "Path to the codebase"
            },
            "analysis_type": {
              "type": "string",
              "enum": ["component", "dependency", "workflow"],
              "description": "Type of analysis to perform"
            }
          },
          "required": ["codebase_path"]
        },
        "outputSchema": {
          "type": "object",
          "properties": {
            "success": {
              "type": "boolean"
            },
            "data": {
              "type": "object"
            },
            "message": {
              "type": "string"
            }
          }
        }
      }
    }
  }
}
```

## 5. 开发过程中使用 Skills 的最佳实践

### 5.1 安装与配置

- **全局优先**：优先使用全局安装，便于在多个项目中共享 Skills
- **定期更新**：定期更新 Skills 到最新版本，获取新功能和 bug 修复
- **按需安装**：只安装和启用必要的 Skills，避免资源浪费
- **项目定制**：为不同项目创建不同的 AGENTS.md 文件，定制所需 Skills
- **版本控制**：将 AGENTS.md 文件纳入版本控制，便于团队协作

### 5.2 使用技巧

- **明确指定 Skills**：在提示词中明确指定要调用的 Skills，提高 AI 执行的准确性
- **提供详细需求**：提供详细的需求描述，帮助 Skills 更好地理解任务
- **组合使用 Skills**：结合多个 Skills 完成复杂任务，发挥组合优势
- **迭代优化**：利用 Skills 生成的代码作为起点，进行进一步优化
- **学习官方示例**：研究官方 Skills 的实现，学习最佳实践
- **测试验证**：对 Skills 生成的结果进行测试和验证，确保质量

### 5.3 开发流程优化

根据官方最佳实践，推荐的 Skills 辅助开发流程如下：

1. **需求分析**：使用 Skills 辅助分析需求，生成需求文档
2. **架构设计**：使用 Skills 生成架构设计和技术方案
3. **代码生成**：使用 Skills 生成基础代码框架
4. **代码优化**：手动优化和完善生成的代码
5. **测试生成**：使用 Skills 生成测试用例
6. **文档生成**：使用 Skills 生成项目文档
7. **持续集成**：将 Skills 集成到 CI/CD 流程中，自动化重复任务

## 6. 创建自定义 Skills（官方最佳实践）

### 6.1 技能创建流程

根据 Anthropics 官方文档，创建 Skills 的推荐流程如下：

1. **问题定义**：明确技能要解决的问题和目标用户
2. **设计阶段**：
   - 定义技能的输入和输出
   - 设计技能的工作流程
   - 创建技能的架构设计
3. **开发阶段**：
   - 按照官方目录结构创建技能
   - 实现技能核心逻辑
   - 编写测试用例
   - 创建详细文档
4. **测试阶段**：
   - 单元测试
   - 集成测试
   - 用户测试
5. **发布阶段**：
   - 版本标记
   - 发布到 Git 仓库
   - 更新文档
6. **维护阶段**：
   - 收集用户反馈
   - 修复 bug
   - 添加新功能

### 6.2 技能开发最佳实践

1. **功能单一性**：每个 Skill 专注于一个特定功能领域
2. **输入输出明确**：清晰定义技能的输入参数和输出格式
3. **错误处理完善**：实现全面的错误处理，提供有用的错误信息
4. **文档详细**：为每个 Skill 提供清晰的使用说明和示例
5. **测试充分**：编写全面的测试用例，确保技能质量
6. **性能优化**：优化技能的执行性能，减少响应时间
7. **安全性**：确保技能不会引入安全漏洞
8. **可扩展性**：设计技能时考虑未来扩展需求
9. **兼容性**：确保技能在不同环境下都能正常工作
10. **遵循规范**：严格遵循官方技能开发规范

### 6.3 技能测试最佳实践

- **单元测试**：测试技能的各个组件和功能
- **集成测试**：测试技能与 AI 工具的集成
- **端到端测试**：测试完整的技能使用流程
- **边界测试**：测试技能在边界条件下的表现
- **性能测试**：测试技能的执行性能

## 7. 常见问题与解决方案

### 7.1 Skills 不被识别

- 检查是否已安装 OpenSkills
- 检查是否已创建 AGENTS.md 文件
- 检查 AGENTS.md 文件是否包含正确的 Skills 定义
- 重启 AI 编程工具
- 检查技能配置是否符合官方规范

### 7.2 Skills 调用失败

- 检查网络连接是否正常
- 检查 Skill 配置是否正确
- 查看 AI 工具的日志，获取详细错误信息
- 尝试更新 Skills 到最新版本
- 检查技能实现是否存在 bug

### 7.3 生成的代码不符合预期

- 提供更详细的需求描述
- 明确指定要调用的 Skills
- 调整提示词的语气和结构
- 手动优化生成的代码
- 考虑使用多个 Skills 组合完成任务
- 检查技能配置是否正确

## 8. 技能生态系统

### 8.1 官方 Skills 库

- **Anthropic Skills**：https://github.com/anthropics/skills
- **OpenSkills Hub**：https://github.com/numman-ali/openskills

### 8.2 第三方 Skills 资源

- **Skills Directory**：https://skills.anthropic.com/
- **GitHub Skills Topics**：https://github.com/topics/claude-skills
- **Community Skills**：https://community.anthropic.com/

### 8.3 Doc Studio 项目技能

Doc Studio 项目配置了专属的 AI 技能，用于辅助项目开发、文档生成和架构设计。以下是项目中可用的技能：

#### 8.3.1 code-reflection

**技能名称**：code-reflection
**描述**：分析代码质量，识别架构问题，提出改进建议，支持高内聚、低耦合的设计原则
**版本**：1.0.0
**作者**：DocStudio Team

**使用方法**：
```
调用 code-reflection 技能，分析这段代码的质量并提出改进建议
```

**参数**：
- `code_path`：代码文件路径或目录路径
- `analysis_type`：分析类型（architecture, performance, security, readability）
- `focus_areas`：重点关注领域（cohesion, coupling, naming, structure）

**使用示例**：
```
调用 code-reflection 技能，分析 ./memora-server/memora-server-manager/src/main/java/com/memora/manager/controller/KnowledgeBaseController.java 文件，分析类型为 architecture，重点关注 cohesion 和 coupling
```

#### 8.3.2 self-assessment

**技能名称**：self-assessment
**描述**：评估系统整体状态和性能，生成全面的评估报告
**版本**：1.0.0
**作者**：DocStudio Team

**使用方法**：
```
调用 self-assessment 技能，生成系统评估报告
```

**参数**：
- `assessment_type`：评估类型（code_quality, performance, business, comprehensive）
- `target_areas`：评估目标领域（backend, frontend, database, api）
- `time_range`：评估时间范围（day, week, month, quarter）

**使用示例**：
```
调用 self-assessment 技能，生成 comprehensive 类型的评估报告，评估目标领域包括 backend 和 frontend，时间范围为 week
```

#### 8.3.3 iteration-optimization

**技能名称**：iteration-optimization
**描述**：支持迭代优化流程，基于自我评估结果生成具体的优化建议
**版本**：1.0.0
**作者**：DocStudio Team

**使用方法**：
```
调用 iteration-optimization 技能，基于评估结果生成优化建议
```

**参数**：
- `assessment_result`：自我评估结果（可从self-assessment技能获取）
- `iteration_goals`：迭代目标
- `priority_levels`：优先级设置（high, medium, low）
- `timebox`：时间限制（天）

**使用示例**：
```
调用 iteration-optimization 技能，基于之前的评估结果，设置迭代目标为提高代码质量和性能，优先级为high和medium，时间限制为14天
```

## 9. 总结

业界 Skills 是 AI 编程工具的强大扩展能力，可以大幅提升开发效率和代码质量。通过遵循官方最佳实践配置和使用 Skills，可以标准化开发流程，加快开发速度，提高代码质量。

本文档详细介绍了在 TRAE、Cursor、Claude、QCode 和 Anthropics 等主流 AI 编程工具中配置和使用 Skills 的方法，以及符合官方最佳实践的技能开发规范。同时，也介绍了 Doc Studio 项目专属的 AI 技能，希望能帮助开发者更好地利用 Skills 提升开发效率，创建高质量的自定义技能。

## 10. 参考文档

- [OpenSkills 官方文档](https://github.com/numman-ali/openskills)
- [Anthropic Skills 官方文档](https://docs.anthropic.com/claude/docs/skills)
- [Anthropic Skills 开发指南](https://docs.anthropic.com/claude/docs/developing-skills)
- [TRAE Skills 使用指南](https://aicoding.csdn.net/697047d37c1d88441d8e6db3.html)
- [Claude Skills 最佳实践](https://docs.anthropic.com/claude/docs/skill-best-practices)
- [Doc Studio 项目技能配置](https://github.com/doc-studio/doc-studio/blob/main/AGENTS.md)
