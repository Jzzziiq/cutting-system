# 切割系统 — 可用 Skills 清单

> 生成日期：2026-05-14 | 共 **50+** 个可用 Skill

---

## 一、本项目专用（切割系统）

| Skill | 来源 | 用途 | 触发场景 |
|---|---|---|---|
| **cutting-system-backend** | 项目 | Spring Boot 后端开发规范（Controller/Service/MyBatis/DTO/VO） | 修改 Java 后端代码 |
| **cutting-system-algorithm** | 项目 | 排版算法工作流（TabuSearch/Skyline/ReadDataUtil） | 修改算法或排版逻辑 |
| **cutting-system-api-contract** | 项目 | API 契约规范（路径/字段/鉴权/Result 包装） | 新增或修改 API 接口 |
| **java-springboot** | 项目 | Spring Boot + MyBatis-Plus + Maven 最佳实践 | 后端编码、测试、配置 |
| **element-plus-vue3** | 项目 | Element Plus Vue 3 组件库指南 | 使用 Element Plus 组件 |
| **vue** | 项目 | Vue 3 Composition API，`<script setup>` 语法 | 编写 .vue 文件 |
| **vue-best-practices** | 项目 | Vue.js 最佳实践，强制 Composition API + TypeScript | Vue 相关任务 |
| **vue-router-best-practices** | 项目 | Vue Router 4 路由守卫/参数/生命周期 | 修改路由配置 |
| **vue-testing-best-practices** | 项目 | Vue 组件测试最佳实践 | 编写 Vue 测试 |
| **vueuse-functions** | 项目 | VueUse 组合式函数集 | 需要工具函数（useStorage 等） |
| **vite** | 项目 | Vite 构建工具配置 | 修改 vite.config.ts |
| **wechat-miniprogram-skill** | 项目 | 微信小程序原生开发指南 | 修改小程序代码 |

---

## 二、通用工程流程（所有项目适用）

### Superpowers 系列（14 个）

| Skill | 用途 | 本项目适用场景 |
|---|---|---|
| **using-superpowers** | 会话启动时加载技能索引 | 每次新会话开始时 |
| **brainstorming** | 需求澄清、设计探索（写代码前必用） | 接到新需求时 |
| **writing-plans** | 多步骤任务的实现计划编写 | 复杂功能开发前 |
| **executing-plans** | 按计划分阶段执行，设检查点 | 执行已有计划 |
| **subagent-driven-development** | 当前会话中并行执行独立子任务 | 多个独立改动同时进行 |
| **dispatching-parallel-agents** | 跨会话分发独立任务 | 大规模并行工作 |
| **test-driven-development** | TDD 流程（先写测试再写实现） | 修 bug 或加功能时 |
| **systematic-debugging** | 系统化调试（遇 bug 先分析后修复） | 遇到任何 bug |
| **verification-before-completion** | 完成前验证（先跑测试再声称完成） | 提交代码前 |
| **requesting-code-review** | 完成功能后请求代码审查 | 功能完成时 |
| **receiving-code-review** | 接收 code review 反馈后的处理流程 | 收到 review 意见 |
| **finishing-a-development-branch** | 分支收尾（合并/PR/清理） | 开发完成时 |
| **using-git-worktrees** | 创建隔离工作区 | 需要隔离环境时 |
| **writing-skills** | 创建/编辑/验证自定义 Skill | 沉淀项目规范 |

### 其他流程 Skill

| Skill | 来源 | 用途 |
|---|---|---|
| **planning-with-files** | 插件 | 任务计划写到 Markdown 文件，自动追踪进度，支持 `/clear` 后恢复 |
| **andrej-karpathy-skills:karpathy-guidelines** | 插件 | 减少 LLM 常见编码错误（过度设计/盲目修改/缺少验证） |
| **simplify** | 内置 | 审查已修改代码的复用性、质量和效率，修复发现的问题 |
| **security-review** | 内置 | 对当前分支变更做安全审查 |
| **review** | 内置 | PR 审查 |
| **loop** | 内置 | 定时执行命令或 skill |

---

## 三、前端 / UI / 设计

### 设计系统

| Skill | 来源 | 用途 | 本项目适用场景 |
|---|---|---|---|
| **ui-ux-pro-max** | 插件 | UI/UX 设计智能（67 风格/161 调色板/57 字配/25 图表/99 UX 准则） | 改切割工作台、看板等页面 |
| **ui-styling** | 插件 | Tailwind/shadcn/ui 组件美化 | 使用 shadcn 组件时 |
| **design-system** | 插件 | Design Token 架构、组件规范 | 建立设计系统 |
| **design** | 插件 | 综合设计（品牌/Logo/CIP/图标/Banner） | 需要全套设计 |
| **brand** | 插件 | 品牌调性、视觉一致性 | 营销页面/品牌相关 |
| **slides** | 插件 | HTML 演示文稿（Chart.js） | 技术分享 slide |
| **banner-design** | 插件 | 社交媒体 Banner/广告图 | 宣传素材 |

### 前端实现

| Skill | 来源 | 用途 |
|---|---|---|
| **frontend-design** | 插件/项目 | 高质量前端界面，避免 AI 模板化审美 |
| **web-design-guidelines** | 项目 | 页面可访问性/响应式/焦点/动效/暗黑模式检查 |
| **web-artifacts-builder** | 插件 | 复杂多组件 HTML 制品（React + Tailwind + shadcn/ui） |
| **webapp-testing** | 插件 | Playwright 自动化测试前端页面 |

