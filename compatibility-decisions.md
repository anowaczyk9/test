# Compatibility Decisions

## Scope

This document is the durable F-01 evidence log for Spring Boot 4 compatibility decisions. Spike edits are temporary; only evidence, decisions, fallback/blocker notes, owners, and S-01 impact belong here.

## Candidate inputs

| Input | Value | Evidence |
|---|---|---|
| Spring Boot candidate | `4.0.6` | Latest stable `4.0.x` listed in Maven Central metadata for `org.springframework.boot:spring-boot-starter-parent` and `spring-boot-dependencies`, checked 2026-05-28. |
| Spring Cloud candidate | `2025.1.1` | User-approved plan input in `context/changes/compatibility-decision-spikes/plan.md`. |
| Spring Framework path | Framework 7 through Spring Boot 4 dependency management | User-approved Boot 4 platform path in `context/changes/compatibility-decision-spikes/plan.md`. |
| springdoc candidate | `3.0.x` | User-approved plan input in `context/changes/compatibility-decision-spikes/plan.md`. |
| Resilience4j candidate | `resilience4j-spring-boot4` | User-approved plan input in `context/changes/compatibility-decision-spikes/plan.md`. |
| CXF candidate | `4.2.x` | User-approved plan input in `context/changes/compatibility-decision-spikes/plan.md`. |
| Jasypt first path | Keep `jasypt-spring-boot-starter:3.0.5` first | User-approved plan input in `context/changes/compatibility-decision-spikes/plan.md`; fallback recorded only if spike evidence requires it. |

## Current baseline snapshot

| Area | Current baseline | Repo reference |
|---|---|---|
| Spring Boot | `3.5.13` parent | `pom.xml:14-18` |
| Spring Cloud | `2025.0.2` BOM property | `pom.xml:20-21`, `pom.xml:394-404` |
| Java | `25` | `pom.xml:32-34` |
| CXF | `4.1.4` | `pom.xml:35-36`, `pom.xml:188-202` |
| Jasypt | `jasypt-spring-boot-starter:3.0.5`; encryptable properties enabled | `pom.xml:353-357`, `src/main/java/pl/santander/vas/VasApplication.java:23-32` |
| springdoc | `springdoc-openapi-starter-webmvc-ui:2.6.0` | `pom.xml:223-227` |
| OpenAPI generator | `openapi-generator-maven-plugin:7.13.0`, `useJakartaEe=true` | `pom.xml:441-470` |
| Jackson/generated-client support | `jackson-databind-nullable:0.2.1`, `jackson-module-jaxb-annotations:2.12.3` | `pom.xml:338-347` |
| Feign and Resilience4j | Spring Cloud OpenFeign starter, Spring Cloud CircuitBreaker Resilience4j starter, Feign OkHttp `13.6` | `pom.xml:122-134` |
| Internal libraries | `pl.santander.sales.wksme1:wksme1-commons-logging:7.2.4`, `pl.santander.utils:jwt:2.2.10` | `pom.xml:136-155` |
| Maven verification tooling | enforcer dependency convergence, OpenAPI generation, Jacoco, Surefire, Sonar | `pom.xml:425-520` |
| Runtime encrypted config scope | Runtime configuration contains encrypted values; values are not copied here | `src/main/resources/application.yml:18-21`, `src/main/resources/application.yml:29-34` |

## Decision matrix

