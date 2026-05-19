---
name: cutting-system-backend
description: Project-specific Spring Boot backend workflow for cutting-system. Use when modifying Java backend controllers, services, service implementations, mappers, MyBatis XML, entities, DTO/VO classes, JWT interceptor/auth utilities, or MockMvc tests in this repository.
---

# Cutting System Backend

## Workflow

Read `AGENTS.md` first, then inspect the exact controller, service, mapper, entity, DTO/VO, test, and frontend or mini program call sites touched by the request.

Keep changes small and layered:

- Preserve the existing package structure, including the historical `entitys` package name.
- Keep REST paths, table fields, DTO/VO field names, and response shapes stable unless the user explicitly asks for a contract change.
- Return business responses through `Result` with `code`, `msg`, and `data`; remember `POST /algorithm/answer` is the known exception and returns `List<SolutionResponseDTO>` directly.
- Put write-heavy business logic in `service/impl`, keep controllers thin, and keep mapper XML aligned with mapper interfaces and entities.
- Keep JWT behavior consistent with `WebConfig`, `TokenInterceptor`, and `UserContext`: protected APIs require `Authorization: Bearer token` and request cleanup must not leak user context.

## Testing

Choose the narrowest useful verification:

- API path, validation, auth, or response changes: add or update MockMvc tests, prioritizing module tests plus `InterfaceSmokeTest` or `AuthenticationAuthorizationTest` when relevant.
- Service or database writes: add service/integration coverage and SQL fixtures only when the behavior cannot be checked at the controller layer.
- Algorithm-touching backend changes: also use `$cutting-system-algorithm`.

Preferred commands:

```powershell
mvn "-Dmaven.repo.local=target\.m2" -Dtest=<TestClass> test
mvn "-Dmaven.repo.local=target\.m2" test
```

If a change adds an API, modifies a data model, adjusts algorithm behavior, or introduces a dependency, update `AGENTS.md` and its change log in the same task.
