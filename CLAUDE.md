# CLAUDE.md

@AGENTS.md

This file is the Claude Code entry point for this repository. `AGENTS.md` is the canonical project context for architecture, commands, API contracts, data models, algorithm rules, frontend/miniprogram conventions, test strategy, and changelog.

## Claude Code Rules

- User mentions of "需求文档" refer to `docs/user-manual/需求文档.md`, not other docs.
- Read `AGENTS.md` before code, API, data model, algorithm, dependency, test, or documentation work.
- Do not duplicate large project facts here; update `AGENTS.md` instead when shared rules or project facts change.
- Keep changes small, explicit, and traceable to the user's request. Mention unrelated issues instead of fixing them opportunistically.
- Preserve existing package names, table fields, API paths, DTO/VO fields, and user edits unless the task explicitly requires changing them.
- For API changes, check backend tests plus `frontend/src/api/` and `miniprogram/services/api.js`.
- For data model changes, check entity, DTO, VO, Mapper/XML, migration scripts, frontend API clients, miniprogram pages, and tests.
- For algorithm changes, prioritize `src/test/java/com/cutting/cuttingsystem/model/AlgorithmUnitTest.java` and cover fit, rotation, gap, empty list, and multi-container scenarios.
- Use relevant, low-cost verification by default. Do not run full tests, browser automation, E2E, or long service integration unless the user asks; provide the recommended test plan when not run.
- If changes include SQL migration scripts, execute them directly against local MySQL using credentials from `src/main/resources/application-local.yml` (password) and `application.yml` (host/port/dbname). Verify with `SHOW COLUMNS` after execution.
- User mentions of "写入规则" or "记录规则" refer to `CLAUDE.md` (project root) or `src/CLAUDE.md` / `frontend/CLAUDE.md` (subdirectory-specific), not memory files.

## Maintenance

- Keep this file short, concrete, and executable; target fewer than 80 lines.
- If a rule grows into architecture, API, glossary, or workflow documentation, move it to the appropriate project document and link from `AGENTS.md`.
- Add a new rule only when it prevents a repeated, real agent mistake or captures a project constraint that must be loaded every time.
