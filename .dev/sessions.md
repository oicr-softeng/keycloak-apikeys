# Sessions

## 2026-06-22

Bootstrapped devctx; scoped KC26 upgrade work based on overture-dev deployment requirements (Song/Score confirmed KC26-compatible on client side; only this plugin needs updating).

- `CLAUDE.md`, `AGENTS.md`: scaffolded from agentics template v0.1.0; project context added
- `.dev/roadmap.md`: reframed from KC26 upgrade to public SPI migration for broad KC 22+ compatibility; "in place replacement" goal; KC 22 as compile minimum; version matrix testing (22/24/26)
- `.dev/tech-debt.md`: six items; JPA entity migration is the key item enabling version independence; compile target framed as minimum not latest; testcontainers expanded to multi-version matrix