| Cluster | Selected candidate/input | Evidence command/source | Result | Fallback/blocker and owner | S-01 impact |
|---|---|---|---|---|---|
| Platform: Boot/Cloud/Framework | Boot `4.0.6`, Spring Cloud `2025.1.1`, Framework 7 via Boot | OpenRewrite active Boot 4 dry run generated `target/rewrite/rewrite.patch`; temporary Boot/Cloud compile spike failed during dependency resolution on `org.hibernate:hibernate-validator` coordinate; Phase 3 proved `org.hibernate.validator` groupId fix passes dependency resolution; compile then fails on expected Boot 4 package moves (26 files) and Swagger 1.x removal (same as OpenAPI cluster) | proceed | Owner: implementation owner. Fix `org.hibernate:hibernate-validator` → `org.hibernate.validator:hibernate-validator` coordinate, then apply OpenRewrite patch by category (99 files: POM, source, test, config). | S-01 uses Boot `4.0.6` and Cloud `2025.1.1`; requires hibernate-validator coordinate fix, OpenRewrite-guided package-move and annotation migrations, and Swagger 1.x → v3 migration across 26 files. |
| Jasypt encrypted config | Keep current `jasypt-spring-boot-starter:3.0.5` | Phase 3 targeted Boot 4 spike with enforcer skip; OpenRewrite patch analysis | proceed | Owner: implementation owner. No Jasypt-specific blocker; library resolves under Boot 4 and OpenRewrite does not change Jasypt dependency or `@EnableEncryptableProperties` usage. Manual environment smoke (F-02) is the final confirmation gate. | S-01 keeps `jasypt-spring-boot-starter:3.0.5` and `@EnableEncryptableProperties`; only autoconfig exclusion package paths need updating per OpenRewrite patch. |
| OpenAPI/springdoc/Jackson | springdoc `3.0.3`, OpenAPI generator `7.13.0`, `jackson-databind-nullable:0.2.1`, Boot 4-managed Jackson `2.21.2` | Phase 4 Boot 4 spike: `generate-sources` passed, zero generated-source compilation errors, no signature/serialization drift in models; main-source compilation failed on Swagger 1.x annotations only (`io.swagger:swagger-annotations` removed in favour of `io.swagger.core.v3:swagger-annotations-jakarta`) | proceed | Owner: implementation owner. Swagger 1.x → v3 annotation migration required in 26 files (controllers + checked-in external client models); `jackson-module-jaxb-annotations:2.12.3` explicit pin should be removed (Boot 4 manages `2.21.2`). No generated-client HTTP/JSON contract drift. | S-01 migrates Swagger annotations (`@ApiModelProperty` → `@Schema`, `@ApiOperation` → `@Operation`, etc.) and removes explicit Jackson module version pin. Generated `currentAccounts` client compiles and serializes identically. |
| CXF/SOAP | Remove CXF dependencies; no active code use | Phase 4 evidence: zero CXF/javax.xml.ws/soap/jws imports in source; zero `@WebService`/`@EnableJaxRs`/`@EnableJaxWs`/CXF config references; only SOAP reference is log-masking regex patterns in `CustomValueMasker.java` and config YAML (`fullyAnonymizedSoapPatterns`). Enforcer confirms CXF 4.1.4 causes `angus-activation` 2.0.2/2.0.3 convergence conflict under Boot 4. | proceed | Owner: implementation owner. CXF dependencies are unused legacy; removal eliminates the enforcer convergence conflict and ~15 transitive JARs. Log-masking SOAP regex patterns are string-only and do not depend on CXF library classes. | S-01 removes `cxf-spring-boot-starter-jaxrs`, `cxf-rt-frontend-jaxws`, `cxf-rt-transports-http`, `jaxb-runtime:4.0.6`, `jaxws-api:2.3.1`, `javax.xml.soap-api:1.4.0`, `jaxb-impl:2.3.0`, `jsr181-api:1.0-MR1`, and `javax.activation:activation:1.1.1`. Eliminates enforcer angus-activation conflict. |
| Feign/Resilience4j/internal libs | Spring Cloud OpenFeign `5.0.1`, CircuitBreaker Resilience4j `5.0.1`, `feign-okhttp:13.6`, `wksme1-commons-logging:7.2.4`, `pl.santander.utils:jwt:2.2.10` | Phase 4 Boot 4 spike: all dependencies resolved under Cloud `2025.1.1`; zero Feign/Resilience4j/internal-library-specific compilation errors; `DefaultFeignClientBaseConfiguration`, Feign clients, `AuthRequestInterceptor`, `Http5xxErrorDecoder` all compile; only errors in wksme1 external client models are Swagger annotation issue (same as OpenAPI cluster) | proceed | Owner: implementation owner. Internal libraries (`wksme1-commons-logging:7.2.4`, `jwt:2.2.10`) resolve and compile without Feign/Resilience4j conflicts; assumption validated. Swagger annotation migration in `SmeModuleChanges.java`/`SmeVersionChanges.java` is the only required S-01 work. | S-01 keeps current Feign/Resilience4j/internal library versions; only Swagger annotation migration needed in 2 checked-in external client model files. |
| Maven/CI tooling | Current Maven plugins and Java 25 toolchain | Phase 1 baseline commands passed; Phase 2 OpenRewrite patch generation succeeded but compile spike reached Maven dependency resolution then failed before enforcer/generated sources/compile | fallback | Owner: implementation owner for POM coordinate fix; CI owner if internal repository mirrors must be updated after coordinate correction | S-01 starts from a green Boot 3.5.13 baseline but needs a validation-coordinate fix before Maven tooling can provide deeper Boot 4 evidence. |
| Manual CI/DevOps confirmations | Confirm OpenJDK 25 builder/runtime, GitLab/Jenkins behavior, Sonar/Jacoco, S2I packaging | Phase 2 questions recorded below; manual confirmation outside repo | fallback | Owner: CI/DevOps owner to be assigned. S-01 can proceed without these confirmations; unresolved items become explicit follow-up tasks. Safe to proceed because F-01 scope is local Maven evidence only; CI pipeline behavior is confirmed during S-01 deployment, not during compatibility spikes. | S-01 should not change CI/deployment; unresolved confirmations become explicit follow-up rather than hidden migration work. |

