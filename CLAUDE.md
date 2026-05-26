# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

柜门板材切割排版系统 — Spring Boot 3.5 backend + Vue 3 SPA + 微信小程序. Base package: `com.cutting.cuttingsystem`.

## Commands

```bash
# Backend (Maven wrapper, no global Maven needed)
./mvnw spring-boot:run                    # run on :8080
./mvnw test                               # run all tests
./mvnw -Dtest=ClassNameTest test          # run single test class
mvn "-Dmaven.repo.local=target/.m2" test  # alternative with local repo

# Frontend
cd frontend && npm install && npm run dev   # dev server on :5173 (proxies /api → :8080)
cd frontend && npm run build                # production build

# Both together
powershell -ExecutionPolicy Bypass -File scripts/start-dev.ps1
powershell -ExecutionPolicy Bypass -File scripts/stop-dev.ps1

# Docker
docker-compose up -d --build
```

Prerequisites: JDK 17+, Maven 3.6+, Node.js 20.19+ or 22.12+, MySQL 8.0 (`board_cutting_db`).

## Architecture

```
com.cutting.cuttingsystem
  ├── config/        # WebConfig, JwtConfig, MybatisPlusConfig (tenant + pagination)
  ├── controller/    # REST controllers (17+)
  ├── entitys/       # Entities, DTOs, VOs (intentional spelling — do not rename)
  ├── model/         # Algorithms: TabuSearch, GeneticAlgorithm, AlgorithmRegistry
  ├── service/       # IService<Entity> interfaces + impl/
  ├── mapper/        # MyBatis-Plus BaseMapper interfaces; XML in resources/mapper/
  ├── interceptor/   # TokenInterceptor → UserContext (ThreadLocal)
  ├── aop/           # @AuditLog aspect
  └── util/          # JwtUtil, ReadDataUtil (algorithm orchestrator)
```

**Request flow:** HTTP → `TokenInterceptor` (JWT + RBAC + `UserContext`) → Controller (`@Valid`, `BeanUtils.copyProperties`) → Service → Mapper → MySQL (auto tenant filter via `org_id`).

**Auth:** JWT Bearer tokens. `UserContext` ThreadLocal stores `userId/orgId/roles/permissions`. `@RequirePermission` annotation enforces RBAC. Multi-tenancy: `TenantLineInnerInterceptor` auto-appends `WHERE org_id = ?`; excluded tables: `t_user`, `t_role`, `t_permission`, `t_organization`, `t_cabinet_template`, `t_audit_log`.

**Response envelope:** `Result { code, msg, data }`. Exception: `POST /algorithm/answer` returns raw `List<SolutionResponseDTO>`.

**Migrations:** Raw SQL in `src/main/resources/db/migration/` (no Flyway/Liquibase).

## Rules

- 用户提到"需求文档"指 `docs/user-manual/需求文档.md`。
- Read `AGENTS.md` before code, API, data model, algorithm, or documentation work.
- Keep changes small, explicit, traceable. Don't refactor opportunistically.
- Preserve existing package names, table fields, API paths, DTO/VO fields unless explicitly required to change.
- Cross-endpoint changes must sync: backend controller/service/DTO/VO + `frontend/src/api/` + `miniprogram/services/api.js`.
- Algorithm changes: prioritize `AlgorithmUnitTest`, cover fit/rotation/gap/empty-list/multi-container.
- SQL migrations: execute directly against local MySQL using credentials from `application-local.yml`. Verify with `SHOW COLUMNS`.
- Use low-cost verification by default. No full test suites, E2E, or browser automation unless asked.
- "写入规则"/"记录规则" refer to `CLAUDE.md` or subdirectory `CLAUDE.md`, not memory files.

## Docs

| Topic | File |
|---|---|
| Code layout, module responsibilities | `docs/ai/code-navigation.md` |
| REST API contract, auth, endpoints | `docs/ai/api-contract.md` |
| Entities, DTOs, VOs, DB scripts | `docs/ai/data-model.md` |
| Algorithm (tabu search, skyline) | `docs/ai/algorithm.md` |
| Frontend (Vue, Element Plus, Canvas) | `docs/ai/frontend.md` |
| Testing strategy | `docs/ai/testing.md` |
| Changelog | `docs/ai/change-log.md` |

Subdirectory rules: `src/CLAUDE.md` (backend), `frontend/CLAUDE.md` (frontend), `miniprogram/CLAUDE.md` (小程序).
