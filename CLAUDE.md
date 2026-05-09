# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Coding Guidelines (Karpathy)

Bias toward caution over speed. For trivial one-line fixes, use judgment.

### 1. Think Before Coding

- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop, name what's confusing, and ask.

### 2. Simplicity First

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Test: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

Test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria require constant clarification.

## Context

Read `AGENTS.md` first — it is the canonical project context file with detailed architecture, API maps, data models, algorithm conventions, and changelog.

## Essential Commands

```powershell
# Run all tests
mvn test

# Isolate Maven local repo (avoids global ~/.m2 cache issues):
mvn "-Dmaven.repo.local=target\.m2" test

# Run a single test class
mvn "-Dmaven.repo.local=target\.m2" -Dtest=ClassNameTest test

# Run the backend
mvn spring-boot:run

# Start both backend + frontend (recommended for local dev)
powershell.exe -ExecutionPolicy Bypass -File scripts\start-dev.ps1

# Stop dev services
powershell.exe -ExecutionPolicy Bypass -File scripts\stop-dev.ps1

# Frontend dev server
cd frontend && npm install && npm run dev

# Frontend production build
cd frontend && npm run build
```

- Backend runs on `http://127.0.0.1:8080`, frontend dev server on `http://127.0.0.1:5173`.
- Logs go to `logs/dev/backend.log` and `logs/dev/frontend.log` when started via scripts.
- Use the `-Restart` flag with `start-dev.ps1` to restart, `-SkipInstall` to skip frontend dependency install.

## Key Project Conventions

- **Response format**: All business endpoints return `Result { code, msg, data }`. Exception: `POST /algorithm/answer` returns `List<SolutionResponseDTO>` directly.
- **Entity/DTO/VO separation**: Entities map to DB tables. DTOs are for incoming requests. VOs are for outgoing responses. When adding a field, check all three plus mapper SQL and frontend API clients.
- **DB schema changes**: Place incremental SQL in `src/main/resources/db/migration/`.
- **Package name**: The `entitys` package name is intentional — do not rename it.
- **Frontend API clients**: One file per backend module in `frontend/src/api/`, each importing from `http.js` (pre-configured Axios with JWT interceptor and `Result` unwrapping).
- **Cutting pages**: Two production-focused views — `DataInputView` (Excel-style data entry) and `LayoutWorkbenchView` (CAD-style canvas visualization). Canvas rendering is custom (no third-party graphics library).
- **Algorithm testing**: When modifying `TabuSearch` or `ReadDataUtil`, run `mvn -Dtest=AlgorithmUnitTest test`. Cover scenarios: fits, rotation fits, gap blocks fit, empty list, multi-container.

## Before Making Changes

1. Read `AGENTS.md` for the full interface map, data model details, and changelog.
2. Check which tests exist for the module you're touching.
3. For API changes: check both `frontend/src/api/` and `miniprogram/services/api.js`.
4. For frontend changes: prefer Element Plus components over custom HTML; use composables (`useCuttingTable`, `useLayoutCanvas`, `useAlgorithmSubmit`); append new CSS to the `/* === 切割页面样式 === */` block in `main.css`.
5. Run relevant tests before and after changes.