## Baseline verification

| Gate | Command | Result | Notes |
|---|---|---|---|
| F-02 targeted safety net | `mvn -B "-Dspring.profiles.active=test" "-Djasypt.encryptor.password=dummy" "-Dtest=AuthRegressionSafetyNetTest,KeyCloakSessionRegressionSafetyNetTest,SecureWithTokensRegressionSafetyNetTest,OffersRegressionSafetyNetTest,WebAnalyticsQueueServiceTest,RabbitMqMessageUtilsTest,TragarzBakubaUploadFileTest,PreLimitProcessResponseServiceTest,LiquibaseJpaRegressionSmokeTest,JasyptRegressionSmokeTest,ActuatorRegressionSmokeTest,CustomValueMaskerTest" test` | pass | 2026-05-28: 39 tests, 0 failures, 0 errors, 0 skipped; build success; total time 01:32 min. Command source: `context/changes/regression-proof-safety-net/manual-smoke.md:62-68`. |
| CI-like package gate | `mvn -B "-DdisableJWTFilter=true" "-Dspring.profiles.active=dev" "-Djasypt.encryptor.password=dummy" package` | pass | 2026-05-28: 591 tests, 0 failures, 0 errors, 9 skipped; build success; total time 02:08 min. Command source: `context/changes/regression-proof-safety-net/manual-smoke.md:70-74`. |

## Phase 2 platform spike evidence

| Gate | Command/evidence | Result | Classification |
|---|---|---|---|
| OpenRewrite active recipe dry run | `mvn -B org.openrewrite.maven:rewrite-maven-plugin:6.40.0:dryRun "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.31.0" "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0"`; user rerun produced `target/rewrite/rewrite.patch` | patch generated for 99 files: `pom.xml`, 21 generated external client files, 45 main Java files, 29 test Java files, 2 main resource files, and 1 test resource file | primary blast-radius evidence |
| OpenRewrite patch categories | `target/rewrite/rewrite.patch`, checked 2026-05-29 | POM changes include Boot/Cloud upgrade, web/webservices starter renames, springdoc `3.0.3`, Swagger v3 Jakarta annotations, Jakarta XML SOAP/WS, Jackson 3 `tools.jackson`, HTTP Client 5, Liquibase starter, Boot 4 test starters, Mockito update, and Lombok annotation processor path; source changes include Swagger annotation migrations, generated-client `javax.annotation` -> `jakarta.annotation`, Jackson 3 imports/configuration, Spring Boot package moves, `org.springframework.lang.NonNull` -> `org.jspecify.annotations.NonNull`, test annotation updates, and `server.contextPath` -> `server.servlet.context-path` | wide platform/client/test/config blast radius for S-01 |
| Temporary Boot/Cloud compile spike | Temporary `pom.xml` edit only: parent `3.5.13` -> `4.0.6`, `spring-cloud.version` `2025.0.2` -> `2025.1.1`; command `mvn -B "-DskipTests" compile` | failed after 01:29 min during dependency resolution: `Could not find artifact org.hibernate:hibernate-validator:jar:9.0.1.Final` in the configured repository | platform/dependency blocker, before enforcer, generated sources, or Java compile |
| Coordinate cross-check | Maven Central metadata for `org.hibernate:hibernate-validator` and `org.hibernate.validator:hibernate-validator`, checked 2026-05-29 | `org.hibernate:hibernate-validator` has no `9.0.1.Final`; `org.hibernate.validator:hibernate-validator` contains `9.0.1.Final` | direct dependency coordinate adaptation required in S-01 |
| Revert check | `git --no-pager diff -- pom.xml`; `git --no-pager status --porcelain` | no intended `pom.xml`, generated source, or config spike diff remains after removing temporary OpenRewrite-added validation dependencies from the working tree | temporary edits reverted; `target/rewrite/rewrite.patch` is evidence only |

