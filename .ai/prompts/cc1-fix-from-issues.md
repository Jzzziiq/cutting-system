# CC1: Analyze Issues, Implement Fixes, And Report

请使用 planning with files 工作方式，分析并执行修复。

仓库路径：

```text
F:\Code\Java\cutting-system
```

输入文档：

```text
F:\Code\Java\cutting-system\.ai\issues\current.md
```

输出目录：

```text
F:\Code\Java\cutting-system\.ai\reports
```

输出报告：

```text
F:\Code\Java\cutting-system\.ai\reports\current.md
```

工作要求：

1. 先读取 `F:\Code\Java\cutting-system\AGENTS.md`、`F:\Code\Java\cutting-system\CLAUDE.md` 和 `F:\Code\Java\cutting-system\.ai\issues\current.md`。
2. **归档当前问题文档**：将 `current.md` 复制为 `round-NNN.md`（按已有编号递增，如已有 round-001.md 则新建 round-002.md），保留完整历史记录。
3. 逐项分析输入文档中的问题。
4. 为每个问题给出解决方案。
5. 按方案执行代码修改。
6. 运行相关测试或构建命令。
7. 将解决方案、修改内容、验证结果写入 `F:\Code\Java\cutting-system\.ai\reports\current.md`。
8. 如果 `F:\Code\Java\cutting-system\.ai\reports` 或 `F:\Code\Java\cutting-system\.ai\issues` 不存在，请创建。
9. 全部修复完成后，将 `current.md` 重置为空白模板，等待下一轮标注。
10. 不要做与问题无关的重构或格式化。

输出文档格式：

```md
# 修复报告

## 输入来源

- 问题文档：`F:\Code\Java\cutting-system\.ai\issues\current.md`
- 执行时间：
- 执行者：Claude Code

## 总体结论

- 已修复：
- 未修复：
- 需要人工确认：

## 问题处理明细

### P1-001 问题标题

- 原因分析：
- 解决方案：
- 修改文件：
- 修改内容：
- 验证方式：
- 验证结果：
- 残余风险：

## 修改文件汇总

-

## 执行过的命令

```powershell

```

## 未完成事项

-

## 给 Codex 审阅者的说明

- 请重点审查修复是否符合 `F:\Code\Java\cutting-system\AGENTS.md`。
- 请检查是否有遗漏测试、接口契约破坏或超范围修改。
```
