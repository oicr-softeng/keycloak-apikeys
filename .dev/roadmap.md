# Roadmap

## Migrate to public SPI for broad KC version compatibility [in progress]

**Goal:** make the plugin a drop-in replacement across KC 22+ - no plugin version change needed when the KC version changes.

The plugin is consumed by Song and Score for API key introspection via `POST /apikey/check_api_key/`. It runs as a JAR in Keycloak's `providers/` directory; an incompatible JAR causes silent endpoint failure with no meaningful error from KC.

**Root cause of version coupling:** the plugin accesses KC-internal JPA entities (`UserEntity`, `UserAttributeEntity`) directly. These have no stability guarantee across major versions. The rest of the plugin uses KC's public SPI (`RealmResourceProvider`, `UserModel`, `KeycloakSession`) which is stable across KC 17+.

**Strategy:**
1. Migrate all JPA entity access to the public `UserModel` API (`user.setAttribute()`, `user.getAttributeStream()`, `session.users().searchForUserByUserAttribute()`). This removes the only hard version coupling.
2. Compile against KC 22 as the minimum supported version. Using only public SPI methods that exist in KC 22 means the same JAR runs on KC 22 through 26+ without recompilation.
3. Audit remaining internal usages (`AuthenticationManager`, `BearerTokenAuthenticator`, authorization SPI) - migrate to public alternatives where they exist, document as known internal coupling where they do not.
4. Expand testcontainers-keycloak to run integration tests against KC 22, KC 24, and KC 26 to establish and maintain the compatibility matrix.
5. Re-enable `ApiKeyResourceTest` (Issue #10) as the primary regression gate.

**Target minimum: KC 17.** The `RealmResourceProvider` / `RealmResourceProviderFactory` SPI and the full `UserModel` API (`setAttribute`, `getAttributeStream`, `searchForUserByUserAttribute`) all predate KC 17 and have not changed in ways that break plugins using them. KC 17 (February 2022) is also the first version to ship the Quarkus distribution as the default, making it the earliest version that matches the container model overture-dev uses. There is no known reason the plugin cannot run on KC 17+ once JPA entity usage is removed; the version matrix tests will confirm it.

**Why KC 22 was the previous compile target:** it was the version the plugin was built and tested against, not a measured minimum. It is not a meaningful lower bound.

**Test matrix:** KC 17, KC 22, KC 24, KC 26 - covering the likely range of installations in the wild. Verify that `dasniko/testcontainers-keycloak` provides KC 17 container images; older versions may not be on Docker Hub.

Song and Score are already KC-version-agnostic on the client side (standard `spring-boot-starter-oauth2-resource-server`, no KC adapters). Only this plugin needed updating.

**Work items (see tech-debt for detail):**
- JPA entity migration: `UserEntity`/`UserAttributeEntity` → `UserModel` public API (key item; enables version independence)
- Compile target: set `keycloak.version` to KC 17 minimum; verify all API calls exist in KC 17
- Auth manager API: audit and migrate `AuthenticationManager.AuthResult`, `AppAuthManager.BearerTokenAuthenticator`
- Authorization SPI: audit `AuthorizationProvider`, `DefaultEvaluationContext` for public equivalents
- JBoss resteasy: review and remove if unnecessary with KC Quarkus distribution
- testcontainers: KC 17/22/24/26 version matrix; verify KC 17 image availability
- Re-enable `ApiKeyResourceTest` (Issue #10)