### 前端工具链

| Skill | 来源 | 用途 |
|---|---|---|
| **pnpm** | 项目 | pnpm 包管理器 |
| **antfu** | 项目 | Anthony Fu 风格工具链配置 |
| **unocss** | 项目 | UnoCSS 原子化 CSS 引擎 |
| **vitest** | 项目 | Vitest 单元测试框架 |
| **vitepress** | 项目 | VitePress 文档站 |
| **nuxt** | 项目 | Nuxt 全栈框架 |
| **pinia** | 项目 | Pinia 状态管理 |
| **tsdown** | 项目 | TypeScript 库打包 |
| **turborepo** | 项目 | Monorepo 管理 |
| **slidev** | 项目 | 开发者幻灯片 |

---

## 四、文档 / 演示 / 办公

| Skill | 来源 | 用途 | 本项目适用场景 |
|---|---|---|---|
| **docx** | 插件 | Word 文档创建/编辑/格式处理 | 论文/需求文档编辑 |
| **pdf** | 插件 | PDF 读取/合并/拆分/加密/OCR | 处理 PDF 资料 |
| **pptx** | 插件 | PPT 创建/编辑/模板/备注 | 毕设答辩/汇报 |
| **academic-pptx-skill** | 内置 | 学术演示文稿（论文/答辩/讲座） | 毕设答辩 PPT |
| **xlsx** | 插件 | Excel/CSV 创建/编辑/公式/图表 | 数据报表/导入导出 |
| **doc-coauthoring** | 插件 | 技术文档协作写作流程 | 写技术方案/设计文档 |
| **internal-comms** | 插件 | 内部沟通文档（周报/公告/复盘） | 项目汇报 |
| **theme-factory** | 插件 | 为文档/幻灯片/页面套用主题 | 统一文档风格 |
| **brand-guidelines** | 插件 | Anthropic 品牌色/字体规范 | 对外输出时 |
| **canvas-design** | 插件 | 静态视觉设计（海报/艺术图） | 做海报 |
| **algorithmic-art** | 插件 | p5.js 生成艺术/粒子系统 | 创意视觉 |
| **slack-gif-creator** | 插件 | Slack 动图制作 | 团队沟通 |

---

## 五、质量 / 测试 / 审查

| Skill | 来源 | 用途 |
|---|---|---|
| **code-review-and-quality** | 项目 | 多维度代码审查（正确性/可读性/架构/安全/性能/测试） |
| **test-driven-development** | Superpowers | TDD 红绿重构流程 |
| **systematic-debugging** | Superpowers | 结构化调试方法 |
| **verification-before-completion** | Superpowers | 完成前验证证据 |
| **requesting-code-review** | Superpowers | 代码审查请求流程 |
| **receiving-code-review** | Superpowers | 处理审查反馈 |
| **vue-testing-best-practices** | 项目 | Vue 组件测试 |
| **security-review** | 内置 | 安全审查 |
| **review** | 内置 | PR 审查 |

---

## 六、配置 / 辅助

| Skill | 来源 | 用途 |
|---|---|---|
| **update-config** | 内置 | 修改 settings.json |
| **keybindings-help** | 内置 | 自定义快捷键 |
| **fewer-permission-prompts** | 内置 | 减少权限弹窗 |
| **skill-creator** | 插件 | 创建/优化自定义 Skill |
| **mcp-builder** | 插件 | 开发 MCP Server |
| **claude-api** | 插件/内置 | Claude API/SDK 开发 |
| **claude-hud:setup** | 插件 | 配置状态栏 |
| **claude-hud:configure** | 插件 | HUD 显示选项 |
| **init** | 内置 | 初始化 CLAUDE.md |

---

## 七、快速参考：按场景选 Skill

| 我要做什么 | 用哪个 Skill |
|---|---|
| 接到新需求，梳理清楚再动手 | `brainstorming` → `writing-plans` |
| 开始写后端代码 | `cutting-system-backend` + `java-springboot` |
| 改排版算法 | `cutting-system-algorithm` + `test-driven-development` |
| 改 API 接口 | `cutting-system-api-contract` |
| 写前端页面 | `element-plus-vue3` + `vue-best-practices` + `ui-ux-pro-max` |
| 提升页面设计质量 | `ui-ux-pro-max` + `frontend-design` |
| 上线前检查页面 | `web-design-guidelines` |
| 前端自动化测试 | `webapp-testing` |
| 遇到 Bug | `systematic-debugging` |
| 功能写完，准备提交 | `verification-before-completion` → `code-review-and-quality` |
| 审查代码质量 | `code-review-and-quality` + `security-review` |
| 多人协作/长周期任务 | `planning-with-files` |
| 写技术文档 | `doc-coauthoring` |
| 处理论文/Word | `docx` |
| 做答辩 PPT | `academic-pptx-skill` + `pptx` |
| 处理 Excel 数据 | `xlsx` |
| 写周报/汇报 | `internal-comms` |
| 沉淀项目规范 | `skill-creator` |
| 开发 MCP Server | `mcp-builder` |