## CI/DevOps confirmation questions

| Question | Owner | Scope boundary |
|---|---|---|
| Do GitLab/Jenkins builder and runtime images provide OpenJDK 25 and Maven settings compatible with Boot `4.0.6` and Spring Cloud `2025.1.1` artifacts? | CI/DevOps owner | Confirm only; do not change CI in F-01. |
| Does the internal Maven mirror/proxy need refresh or allow-list changes after the Hibernate Validator coordinate is corrected to the Boot 4-compatible group? | CI/DevOps owner | Confirm repository availability only; S-01 owns POM changes. |
| Do existing GitLab Maven goals and Jenkins shared library wrappers pass through the same Maven settings used by the local baseline commands? | CI/DevOps owner | Confirm command/runtime parity only. |
| Are Sonar `5.5.0.6356`, Jacoco `0.8.14`, and Surefire `3.5.4` accepted by the CI environment with Java 25 and Boot 4 classpaths? | CI/DevOps owner | Confirm compatibility; plugin changes belong to S-01 only if evidence requires them. |
| Does the OpenShift/S2I packaging path expect Boot 3-specific layout, layers, or start commands? | CI/DevOps owner | Confirm packaging expectations only; no OpenShift resource edits in F-01. |

## Secret-handling boundary

No real encrypted value, plaintext secret, token, partner credential, SFTP credential, Jasypt password, or internal environment value should be copied into this file. Evidence may name sanitized property categories, commands, artifact coordinates, and pass/fail status only.

## Phase 3 Jasypt encrypted config evidence

| Gate | Command/evidence | Result | Classification |
|---|---|---|---|
| Targeted Boot 4 Jasypt spike | Temporary `pom.xml`: parent `4.0.6`, Cloud `2025.1.1`, `org.hibernate.validator` groupId fix; command `mvn -B "-Dspring.profiles.active=test" "-Djasypt.encryptor.password=dummy" "-Dtest=JasyptRegressionSmokeTest" "-Denforcer.skip=true" test` | dependency resolution passed; OpenAPI generation passed; compilation failed on unrelated Boot 4 package-move errors in `VasApplication.java` and `IndexController.java` (`org.springframework.boot.web.client` and `org.springframework.boot.web.servlet.error` packages moved); no Jasypt-related error appeared | Jasypt itself is not a blocker; test execution blocked by unrelated Boot 4 source-level changes |
| Enforcer convergence (pre-Jasypt) | Same spike without `-Denforcer.skip=true` | enforcer dependency convergence fails on `org.eclipse.angus:angus-activation` 2.0.2 (Boot 4 saaj-impl) vs 2.0.3 (CXF 4.1.4); failure is CXF-vs-Boot version conflict, not Jasypt | Phase 4 CXF decision; not a Jasypt concern |
| OpenRewrite Jasypt patch section | `target/rewrite/rewrite.patch` diff for `JasyptRegressionSmokeTest.java` | only changes: `DataSourceAutoConfiguration` → `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`, `LiquibaseAutoConfiguration` → `org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration`, `HibernateJpaAutoConfiguration` → `org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration` | autoconfig exclusion package paths move; `@EnableEncryptableProperties`, Jasypt imports, and test logic are unchanged |
| Jasypt library compatibility signal | `jasypt-spring-boot-starter:3.0.5` downloaded and resolved successfully under Boot 4.0.6 dependency tree; no version conflict or missing artifact for `com.github.ulisesbocchio` coordinates | library resolves cleanly | no Jasypt dependency blocker |
| Fallback assessment | Not triggered; current path shows no Jasypt-specific failure | fallback not needed | proceed with `3.0.5` |
| Manual environment smoke handoff | Evidence shape per `context/changes/regression-proof-safety-net/manual-smoke.md:109-115`: profile/environment, Jasypt implementation/version path (`jasypt-spring-boot-starter:3.0.5`), sanitized property name, startup/decryption signal, and pass/fail | deferred to F-02 manual environment smoke; no secrets stored here | F-02 gate; not F-01 scope |
| Revert check | `git checkout -- pom.xml`; confirmed no pom.xml diff | temporary spike fully reverted | clean |

