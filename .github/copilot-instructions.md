# Project Guidelines

## Language and Collaboration

- Respond to the user in French by default. Keep code identifiers, APIs, and technical names consistent with the existing English codebase.
- When the user asks for analysis, recommendations, or explicitly says not to edit, inspect and explain options without changing files. Implement when the user clearly asks for a change.
- Prefer focused, low-noise investigation: start from the named file or behavior and inspect nearby ownership boundaries before broad searches.
- Preserve user edits. Do not revert unrelated changes or overwrite changed files without first reading their current contents.
- **GitHub**: never commit or push without an explicit request; preserve local changes and exclude generated outputs and secrets.

## Architecture

- Keep responsibilities explicit. Separate domain state, orchestration, input handling, and rendering; extract a manager, controller, or reusable component when it removes real duplication or clarifies ownership.
- Avoid both responsibility-heavy classes and abstractions created only for hypothetical reuse. Explain meaningful tradeoffs before broad refactors.
- Prefer reusable components with explicit configuration and per-instance state. UI window management must support multiple windows, z-order, lifecycle, and input capture centrally; renderers should draw and delegate content rather than own window behavior.
- Keep simulation independent of camera visibility. Cull for rendering, not for game rules.
- **Game rules**: never silently invent a gameplay rule.
- Avoid recomputing expensive data each frame when it can be cached and invalidated by meaningful changes. Profile before optimizing; keep debug visualization throttled and buffered/instanced for large populations.
- Follow established ownership patterns: `GameManager` orchestrates game collaborators, registries own handler registration, XML resources are parsed through shared caches, and `WorldRenderer` delegates entity presentation to the existing rendering abstractions.

## Package Organization

- Organize packages by domain and responsibility, not only by Java type. Keep enums in concept-local `enums` packages, and records under `model/records`, grouped by domain.
- Keep generated data separate from the algorithms that create it. For terrain, `TerrainMap` owns the terrain data; implementations of `TerrainGenerator` live under `terrain/generation`.
- Avoid duplicate or parallel package trees, generic dumping packages, and empty packages. Before moving types, verify their current location and update all consumers together.
- For procedural generation, inject `Random` so tests can reproduce maps; keep random generation as the default unless the feature requires persisted seeds.

## Java and Formatting

- Use the existing Java/Gradle structure under `app/src/main/java` and matching tests under `app/src/test/java`.
- Use the repository formatter settings: tabs for indentation, tab width 4, and print width 100 (`.prettierrc`). Preserve surrounding file style.
- Do not run broad formatting or change formatter settings unless requested. Keep comments concise and add them only when they explain non-obvious behavior.
- Preserve established public APIs unless the requested change requires migration; update all affected callers and tests together.

## Validation

- Run focused tests first, then `./gradlew :app:test` for behavioral changes when practical.
- Tests should cover behavior and edge cases, not merely implementation details. For graphics or gameplay changes, tests alone do not prove the in-game behavior: run the app and verify it when the environment allows. If runtime verification is unavailable, say so explicitly.
- Do not claim a visual or gameplay fix solely because compilation or unit tests pass.
- Keep performance measurements scoped and low-overhead; distinguish CPU command-submission timings from actual GPU execution.
