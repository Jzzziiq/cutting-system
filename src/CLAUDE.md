---
paths: ["src/**", "pom.xml", "scripts/**"]
---

# 后端开发规则

## 常用命令

```powershell
mvn test
mvn "-Dmaven.repo.local=target\.m2" test
mvn "-Dmaven.repo.local=target\.m2" spring-boot:run
```

## 硬约束

- `entitys` 是既有包名，不要无任务改名。
- 受保护接口必须携带 `Authorization: Bearer <token>`；`TokenInterceptor` 会把 `userId` 写入 `UserContext` 并在请求结束后清理。
- 表结构变更优先放入 `src/main/resources/db/migration/`，并同步实体、DTO/VO、Mapper/XML、前端、小程序和测试。
- 修改 `TabuSearch`、`ReadDataUtil` 或放置/旋转/间距/利用率逻辑时，优先补充 `AlgorithmUnitTest`。
- 后端接口路径、参数或返回字段变化时，必须同步检查 `frontend/src/api/` 和 `miniprogram/services/api.js`。
