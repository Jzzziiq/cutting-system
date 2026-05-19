# 开发过程踩坑记录

> 记录与业务逻辑无关的通用性 Bug、环境问题和架构陷阱，供后续开发参考。
> 最后更新: 2026-05-07

---

## 一、编码问题

### 1.1 Windows CMD GBK vs UTF-8 乱码

**现象**: Java 日志中中文显示为 `�?佳迭代次数`，MySQL CLI 查询结果显示 `��������Ա`

**原因**: Windows CMD 默认代码页为 GBK(936)，而 Java/MySQL 使用 UTF-8

**修复**:
- MySQL CLI: 连接时加 `--default-character-set=utf8mb4`
- Java 测试: `pom.xml` 中 `maven-surefire-plugin` 添加 `<argLine>-Dfile.encoding=UTF-8</argLine>`
- `application.yml` 添加 `logging.charset.console: UTF-8` + `spring.output.ansi.enabled: never`
- 建议终端执行前先 `chcp 65001`

### 1.2 Windows curl 传 JSON 中文报错

**现象**: `Invalid UTF-8 start byte 0xb2`，Jackson 反序列化失败

**原因**: Windows `curl.exe` 发送中文 JSON 时使用系统编码(GBK)而非 UTF-8

**修复**: 测试时用英文数据，或使用 `--data-binary @file.json` 指定 UTF-8 文件

### 1.3 mysqldump stderr 混入 SQL 文件

**现象**: `docker/init/01-schema.sql` 首行为 `mysqldump: [Warning] Using a password...`

**原因**: `mysqldump ... > file.sql` 只重定向 stdout，但 warning 输出到 stderr 在某些 shell 中也会混入

**修复**: 使用 `mysqldump ... 2>/dev/null > file.sql`

---

## 二、YAML 配置陷阱

### 2.1 缩进断裂导致 datasource 掉到 logging 下

**现象**: 164 测试全报 `Failed to determine a suitable driver class`

**原因**: 编辑 `application.yml` 时新增的 `logging:` 块缩进不当，`datasource:` 从 `spring:` 下掉到了 `logging:` 下

```yaml
# 错误结构
spring:
  output:
    ansi:
      enabled: never

logging:
  charset:
    console: UTF-8
  datasource:          # ← 这里缩进错了！应该是 spring 的子节点
    driver-class-name: ...
```

**教训**: 编辑 YAML 后立即 `mvn compile` 验证，或使用带 YAML 校验的 IDE

### 2.2 `Spring:` vs `spring:` 大小写

**现象**: 旧 `application.yml` 使用 `Spring:`（大写 S），Spring Boot 宽松绑定能解析，但不符合 YAML 惯例

**修复**: 统一为小写 `spring:`

---

## 三、Git 安全陷阱

### 3.1 GitHub Secret Scanning 告警

**现象**: GitHub 发送安全邮件，提示仓库包含疑似密钥

**泄露内容**:
- `application.yml` 中 JWT 密钥 `xK7vN9mP2qR8tL5wY3nH6jB4cF1dG0sA9eU8iO7pM2k=` 明文硬编码
- `application.yml` 中 MySQL 密码 `0625` 明文硬编码
- `.env` 文件被 git 跟踪(未加入 `.gitignore`)

**修复**:
1. 轮换 JWT 密钥(生成新值，旧值作废)
2. `application.yml` 中敏感值改为 `${ENV_VAR:placeholder}`
3. `.env` 加入 `.gitignore` 并从仓库删除
4. 创建 `.env.example` 模板文件
5. `git filter-branch` 清除 31 个历史提交中的密钥
6. 强制推送覆盖远程

**教训**:
- 项目初始化时就配置 `.gitignore`，将 `.env`、`application-local.yml` 等纳入
- 敏感配置一律用环境变量，不在代码中硬编码
- Docker Compose 的 `environment` 也不要有硬编码回退值

### 3.2 `.env` 未被 `.gitignore` 跟踪

**现象**: `.env` 文件被提交到仓库

**原因**: 项目初始 `.gitignore` 中没有 `*.env` 规则

**修复**: 在 `.gitignore` 中添加:
```
.env
*.env.local
*.env.production
application-local.yml
```

---

## 四、Spring/MyBatis 代理冲突

### 4.1 `@Async` 在 MyBatis-Plus ServiceImpl 上不生效

**现象**: 方法标注 `@Async` 但始终在调用线程同步执行

**原因**: `AlgorithmServiceImpl extends ServiceImpl<Mapper, Entity>` — MyBatis-Plus 的 `ServiceImpl` 创建了 CGLIB 代理。Spring 的 `@Async` 也需要创建代理。两个代理链冲突，`@Async` 被忽略

