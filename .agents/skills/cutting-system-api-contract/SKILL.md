---
name: cutting-system-api-contract
description: Project-specific API contract workflow for cutting-system. Use when changing REST paths, request or response fields, auth requirements, Result wrappers, algorithm response shapes, frontend API modules, mini program API calls, or API documentation in this repository.
---

# Cutting System API Contract

## Workflow

Read `AGENTS.md` first, then lock the contract before editing code. Identify whether the change affects only the backend or must also update the Vue web app, WeChat mini program, tests, and docs.

For every contract change, check:

- backend controller route, DTO/VO, validation, service behavior, mapper/entity fields, and tests
- web API wrappers under `frontend/src/api/` and any consuming views, composables, stores, or router behavior
- mini program calls under `miniprogram/services/api.js`, `miniprogram/utils/request.js`, and affected pages
- `AGENTS.md` interface map, data model notes, frontend notes, and change log when applicable

## Contract Rules

Preserve compatibility unless the user explicitly requests a breaking change:

- Business APIs return `Result` with `code`, `msg`, and `data`.
- `POST /algorithm/answer` returns `List<SolutionResponseDTO>` directly and frontend error handling must keep that exception in mind.
- Protected APIs require `Authorization: Bearer token`.
- Do not silently rename paths, DTO/VO fields, table fields, or persisted JSON shapes.
- If a backend endpoint changes, update web and mini program callers in the same task or explicitly document why they are unaffected.

## Testing

Use the smallest verification that proves the contract:

```powershell
mvn "-Dmaven.repo.local=target\.m2" -Dtest=<ModuleTest> test
cd frontend
npm run build
```

Prefer MockMvc tests for backend contract changes. For web changes, run the frontend build. For mini program-only changes, state that final preview requires WeChat Developer Tools.

If the change crosses backend, web, and mini program, summarize the contract before implementation and verify each affected layer before delivery.