## Phase 4 client and integration compatibility evidence

### OpenAPI/springdoc/Jackson cluster

| Gate | Command/evidence | Result | Classification |
|---|---|---|---|
| Generate-sources under Boot 4 | Temporary `pom.xml`: parent `4.0.6`, Cloud `2025.1.1`, `org.hibernate.validator` groupId fix, springdoc `3.0.3`, `io.swagger.core.v3:swagger-annotations-jakarta:2.2.28`; command `mvn -B "-Denforcer.skip=true" "-DskipTests" generate-sources` | BUILD SUCCESS in 9.5s; all `currentAccounts` client models, API classes, and invoker classes generated identically | OpenAPI generator `7.13.0` with `useJakartaEe=true` works under Boot 4 |
| Generated-source compilation | Same spike; command `mvn -B "-Denforcer.skip=true" "-DskipTests" test-compile` | zero compilation errors in `target/generated-sources/openapi/`; all 104 errors are in main-source (non-generated) files | generated client compiles cleanly under Boot 4 |
| Generated-client signature drift | Compared regenerated `CurrentAccounts.java`, `ApiClient.java` model structure, imports, annotations | still uses `com.fasterxml.jackson` (Jackson 2.x), `jakarta.annotation.Generated`, `jakarta.annotation.Nullable`, `org.openapitools.jackson.nullable.JsonNullableModule`; no namespace/signature/serialization drift | no HTTP/JSON contract drift |
| Jackson version under Boot 4 | `dependency:tree` for `com.fasterxml.jackson.core` | Boot 4.0.6 manages Jackson `2.21.2` (still Jackson 2.x); `jackson-databind-nullable:0.2.1` resolves; `jackson-module-jaxb-annotations` managed at `2.21.2` but overridden by explicit `2.12.3` pin in POM | version pin mismatch; safe to remove explicit `2.12.3` in S-01 |
| Swagger 1.x annotation errors | Compile errors for missing `ApiModelProperty`, `ApiImplicitParam`, `ApiImplicitParams`, `ApiOperation`, `Api` classes | 26 files affected: 7 external model/client files + 14 controllers + 3 DTOs + 2 Boot 4 package-move files (`IndexController`, `VasApplication`) | Swagger annotation migration is S-01 work scope; well-categorized by OpenRewrite patch |
| Revert check | `git checkout -- pom.xml`; confirmed no pom.xml diff | temporary spike fully reverted | clean |

### CXF/SOAP cluster

