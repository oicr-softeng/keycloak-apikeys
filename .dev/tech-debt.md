# Tech debt

## JPA entity migration: UserEntity/UserAttributeEntity → UserModel public API

standalone: no
context: public SPI migration roadmap. This is the key item that enables broad version compatibility. `ApiKeyService` (and possibly others) access `org.keycloak.models.jpa.entities.UserEntity` and `UserAttributeEntity` directly via `EntityManager`. These are KC-internal classes with no stability guarantee.

Migrate to public `UserModel` API:
- Attribute reads/writes: `user.setAttribute()`, `user.getAttributeStream()`, `user.getFirstAttribute()`
- User lookup by attribute: `session.users().searchForUserByUserAttribute(realm, attributeName, value)`

Once this is done, the plugin no longer has any hard KC version coupling and can be compiled against KC 22 and run on KC 22-26+ from the same JAR.

## Compile target: set keycloak.version to KC 17 minimum

standalone: no
context: public SPI migration roadmap. After the JPA entity migration, set `keycloak.version` in pom.xml to 17.x (the target minimum). Do not raise the minimum unless a required public API genuinely does not exist in KC 17. KC 22 was the previous compile target only because it was tested, not because earlier versions were ruled out. Also review all transitive KC BOM dependencies for anything not present in KC 17.

## Auth manager API: audit and migrate

standalone: no
context: public SPI migration roadmap. `AuthService` uses `AuthenticationManager.AuthResult` and `AppAuthManager.BearerTokenAuthenticator` from `org.keycloak.services.managers`. These sit in the services layer (not public SPI, not internal entities - somewhere in between). Verify whether KC 26 provides a public equivalent (e.g. `TokenVerifier`, `AccessToken` via `org.keycloak.TokenVerifier`). Migrate if a public path exists; document as known internal coupling if not. Either way, verify the signatures still compile against KC 22 after migration.

## Authorization SPI: audit for public equivalents

standalone: no
context: public SPI migration roadmap. Plugin uses `AuthorizationProvider`, `DefaultEvaluationContext`, and related classes from `org.keycloak.authorization.*`. The authorization SPI is more stable than the JPA entities but has been restructured. Audit whether the plugin actually needs scope evaluation at the KC layer or whether this can be handled in the plugin's own logic. If KC authorization SPI is required, confirm API stability across KC 22-26 and document the dependency.

## testcontainers-keycloak: KC 17/22/24/26 version matrix

standalone: no
context: public SPI migration roadmap. Integration tests use `com.github.dasniko:testcontainers-keycloak:3.0.0`. Expand to run tests against KC 17, KC 22, KC 24, and KC 26 containers. Use JUnit 5 parameterized tests or a Maven Surefire profile per KC version. First: verify that `dasniko/testcontainers-keycloak` supports KC 17 container images (older images may not be published). Check https://github.com/dasniko/testcontainers-keycloak for the library version that covers KC 17-26.

## JBoss resteasy dependency: review for KC Quarkus

standalone: no
context: public SPI migration roadmap. `pom.xml` includes `org.jboss.resteasy:resteasy-multipart-provider`. KC 26 (Quarkus) bundles its own RESTEasy. Verify whether this dependency is needed at compile time only (resolved by BOM) or whether it is a runtime addition. Remove if it is redundant; if it is needed, confirm it does not conflict with the KC 26 Quarkus distribution.

## Re-enable ApiKeyResourceTest (Issue #10)

standalone: no
context: public SPI migration roadmap. `ApiKeyResourceTest` is currently excluded from the Surefire run. Re-enable once the JPA entity migration compiles and the testcontainers version is updated. This is the primary integration test gate for the `check_api_key/` endpoint that Song and Score depend on; it must pass across all versions in the compatibility matrix before a release is cut.
