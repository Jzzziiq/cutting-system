---
name: cutting-system-algorithm
description: Project-specific cutting layout algorithm workflow for cutting-system. Use when modifying TabuSearch, skyline placement, ReadDataUtil, algorithm DTOs, Solution or placement models, layout result calculations, or algorithm tests.
---

# Cutting System Algorithm

## Workflow

Read `AGENTS.md` first, then inspect the algorithm entrypoint and the exact data flow before editing:

- Controller entry: `POST /algorithm/answer`
- Input DTO: `InstanceDTO`
- Output DTO: `SolutionResponseDTO`
- Core search: `TabuSearch`
- Multi-container solving and parsing: `ReadDataUtil`
- Primary tests: `AlgorithmUnitTest`

Preserve the existing external contract unless the user explicitly asks for a breaking change. `POST /algorithm/answer` returns `List<SolutionResponseDTO>` directly, while other business APIs normally return `Result`.

## Algorithm Rules

When changing placement, rotation, spacing, search order, or utilization:

- Keep `TabuSearch.evaluate(...)` focused on skyline placement evaluation.
- Keep `TabuSearch.search()` focused on tabu-search iteration and improvement.
- Keep `ReadDataUtil.getSolution(...)` responsible for repeated multi-container solving until remaining rectangles are placed or cannot fit.
- Treat `rotateEnable`, `gapDistance`, container `L` and `W`, and each rectangle `id`, `l`, and `w` as API-level inputs that must remain compatible with the web and mini program clients.
- Verify placements stay inside the container, do not overlap, respect gap distance, preserve rectangle identity, and report utilization consistently.

## Testing

Update `src/test/java/com/cutting/cuttingsystem/model/AlgorithmUnitTest.java` for algorithm behavior changes.

Cover the affected scenarios, especially:

- rectangle fits without rotation
- rectangle fits only with rotation
- gap distance makes a placement invalid
- empty input list
- multi-container solving
- impossible rectangle or remaining item handling
- no overlap and in-bounds placement invariants

Preferred commands:

```powershell
mvn "-Dmaven.repo.local=target\.m2" -Dtest=AlgorithmUnitTest test
mvn "-Dmaven.repo.local=target\.m2" test
```

If algorithm behavior or DTO semantics change, update `AGENTS.md` and its change log.