| Gate | Command/evidence | Result | Classification |
|---|---|---|---|
| Source-code usage search | `grep` for CXF imports (`import.*cxf`), SOAP API imports (`import.*javax.xml.ws`, `import.*javax.xml.soap`, `import.*javax.jws`), CXF annotations (`@EnableJaxRs`, `@EnableJaxWs`, `@WebServiceClient`), CXF config properties (`cxf.` in `application*.yml`) | zero matches across entire `src/` tree | no active CXF/SOAP code usage |
| SOAP string references | `grep` for any `soap` reference in source | only log-masking: `SOAP_PATTERN` in `CustomValueMasker.java` (regex for masking XML elements), `fullyAnonymizedSoapPatterns` and `partiallyAnonymizedSoapPatterns` config keys in YAML | string/regex patterns only; no CXF library dependency at runtime |
| CXF dependency tree | `mvn dependency:tree "-Dincludes=org.apache.cxf"` | 3 direct CXF dependencies pull ~15 transitive JARs: `cxf-spring-boot-starter-jaxrs:4.1.4`, `cxf-rt-frontend-jaxws:4.1.4`, `cxf-rt-transports-http:4.1.4`, plus `cxf-core`, `cxf-rt-bindings-soap`, `cxf-rt-wsdl`, `cxf-rt-databinding-jaxb`, etc. | large unused dependency surface |
| Enforcer convergence conflict | `mvn validate` under Boot 4 spike | `org.eclipse.angus:angus-activation` 2.0.2 (Boot 4 saaj-impl) vs 2.0.3 (CXF 4.1.4 via `cxf-core`, `cxf-rt-bindings-soap`, `cxf-rt-frontend-simple`) | CXF removal eliminates this enforcer conflict |
| Legacy javax dependencies linked to CXF | `jaxws-api:2.3.1`, `javax.xml.soap-api:1.4.0`, `jsr181-api:1.0-MR1`, `javax.activation:activation:1.1.1`, `jaxb-impl:2.3.0` in POM | all are javax-namespace APIs associated with CXF/SOAP/JAXB legacy surface; no source-code imports | removable with CXF |
| Decision | Remove all CXF and associated legacy javax SOAP/WS/activation dependencies | reduces dependency surface, eliminates enforcer conflict, no code changes needed | proceed with removal |

### Feign/Resilience4j/internal libraries cluster

| Gate | Command/evidence | Result | Classification |
|---|---|---|---|
| Dependency resolution under Boot 4 | `dependency:tree` with Boot 4.0.6 + Cloud 2025.1.1 | `spring-cloud-starter-openfeign:5.0.1`, `spring-cloud-starter-circuitbreaker-resilience4j:5.0.1`, `feign-okhttp:13.6`, `wksme1-commons-logging:7.2.4` (+ transitive `wksme1-commons-jwt:7.2.4`), `pl.santander.utils:jwt:2.2.10` all resolved | all internal and external Feign/Resilience4j dependencies resolve |
| Compilation of Feign infrastructure | Boot 4 `test-compile` spike | zero errors in `DefaultFeignClientBaseConfiguration.java`, all Feign client interfaces, `MorsApiClientConfiguration.java`, `AuthRequestInterceptor.java`, `Http5xxErrorDecoder.java` | Feign configuration and clients compile under Boot 4 |
| Internal library compatibility | Same spike; `wksme1-commons-logging` and `jwt` transitive tree resolved | no missing artifact or version conflict for `pl.santander.*` coordinates | internal libraries compatible |
| Swagger annotation overlap | `SmeModuleChanges.java` and `SmeVersionChanges.java` (checked-in wksme1 external client models) use `io.swagger.annotations` | same Swagger 1.x annotation issue as OpenAPI cluster; not a Feign/internal-library issue | covered by OpenAPI/springdoc/Jackson cluster migration |
| Resilience4j/CircuitBreaker | No Resilience4j-specific compilation errors | Spring Cloud CircuitBreaker `5.0.1` compatible with Boot 4 | proceed |
| Decision | Keep current versions; internal-library assumption validated | all dependencies resolve and compile; Swagger annotation migration is the only required S-01 work in external client models | proceed |

## Phase 5 final verification evidence

| Gate | Command/evidence | Result | Notes |
|---|---|---|---|
| `git diff -- pom.xml src/` | 2026-05-29 | no diff; zero temporary spike edits remain | clean working tree for production code |
| F-02 targeted safety net | `mvn -B "-Dspring.profiles.active=test" "-Djasypt.encryptor.password=dummy" "-Dtest=AuthRegressionSafetyNetTest,..." test` | pass | 38 tests, 0 failures, 0 errors, 0 skipped; build success; total time 01:07 min. |
| CI-like package gate | `mvn -B "-DdisableJWTFilter=true" "-Dspring.profiles.active=dev" "-Djasypt.encryptor.password=dummy" package` | pass | BUILD SUCCESS; total time 02:13 min. |
| Decision matrix completeness | All 7 rows reviewed | 5× proceed, 1× fallback (Maven/CI tooling), 1× fallback (CI/DevOps confirmations); zero `pending` rows remain | complete |
