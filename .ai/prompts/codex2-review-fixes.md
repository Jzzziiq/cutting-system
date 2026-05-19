# Codex2: Review CC Fixes

请进入审阅模式，只审阅，不修改代码，除非我明确要求你修。

仓库路径：

```text
F:\Code\Java\cutting-system
```

输入文档：

```text
F:\Code\Java\cutting-system\.ai\issues\current.md
F:\Code\Java\cutting-system\.ai\reports\current.md
```

可选审阅输出文件：

```text
F:\Code\Java\cutting-system\.ai\reviews\codex-review.md
```

任务：

1. 读取 `F:\Code\Java\cutting-system\AGENTS.md` 和 `F:\Code\Java\cutting-system\CLAUDE.md`。
2. 读取 `F:\Code\Java\cutting-system\.ai\issues\current.md`。
3. 读取 `F:\Code\Java\cutting-system\.ai\reports\current.md`。
4. 审阅当前代码改动。
5. 如需要保存审阅意见，写入 `F:\Code\Java\cutting-system\.ai\reviews\codex-review.md`。

重点检查：

1. 是否逐项解决了问题文档中的问题。
2. 是否符合 `F:\Code\Java\cutting-system\AGENTS.md` 和 `F:\Code\Java\cutting-system\CLAUDE.md`。
3. 是否有超范围修改。
4. 是否遗漏后端、前端、小程序或测试同步修改。
5. 是否需要补充测试或文档变更记录。

请按 P0/P1/P2/P3 输出审阅意见。若没有发现问题，请明确说明未发现阻塞问题，并列出仍未验证的风险。