**尝试过的无效方案**:
- `@Async("algorithmExecutor")` 直接加在 `ServiceImpl` 方法上
- `new Thread().start()` 在 Controller 中
- `CompletableFuture.runAsync()` 在 `@Component` 中
- `@Scheduled` 定时轮询（未知原因也未执行）

**当前方案**: 使用同步执行，真正的异步延后到引入消息队列阶段

**教训**: 需要代理增强的功能(AOP、@Async、@Transactional)不要和 MyBatis-Plus ServiceImpl 混用，应拆分为独立的 `@Component`

### 4.2 @Slf4j `log` 变量与参数名冲突

**现象**: 编译错误 `log.error(...)` 提示找不到合适的方法

**原因**: `TAuditLogServiceImpl.asyncSave(TAuditLog log)` 中参数名 `log` 与 `@Slf4j` 生成的 `log` 字段冲突

**修复**: 参数改名 `logEntry`

---

## 五、多租户拦截器影响系统表

### 5.1 TenantLineInnerInterceptor 对 RBAC 表注入 user_id 过滤

**现象**: 登录接口报 NPE: `Cannot invoke "Long.longValue()" because "currentUserId" is null`

**原因**: `UserIdHandler.getTenantId()` 从 `UserContext.getCurrentUserId()` 取值。登录接口排除在 TokenInterceptor 外，UserContext 为空。而登录流程调用 `roleService.listRoleCodesByUserId()` 触发了对 `t_role` 表的查询，TenantLineInnerInterceptor 尝试注入 `user_id = null` 过滤条件导致 NPE

**修复**: `UserIdHandler` 两处改动:
```java
// 1. getTenantId() 防御 null
if (currentUserId == null) return new LongValue(0L);

// 2. ignoreTable() 排除系统表
private static final List<String> IGNORE_TABLES = List.of(
    "t_user", "t_role", "t_permission", "t_user_role", "t_role_permission"
);
```

### 5.2 新增系统表时需同步更新 ignoreTable

**教训**: 后续新增系统级表(如 `t_audit_log`, `t_algorithm_task`)时，不需要加到 IGNORE_TABLES，因为这些表的查询发生在 TokenInterceptor 鉴权之后(UserContext 已有值)。但如果需在鉴权前访问，则必须加入

---

## 六、API 签名变更导致测试大面积失败

### 6.1 JwtUtil.generateToken 增加参数

**现象**: 新增 `roleCodes` 参数后，8 个测试文件编译失败

**修复**: 所有调用处从 `generateToken(user)` 改为 `generateToken(user, List.of("admin"))`，并补 `import java.util.List`

### 6.2 LoginInfo 构造器增加字段

**现象**: `@AllArgsConstructor` 生成 6 参数构造器，旧代码 `new LoginInfo(1L, "admin", "admin", "token")` 编译失败

**修复**: 改为 `new LoginInfo(1L, "admin", "admin", "token", null, null)`

---

## 七、依赖缺失

### 7.1 spring-boot-starter-aop 未引入

**现象**: `AuditLogAspect` 编译报 `找不到符号: 类 Aspect`, `类 Around`, `类 ProceedingJoinPoint`

**修复**: `pom.xml` 添加:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

---

## 八、命名遗留问题

### 8.1 TestController 命名不当

**位置**: `controller/TestController.java`

**问题**: 该 Controller 是算法求解入口(`POST /algorithm/answer`)，但命名为 `TestController`，容易误解为测试代码

**建议**: 后续重构时重命名为 `AlgorithmLegacyController` 或合并到 `AlgorithmController`

### 8.2 entitys 包名

**位置**: `entitys/` (正确拼写为 entities)

**状态**: CLAUDE.md 明确说明"不要改包名"

---

## 快速检查清单

开发新模块后，建议确认以下项:

- [ ] `mvn compile` 通过
- [ ] `mvn test` 全量通过
- [ ] 新增表/字段执行了 DB 迁移脚本
- [ ] 新增系统表加到 `UserIdHandler.IGNORE_TABLES`(如鉴权前需访问)
- [ ] 无硬编码密码/密钥/JWT secret 提交
- [ ] `.env` / `application-local.yml` 在 `.gitignore` 中
- [ ] `.env.example` / `application-local.yml.example` 已更新
- [ ] YAML 结构正确(缩进、无重复 key)
- [ ] `@Async` / `@Transactional` 未加在 MyBatis-Plus ServiceImpl 上
- [ ] 没有方法参数与 `@Slf4j` 生成的 `log` 重名
- [ ] 前端 `npm run build` 通过
