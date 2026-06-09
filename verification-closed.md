# Migration Verification — Closed

> Closed: 2026-06-08
> Branch: `TSMERB-11013-spring-boot-4-migration`
> Pre-migration baseline commit: `8ea55be225e0be67927db7ae0834a7837ad5b8b0` (Boot 3.5.13, Java 25)
> Post-migration HEAD: `39c45370` (Boot 4 / Spring Boot 4.0, Java 25)

## Verdict

**Migration verification complete. Residual serialization risk = zero. Boot 4 is behaviorally equivalent to Boot 3.5.13 on all proven surfaces.**

## What was proven and how

### Serialization vectors (all pinned against measured Boot 3.5 baseline)

| Vector | Endpoint / Type | Proven by | 3.5 baseline |
|---|---|---|---|
| Field ordering (JSON object) | `KeyCloakSessionDTO` (`GET /api/v1/context/user-details`) | `CodeFlowContractIT` fixture | `snake_case` fields in declaration order |
| Naming strategy (snake_case) | `KeyCloakSessionDTO` | `CodeFlowContractIT` fixture | `client_id`, `user_id`, etc. |
| Record serialization | `AocData` record (`POST /api/v1/authorizations`) | `AocContractIT` fixture | all compact-constructor fields present, correct names |
| `@JsonInclude(NON_NULL)` | `AgreementObject` (`GET /api/v1/agreements`) | `AgreementsContractIT` fixture | absent nulls, present non-nulls |
| Enum serialization (string) | `AgreementStatus` | `AgreementsContractIT` fixture | `"ACCEPTED"` |
| `@JsonProperty("name")` override | `AgreementObject.name` | `AgreementsContractIT` fixture | serialized as `"name"` |
| BigDecimal scale + trailing zero | `MoneyInfakt.value` (`GET /api/infakt/customer`) | `InFaktCustomerBigDecimalContractIT` fixture | `1234.50` (trailing zero preserved, plain notation) |
| Global web mapper lever | `spring.jackson.use-jackson2-defaults: true` | S-03 OffersApiContractIT + above pins | confirmed active; databind layer preserved |

All fixtures were captured in a `git worktree` at commit `8ea55be2` (Boot 3.5.13), provenance documented in `context/changes/code-consents-aoc-contracts/baseline-provenance.md`. The assertion is always Boot4 == 3.5 (not Boot4 == Boot4).

### Auth and HTTP contract surfaces (proven by full-context ITs)

| Surface | Proven by | Notes |
|---|---|---|
| JWT filter 401 / bypass | `CodeFlowContractIT`, `CodeFlowSkipPathIT`, `ProtectedEndpointITBase` recipe | Public paths return correct status; protected paths gate on valid tokens |
| `UserContext` propagation | `CodeFlowContractIT` (CODE flow end-to-end) | Context assembled from Keycloak token → downstream service stub → response |
| Offers list / single / config HTTP contracts | `OffersApiContractIT` | 3 scenarios, stable JSON, 401 gates |
| AOC authorization HTTP contract | `AocContractIT` | record DTO, stable JSON |
| Agreements list HTTP contract | `AgreementsContractIT` | NON_NULL, enum, property-override |
| CODE flow (skip-path) | `CodeFlowSkipPathIT` | skip-path header honored, no auth required |

### Non-serialization surfaces (covered zero-diff + locked decisions + smoke)

| Surface | Coverage method | Accepted residual |
|---|---|---|
| JPA / Liquibase | Zero production diff; `LiquibaseJpaRegressionSmokeTest` green | — |
| Jasypt `ENC(...)` | Zero production diff; pin jasypt 3.0.5; `JasyptRegressionSmokeTest` green | — |
| Actuator / observability | Zero production diff; `ActuatorRegressionSmokeTest` green | — |
| RabbitMQ | Zero production diff; only startup-gate `@ConditionalOnProperty` annotations added | — |
| SFTP | Zero production diff; `EmbeddedSftpServer` test infra present | **No test wired to EmbeddedSftpServer** (pre-existing gap, not introduced by migration) |
| GSON downstream clients | `@SerializedName` models untouched; GSON is not Jackson | — |
| Swagger → OpenAPI annotations | Doc-metadata swap only; no payload/route/contract change | — |

### Accepted serialization residuals (no risk)

| Residual | Rationale |
|---|---|
| `Double` in `InFaktSmartLoansOffer.amount` | IEEE-754 double-to-JSON is stable across Jackson 2→3 (`writeNumber(double)` delegates to `Double.toString`); no scale semantics; pre-existing type choice |

## Finding escalated during verification

- **Dead `@SecureWithTokens` aspect** (pre-existing, not introduced by migration): the annotation exists and is referenced on controller methods but the AOP advice never fires (aspect not loaded / pointcut never matches at runtime). Escalated to the team for separate investigation. Does not affect the migration.

## Test infrastructure summary

- **`ProtectedEndpointITBase`** — shared cached `@SpringBootTest` context (no `@DirtiesContext`); declares all infrastructure `@MockitoBean`s; shared by `OffersApiContractIT`, `AocContractIT`, `AgreementsContractIT`, `InFaktCustomerBigDecimalContractIT`.
- **`SpringBootTestBase`** — original shared base with `@DirtiesContext(AFTER_CLASS)`; used by `CodeFlowContractIT`, `CodeFlowSkipPathIT`, smoke tests.
- **Two Spring context boots** in full package gate: one for `SpringBootTestBase` family, one for `ProtectedEndpointITBase` family (cached across subclass ITs).

## Artifacts

| Artifact | Location |
|---|---|
| Baseline provenance (S-04 + S-06 addendum) | `context/changes/code-consents-aoc-contracts/baseline-provenance.md` |
| S-04 fixtures (3 JSON, Boot 3.5 captured) | `src/test/resources/regression-proof-safety-net/{context,aoc,agreements}/` |
| S-06 BigDecimal fixture | `src/test/resources/regression-proof-safety-net/partners/infakt-customer-bigdecimal-stable-json-contract.json` |
| Contract ITs | `CodeFlowContractIT`, `CodeFlowSkipPathIT`, `OffersApiContractIT`, `AocContractIT`, `AgreementsContractIT`, `InFaktCustomerBigDecimalContractIT` |
| Smoke tests | `JasyptRegressionSmokeTest`, `LiquibaseJpaRegressionSmokeTest`, `ActuatorRegressionSmokeTest` |
| Roadmap final state | `context/foundation/roadmap.md` |
