---
name: java-springboot
description: Spring Boot guidance adapted for the cutting-system repository. Use when developing, reviewing, or testing Java Spring Boot backend code in this project, especially controllers, services, MyBatis-Plus mappers/XML, DTO/VO validation, JWT interceptor auth, Result responses, Maven tests, and backend configuration.
---

# Java Spring Boot for Cutting System

Use this skill for Spring Boot backend work in `cutting-system`. General Spring Boot best practices apply, but project conventions in `AGENTS.md` and local code always win.

## Project Defaults

- Target Java 17 and Spring Boot 3.5.11 unless the project is explicitly upgraded.
- Use Maven and Spring Boot starters already present in `pom.xml`; do not introduce new starters or dependencies casually.
- Preserve the current layer-based package structure: `controller`, `service`, `service/impl`, `mapper`, `entitys`, `model`, and `util`.
- Do not rename the historical `entitys` package or move code into domain/feature packages unless the user explicitly requests a large refactor.
- Keep configuration externalized. Do not hardcode database passwords, JWT secrets, or production-only values.

## Backend Coding Rules

- Use constructor-based dependency injection and `private final` dependency fields.
- Keep controllers thin: parse/validate requests, call services, and return stable responses.
- Put business logic and data-write coordination in `service/impl`.
- Use DTO/VO classes at API boundaries; do not expose persistence entities as the intended external contract.
- Use Bean Validation annotations on request DTOs when adding or tightening validation.
- Prefer a global exception/response style that preserves existing `Result` behavior.
- Use SLF4J parameterized logging; do not concatenate log messages with request data.

## Data Access

This project uses MyBatis-Plus with MySQL, not Spring Data JPA.

- Use mapper interfaces under `mapper/` and XML under `src/main/resources/mapper/`.
- Keep mapper XML, mapper method signatures, entity fields, DTO/VO fields, and database migration scripts aligned.
- Use MyBatis parameter binding instead of string-built SQL.
- Do not recommend `JpaRepository`, `CrudRepository`, JPA Criteria, `@DataJpaTest`, or entity exposure patterns unless the project actually migrates to JPA.
- For schema changes, add migration scripts under `src/main/resources/db/migration/` and update docs/tests.

## API And Auth Contracts

- Business APIs return `Result` with `code`, `msg`, and `data`.
- `POST /algorithm/answer` is the known exception and returns `List<SolutionResponseDTO>` directly.
- Protected APIs require `Authorization: Bearer <token>`.
- Auth is implemented with `WebConfig`, `TokenInterceptor`, JWT utilities, and `UserContext`, not Spring Security by default.
- Do not introduce Spring Security as the default fix for auth work; adapt to the existing interceptor model unless the user requests a security migration.
- If changing paths, request/response fields, auth behavior, or API wrappers, also use `$cutting-system-api-contract`.

## Testing

Choose the narrowest test that proves the change:

- Controller/API/auth changes: MockMvc tests, especially module tests plus `InterfaceSmokeTest` or `AuthenticationAuthorizationTest` when relevant.
- Service/database write logic: service or integration tests with the minimum needed fixtures.
- Algorithm-touching backend work: `AlgorithmUnitTest` and `$cutting-system-algorithm`.

Preferred commands:

```powershell
mvn "-Dmaven.repo.local=target\.m2" -Dtest=<TestClass> test
mvn "-Dmaven.repo.local=target\.m2" test
```

Do not default to Testcontainers, PostgreSQL, Java 21, Spring Security, or JPA testing patterns for this repository.

## Documentation

When a backend change adds an API, modifies a data model, adjusts algorithm logic, or introduces a dependency, update `AGENTS.md` and its change log in the same task.
