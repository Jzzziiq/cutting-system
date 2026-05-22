# 测试策略

## 常用命令

```powershell
mvn test
mvn "-Dmaven.repo.local=target\.m2" test
mvn "-Dmaven.repo.local=target\.m2" -Dtest=ClassNameTest test
cd frontend; npm run build
```

## 变更类型与推荐验证

| 变更类型 | 推荐测试 |
| --- | --- |
| 算法逻辑 | `mvn "-Dmaven.repo.local=target\.m2" -Dtest=AlgorithmUnitTest test` 或完整 `mvn test` |
| 接口路径、认证、响应结构 | MockMvc 测试，优先补充 `InterfaceSmokeTest` 和 `AuthenticationAuthorizationTest` |
| 服务和数据库写入逻辑 | Service 单测或集成测试；必要时补 SQL 初始化数据 |
| 网页端改动 | `cd frontend; npm run build`，必要时本地启动检查页面 |
| 小程序端改动 | 微信开发者工具导入 `miniprogram/` 后预览检查 |
| 脚本改动 | 在 Windows PowerShell 下运行对应脚本 |

## 高成本验证边界

Codex 默认不要主动执行大规模测试或高成本验证，包括但不限于全量测试、冒烟测试、浏览器自动化、Playwright/E2E 流程、长时间服务联调。

除非用户明确授权，交付时应提供给其他 agent 执行的测试方案。风险较高但未执行的验证，需要写明：

- 原本建议执行的测试方案。
- 具体命令。
- 前置条件。
- 测试数据。
- 预期结果。
- 注意事项。

测试方案较短时可直接写在回复中；内容较多时写入 `docs/test/` 目录下的 Markdown 文档，并在回复中给出路径。
