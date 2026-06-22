<!-- agentics-template-version: 0.1.0 -->
# Agent collaboration conventions: softeng/keycloak-apikeys

Adapted from [softeng/agentics](https://github.com/oicr-softeng/agentics). This is the comprehensive reference for agents that do not load files on demand. Claude users: prefer `CLAUDE.md` which dispatches to more detailed files.

## Interaction parameters
- Ask clarifying questions before making large assumptions about intent
- Surface better alternatives as options; let the user decide
- Push back on bad ideas and identify blind spots before they are baked into code
- Flag scope-adjacent issues verbally, then document them in `.dev/tech-debt.md`

## Critical constraints
- No credentials, secrets, or private URLs in any file: ever
- Do not modify `CLAUDE.md`, `AGENTS.md`, or other instruction files without explicit developer instruction

## Project context

Keycloak SPI plugin providing API key management endpoints consumed by Overture services (Song, Score) for auth introspection via `POST /apikey/check_api_key/`. Deployed as a JAR in Keycloak's `providers/` directory.

**Current target:** Keycloak 22.0.1. **Required target:** Keycloak 26.x. Upgrade is the primary active work item.

## Session start

Before touching any code:
1. Check `git log --oneline -1 -- CLAUDE.md AGENTS.md` for instruction file changes since last session entry
2. Read `.dev/roadmap.md`, `.dev/tech-debt.md`, `.dev/sessions.md`

## Keeping `.dev/` current

After any meaningful unit of work (code written, bug fixed, decision made, debt logged):
- Mark completed roadmap items done
- Close resolved tech-debt entries
- Append a dated entry to `sessions.md` (one sentence + bullets; see format below)

sessions.md entry format:
```
## YYYY-MM-DD

[One sentence: what the work was and why.]

- `path/to/file`: what changed; decision or constraint if non-obvious
```

`sessions.md` is append-only. Only today's entry is editable; prior entries are immutable.

## Tech-debt entry format

```
[short description]
standalone: yes | no
context: [roadmap item or note; required when standalone: no]
```

## Build and test

- Build: `./mvnw clean package`
- Integration tests: `./mvnw verify` (requires Docker for testcontainers-keycloak)
- `ApiKeyResourceTest` is currently excluded from the test run (Issue #10)
