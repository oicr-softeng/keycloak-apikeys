<!-- agentics-template-version: 0.1.0 -->
# Agent collaboration conventions: softeng/keycloak-apikeys

Adapted from [softeng/agentics](https://github.com/oicr-softeng/agentics). To get updates, compare this file's version tag against the agentics CHANGELOG.

## Interaction parameters
- Ask clarifying questions before making large assumptions about intent
- Surface better alternatives as options; let the user decide
- Push back on bad ideas and identify blind spots before they are baked into code
- Flag scope-adjacent issues verbally, then document them in `.dev/tech-debt.md`

## Critical constraints
- No credentials, secrets, or private URLs in any file: ever
- Do not modify `CLAUDE.md`, `AGENTS.md`, or other instruction files without explicit developer instruction: surface suggestions, do not self-edit

## Project context

Keycloak SPI plugin that adds API key management endpoints to Keycloak:
- `POST /apikey` - create API key
- `GET /apikey` - list API keys
- `POST /apikey/check_api_key/` - validate an API key (consumed by Song and Score for auth introspection)
- `DELETE /apikey/{id}` - revoke an API key

Built as a JAR deployed into Keycloak's `providers/` directory. In overture-dev, it is downloaded at pod startup via an init container and loaded automatically by Keycloak on boot.

**Current target:** Keycloak 22.0.1. **Required target:** Keycloak 26.x (deployed version in overture-dev is 26.3.3). Upgrade is the primary active work item.

## When to read what
- Starting a session → read `.dev/roadmap.md`, `.dev/tech-debt.md`, `.dev/sessions.md`
- Writing or reviewing tests → tests are in `src/test/`; integration tests use testcontainers-keycloak
- Security-relevant work → read `~/.claude/security-guidelines.md` (auth endpoints touch OWASP A01, A07)

## Build and test
- Build: `./mvnw clean package`
- Integration tests: `./mvnw verify` (requires Docker for testcontainers)
- `ApiKeyResourceTest` is currently excluded from the test run (Issue #10); re-enabling it is a tracked debt item
