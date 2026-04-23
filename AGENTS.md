# Memora Repo Notes

## Current Product Focus

Memora is currently converging on one main delivery path:

`tenant -> knowledge base -> document tree -> editing -> versions -> sync -> permissions`

This repository is no longer organized around the earlier `Doc Studio` skill system. If you find references to resource libraries, AI skills, or generic document generators, treat them as legacy unless the current README or module docs explicitly say otherwise.

## Repository Map

```text
memora-doc/
├── memora-server/      # Spring Boot backend
├── memora-web-app/     # React web console
├── memora-client/      # Tauri sync agent shell
├── doc/                # product, architecture, and dev docs
├── build.gradle.kts
└── settings.gradle.kts
```

## Source Of Truth

- Start with [README.md](./README.md) for current scope and reading order.
- Backend details live in [memora-server/README.md](./memora-server/README.md).
- Web details live in [memora-web-app/README.md](./memora-web-app/README.md).
- Client details live in [memora-client/README.md](./memora-client/README.md).
- Product and architecture decisions live under `doc/`.

## Working Rules

- Prefer improving the online document main flow over expanding side systems.
- Keep multi-tenant isolation, permission checks, and document/version integrity explicit.
- Do not reintroduce old `Doc Studio` naming or `.claude/skills` assumptions.
- When updating docs, prefer fixing canonical entry docs over adding more parallel summaries.

## Validation

- Backend: `gradle test`
- Web: `cd memora-web-app && npm install && npm run lint && npm run build`
- Client: `cd memora-client/src-tauri && cargo check`

## Known Environment Notes

- Backend validation works with system Gradle in this repository.
- Web validation requires local npm dependencies.
- Tauri validation currently depends on a sufficiently new Rust/Cargo toolchain.
