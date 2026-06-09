# Baseline Fixture Provenance — CODE / Consents / AOC Contracts (S-04)

This document is the durable, verifiable provenance record for the three JSON
fixtures used by the S-04 contract ITs (`CodeFlowContractIT`, `AocContractIT`,
`AgreementsContractIT`). It proves the fixtures were serialized by **Spring Boot
3.5.13** (the pre-migration baseline), not circularly regenerated from the
current Boot 4 tree.

## Why provenance matters for S-04

The S-04 contract ITs build synthetic DTOs, serialize them through the live HTTP
path on **Boot 4**, and assert byte-equality against these fixtures. If the
fixtures had also been produced on Boot 4, the assertion would be
"Boot4 == Boot4" — trivially green, proving nothing. The contract only has teeth
if the fixtures encode **3.5 / Jackson 2 serialization** (notably field ordering)
captured before the migration.

The three surfaces under test:

| Surface          | FR     | Endpoint                          | DTO                  |
| ---------------- | ------ | --------------------------------- | -------------------- |
| CODE / user data | FR-010 | `GET /api/v1/context/user-details`| `KeyCloakSessionDTO` |
| AOC              | FR-012 | `POST /api/v1/authorizations`     | `AocData`            |
| Agreements       | FR-013 | `GET /api/v1/agreements`          | `AgreementObject`    |

## Capture facts (verifiable)

- **Commit:** `8ea55be225e0be67927db7ae0834a7837ad5b8b0`
  (`chore(archive): close compatibility-decision-spikes`, 2026-05-29)
- **Spring Boot:** `3.5.13` (pom.xml parent at that commit), Java 25
- **Method:** throwaway full-context `@SpringBootTest` (NOT standalone MockMvc),
  extending `SpringBootTestBase`, `@MockitoBean` for the service collaborators,
  real autoconfigured HTTP message converter (Boot 3.5 default Jackson 2
  `ObjectMapper`).
- **Isolation:** captured in a `git worktree` checked out at `8ea55be2`, so the
  Boot 4 working tree was never disturbed. Worktree removed after capture.
- **Package split (visibility constraint):** `getKeyCloakSession` is
  package-private to `pl.santander.vas.keycloak` and `initializeAuthorization`
  is package-private to `pl.santander.vas.aoc`. A single `pl.santander.vas`
  capture test cannot stub both, so capture was split into two tests living in
  the respective packages (`getAgreements` is public, so it folds into the AOC
  test). **This same constraint means the Phase 3/4 ITs must also live in the
  `keycloak` / `aoc` packages respectively.**
- **user-details skip-path note:** `GET /api/v1/context/user-details` is a
  public (auth-skipped) path listed in `AuthorizationConfiguration.antPatterns`.
  In full-context MockMvc the filter's skip check (`Utils.appliesToPattern`, line
  28) matches the antPattern against `request.getServletPath()`, which is **empty
  under MockMvc**, so the filter does not skip and rejects the request 401. The
  capture test sets `jwt.enabled=false`, which makes `JwtTokenFilter` skip
  unconditionally — faithfully reproducing the public-path behavior the
  antPattern intends. The response body is produced purely by the controller
  serializing the DTO and is unaffected by the filter being skipped. The
  **artifact-vs-regression discrimination is settled empirically below** (see
  "Skip-path: MockMvc artifact, not a Boot 4 regression").
- **Same-log rev-parse proof** (runner emitted `git rev-parse HEAD` immediately
  before the captured bodies in one command/log):

```
PROVENANCE-COMMIT>>>
8ea55be225e0be67927db7ae0834a7837ad5b8b0
PROVENANCE-COMMIT-VERIFY>>>
8ea55be225e0be67927db7ae0834a7837ad5b8b0 2026-05-29 13:15:48 +0200 chore(archive): close compatibility-decision-spikes
===RUNNING CAPTURE===
PROVENANCE-CAPTURE-AOC>>>{"id":"aoc-auth-id-123","digest":"aoc-digest-abc"}<<<
PROVENANCE-CAPTURE-AGREEMENTS>>>[{"key":"MARKETING","name":"Marketing Agreement","status":"ACCEPTED","version":3,"content":"Full agreement text","oldContent":"Previous agreement text"}]<<<
PROVENANCE-CAPTURE-USERDETAILS>>>{"sign_in_session_id":"valid-session-code","cif":"0001234567"}<<<
```

## Byte-equality against committed fixtures

All three captured bodies match the committed fixtures exactly:

- `src/test/resources/regression-proof-safety-net/keycloak/user-details-client-credentials-stable-json-contract.json`
  (declaration order: `sign_in_session_id, cif`)
  ```
  {"sign_in_session_id":"valid-session-code","cif":"0001234567"}
  ```
- `src/test/resources/regression-proof-safety-net/aoc/post-authorizations-stable-json-contract.json`
  (declaration order: `id, digest`)
  ```
  {"id":"aoc-auth-id-123","digest":"aoc-digest-abc"}
  ```
- `src/test/resources/regression-proof-safety-net/agreements/get-agreements-stable-json-contract.json`
  (declaration order: `key, name, status, version, content, oldContent`)
  ```
  [{"key":"MARKETING","name":"Marketing Agreement","status":"ACCEPTED","version":3,"content":"Full agreement text","oldContent":"Previous agreement text"}]
  ```
- `src/test/resources/regression-proof-safety-net/agreements/get-agreements-null-oldcontent-stable-json-contract.json`
  (null-rendering baseline — `oldContent: null`; see "Null-rendering baseline" below)
  ```
  [{"key":"MARKETING","name":"Marketing Agreement","status":"NEW_AVAILABLE","version":1,"content":"Full agreement text","oldContent":null}]
  ```

## Source-unchanged gate (DTO sources at 8ea55be2 vs HEAD)

A `git diff 8ea55be2 HEAD --` was run for the six DTO/contract source files
backing the three surfaces. Result: **5 of 6 byte-identical**; one documented
change, recorded below as a MEASUREMENT (not a pre-judged label):

| Source                  | 3.5 → Boot 4 diff | Serialization impact |
| ----------------------- | ----------------- | -------------------- |
| `AocData`               | identical         | none                 |
| `AocRequest`            | identical         | none                 |
| `AgreementObject`       | identical         | none                 |
| `AgreementStatus`       | identical         | none                 |
| `GratTypeEnum`          | identical         | none                 |
| `KeyCloakSessionDTO`    | CHANGED           | order-neutral (see below) |

### `KeyCloakSessionDTO` change — measured, not assumed

The Boot 4 tree changed `KeyCloakSessionDTO` in two ways:

1. Swagger annotation migration `io.swagger.annotations.ApiModelProperty`
   (Swagger 2) → Swagger 3 equivalents. **Serialization-irrelevant** — Swagger
   annotations do not participate in Jackson output.
2. Added `@JsonPropertyOrder({"sign_in_session_id","cif"})`. **Order-relevant
   only** — pins the JSON field order.

Cross-check of declared order vs measured 3.5 order:

- **Declared at 8ea55be2** (no `@JsonPropertyOrder`): field `code`
  (`@JsonProperty("sign_in_session_id")`) THEN `userCif` (`@JsonProperty("cif")`).
  Jackson 2 with no explicit order emits **declaration order** →
  `sign_in_session_id, cif`.
- **Measured 3.5 capture body:** `{"sign_in_session_id":"valid-session-code","cif":"0001234567"}`
  → order `sign_in_session_id, cif`.
- **HEAD's added `@JsonPropertyOrder`:** pins `sign_in_session_id, cif` — the
  **same** order.

**Conclusion: NEUTRAL.** The added `@JsonPropertyOrder` codifies the exact order
3.5 already emitted by declaration order. The migration change is
order-preserving; no migration-introduced ordering drift. (The user's
halt-if-reversed rule was live during capture and was not triggered.)

Independent corroboration: `KeyCloakSessionRegressionSafetyNetTest` (committed at
8ea55be2, standalone MockMvc) already asserts byte-equality of the user-details
fixture at 3.5 — with explicit `@JsonProperty` names the standalone vs
full-context ordering is identical, so the captured order is genuine 3.5.

## Skip-path: MockMvc artifact, not a Boot 4 regression

The capture-time 401 on user-details (worked around with `jwt.enabled=false`)
raised a real question: is the antPattern skip broken **only under MockMvc**, or
did Spring 7 / Servlet 6 change `getServletPath()` semantics such that the skip
is also broken **in production** (a silent auth regression, like the dead S-02
aspect)? This was settled empirically with two throwaway probes on the Boot 4
tree (commit `39c45370`), issuing the **same** request
(`GET /api/v1/context/user-details`, no auth headers, `jwt.enabled=true` — the
production default, no workaround):

| Harness                                   | `getServletPath()`              | `getPathInfo()`                 | antPattern match | filter skips | HTTP status |
| ----------------------------------------- | ------------------------------- | ------------------------------- | ---------------- | ------------ | ----------- |
| Full-context MockMvc (`MockHttpServletRequest`) | `` (empty)                | `/api/v1/context/user-details`  | no               | no           | **401**     |
| Real embedded Tomcat (Servlet 6 / Jakarta, `webEnvironment=RANDOM_PORT`) | `/api/v1/context/user-details` | `null` | yes | yes | **200** + body |

Real-Tomcat 200 body (service stubbed to a present `Optional`, proving the
controller actually ran): `{"sign_in_session_id":"probe-code","cif":"0001234567"}`.

**Mechanism:** MockMvc's `MockHttpServletRequest` does not simulate the
DispatcherServlet-as-default-servlet (`/`) path split. It leaves `servletPath`
empty and puts the whole path into `pathInfo`, so
`AntPathMatcher.match("/api/v1/context/user-details", "")` is false. Under the
real Servlet 6 container the default servlet returns the full path from
`getServletPath()`, the antPattern matches, the filter skips, and user-details
serves **200 with no auth** — exactly the documented public-skip contract,
intact on Boot 4.

**Verdict: MockMvc artifact. The skip-path is NOT broken in production on Boot 4.**
No escalation. The `jwt.enabled=false` capture workaround is harmless (the body
is controller-produced regardless of the filter). Corroborating evidence: the
3.5 capture of the same endpoint exhibited the identical MockMvc 401 and needed
the identical workaround — the behavior is constant across both versions, i.e.
not migration-introduced.

**Residual closed — skip-path survival is now positively covered.** Because the
body fixture was captured with `jwt.enabled=false`, it proves JSON *shape*
survival but NOT that user-details stays *public*. The real-Tomcat probe above
supplies the missing coverage: on Boot 4, a no-auth request to user-details
returns 200. **Phase 3 (`CodeFlowContractIT`) will therefore carry two assertions:**
(1) a `jwt.enabled=false` MockMvc body-equality test against the user-details
fixture, and (2) a real-Tomcat (`RANDOM_PORT`) no-auth → 200 skip-survival test,
so the public-path contract is asserted, not assumed. The two diagnostic probes
were throwaway and have been deleted; their logic is reproduced above and is the
template for the Phase 3 skip-survival test.

## Null-rendering baseline — agreements `oldContent: null` (Jackson 2 inclusion)

`AgreementObject` is a bare `record` with no `@JsonInclude`, and `oldContent` is
a nullable `String` (null in the production-reachable case of an agreement with
no prior version). Its null-rendering therefore follows the **global** Jackson
configuration. Boot 4 migrated the Jackson **major version (2 → 3)** and the
Boot 4 `application.yml` sets `spring.jackson.use-jackson2-defaults: true` (line
11) — an explicit compatibility flag whose entire job is to preserve Jackson-2
serialization semantics, of which null-inclusion (`Include.ALWAYS`) is one. The
original three fixtures all have non-null fields, so none of them exercised this
flag. A dedicated null baseline was captured to give the flag teeth.

- **Same-log rev-parse proof** (Boot 3.5 worktree at `8ea55be2`):

```
8ea55be225e0be67927db7ae0834a7837ad5b8b0
PROVENANCE-CAPTURE-AGREEMENTS-NULL>>>[{"key":"MARKETING","name":"Marketing Agreement","status":"NEW_AVAILABLE","version":1,"content":"Full agreement text","oldContent":null}]<<<
```

- **Captured body (Boot 3.5 / Jackson 2):** the null field is **included** as
  `"oldContent":null`.
- **Fixture:** `src/test/resources/regression-proof-safety-net/agreements/get-agreements-null-oldcontent-stable-json-contract.json`
  ```
  [{"key":"MARKETING","name":"Marketing Agreement","status":"NEW_AVAILABLE","version":1,"content":"Full agreement text","oldContent":null}]
  ```
- **Contract teeth:** Phase 5's `AgreementsContractIT` asserts this body
  byte-for-byte on Boot 4. If `use-jackson2-defaults: true` fails to preserve
  null-inclusion (e.g. Jackson 3 omits the null, emitting
  `...,"content":"Full agreement text"}]`), the IT goes RED — catching a silent
  serialization regression the populated-field fixtures cannot see.

The capture test (`S04CaptureAgreementsNullTest`, package `pl.santander.vas.aoc`)
is identical to `S04CaptureAocAgreementsTest#captureAgreements` below except the
stubbed `AgreementObject` uses `.oldContent(null)` (and `NEW_AVAILABLE` / version
`1` to mark it as the no-prior-version shape).



## Phase 5 teeth-check results (Boot 4 tree, executed 2026-06-08)

Two independent teeth checks on `AgreementsContractIT`, with different expected
outcomes.

### 1. Record field-ordering

Two complementary probes:

- **Force `spring.jackson.mapper.sort-properties-alphabetically=true`** on the
  real autoconfigured web mapper → **all 3 tests stayed GREEN (no red)**.
  `AgreementObject` is **sort-immune**: the Jackson-3 web mapper serializes record
  components in **declaration order** (`key, name, status, version, content,
  oldContent`), structurally fixed by the canonical constructor and unaffected by
  `SORT_PROPERTIES_ALPHABETICALLY`. Per the slice contract this is an OK,
  informative result — not a defect. (The original Phase-5 expectation that the
  sort flag would turn the record ITs RED was wrong for records; it holds only for
  alphabetically-sortable bean serializers like the S-03 Lombok class.)
- **Positive control — reorder the record components** (`key`↔`name`) and re-run →
  **both happy-path assertions turned RED**. Measured output flipped to
  `[{"name":"Marketing Agreement","key":"MARKETING",...}]` vs the expected
  declaration-order fixture. This decisively proves (a) the serializer follows
  record declaration order, and (b) the byte-equality assertion **catches**
  field-order drift. Record reverted to canonical order after the probe.

Net: record ordering is governed by declaration order, immune to the alphabetical
sort flag, and the test has real teeth against any reorder.

### 2. Null-inclusion (`oldContent: null`)

**Force `spring.jackson.default-property-inclusion=non_null`** → the
**null-fixture test turned RED exactly as predicted**; the populated test stayed
GREEN. Measured output under NON_NULL omitted the field entirely
(`...,"content":"Full agreement text"}`) vs the expected
`...,"oldContent":null`. This proves the null assertion genuinely guards
null-inclusion, so the GREEN result under the production config
(`spring.jackson.use-jackson2-defaults: true`) is a meaningful cross-version proof
that the Jackson-2-compat flag preserves `Include.ALWAYS` null-rendering on Boot 4.
Property reverted after the probe.

### Fixture `status` values (confirmed against the Boot 3.5 capture)

- Populated fixture: `"status":"ACCEPTED"` (enum `name()`), `version` 3,
  `oldContent` non-null — byte-identical to the 3.5 capture.
- Null fixture: `"status":"NEW_AVAILABLE"`, `version` 1, `oldContent` null —
  byte-identical to the 3.5 capture.

Both `status` renderings are the enum `name()` exactly as the 3.5 mapper emitted
them (no assumed value).

## Capture test sources (preserved, not deleted)

The throwaway capture tests below were run in the worktree at `8ea55be2`. They
are preserved here verbatim as durable provenance (deleting capture evidence is
an anti-pattern for a rigor-driven stream). To re-verify:
`git worktree add <dir> 8ea55be2`, drop these files into their packages under
`src/test/java/pl/santander/vas/`, and run
`mvn -B "-Dspring.profiles.active=test" "-Djasypt.encryptor.password=dummy" -Dtest=S04CaptureUserDetailsTest,S04CaptureAocAgreementsTest test`.

### `pl/santander/vas/keycloak/S04CaptureUserDetailsTest.java`

```java
package pl.santander.vas.keycloak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.santander.vas.infrastructure.SpringBootTestBase;

@AutoConfigureMockMvc
@TestPropertySource(properties = "jwt.enabled=false")
class S04CaptureUserDetailsTest extends SpringBootTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeyCloakSessionService keyCloakSessionService;

    @Test
    void captureUserDetails() throws Exception {
        when(keyCloakSessionService.getKeyCloakSession(any(), any(), any()))
            .thenReturn(Optional.of(new KeyCloakSessionDTO("valid-session-code", "0001234567")));

        MvcResult result = mockMvc.perform(get("/api/v1/context/user-details")
                .param("code", "valid-code")
                .param("client_id", "partner-client")
                .param("grant_type", "CLIENT_CREDENTIALS"))
            .andExpect(status().isOk())
            .andReturn();

        System.out.println("PROVENANCE-CAPTURE-USERDETAILS>>>" + result.getResponse().getContentAsString() + "<<<");
    }
}
```

### `pl/santander/vas/aoc/S04CaptureAocAgreementsTest.java`

```java
package pl.santander.vas.aoc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.santander.vas.security.AuthTestFixtures.AUTHORIZATION_TOKEN;
import static pl.santander.vas.security.AuthTestFixtures.AUTHORIZED_PARTY_TOKEN_PDC;
import static pl.santander.vas.security.AuthTestFixtures.CUSTOMER_AUTHORIZED_PARTY_TOKEN;
import static pl.santander.vas.security.AuthTestFixtures.CUSTOMER_REALM;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.santander.utils.jwt.JwtContext;
import pl.santander.vas.agreements.AgreementsService;
import pl.santander.vas.agreements.model.AgreementObject;
import pl.santander.vas.agreements.model.AgreementStatus;
import pl.santander.vas.infrastructure.SpringBootTestBase;
import pl.santander.vas.security.UserContextRepository;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "keycloak-session-config.as-customer-realm.realm=" + CUSTOMER_REALM,
    "keycloak-session-config.as-realm.realm=" + CUSTOMER_REALM,
    "keycloak-session-config.authorized-party-token-pdc=" + AUTHORIZED_PARTY_TOKEN_PDC
})
class S04CaptureAocAgreementsTest extends SpringBootTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AocService aocService;
    @MockitoBean
    private AgreementsService agreementsService;
    @MockitoBean
    private UserContextRepository userContextRepository;

    @BeforeEach
    void setUpTokenStubs() {
        doReturn(validJwtContext()).when(jwtTokenUtil).validateToken(AUTHORIZATION_TOKEN);
        when(jwtTokenUtil.isAudienceCorrect(AUTHORIZATION_TOKEN, List.of(AUTHORIZED_PARTY_TOKEN_PDC)))
            .thenReturn(true);
        doReturn(validJwtContext()).when(jwtTokenUtil).validateToken(CUSTOMER_AUTHORIZED_PARTY_TOKEN);
        when(jwtTokenUtil.get(CUSTOMER_AUTHORIZED_PARTY_TOKEN, "iss"))
            .thenReturn(Optional.of("https://as.test/auth/realms/" + CUSTOMER_REALM));
        when(jwtTokenUtil.get(CUSTOMER_AUTHORIZED_PARTY_TOKEN, "channel_tp"))
            .thenReturn(Optional.of("test-channel"));
    }

    private JwtContext validJwtContext() {
        JwtContext ctx = Mockito.mock(JwtContext.class);
        when(ctx.isValid()).thenReturn(true);
        return ctx;
    }

    @Test
    void captureAoc() throws Exception {
        when(aocService.initializeAuthorization(any(), any()))
            .thenReturn(AocData.builder().id("aoc-auth-id-123").digest("aoc-digest-abc").build());

        MvcResult result = mockMvc.perform(post("/api/v1/authorizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"AGREEMENTS\"}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + AUTHORIZATION_TOKEN)
                .header("Authorized-Party-Token", CUSTOMER_AUTHORIZED_PARTY_TOKEN))
            .andExpect(status().isOk())
            .andReturn();

        System.out.println("PROVENANCE-CAPTURE-AOC>>>" + result.getResponse().getContentAsString() + "<<<");
    }

    @Test
    void captureAgreements() throws Exception {
        when(agreementsService.getAgreements(any(), any(), any()))
            .thenReturn(List.of(AgreementObject.builder()
                .key("MARKETING")
                .name("Marketing Agreement")
                .status(AgreementStatus.ACCEPTED)
                .version(3)
                .content("Full agreement text")
                .oldContent("Previous agreement text")
                .build()));

        MvcResult result = mockMvc.perform(get("/api/v1/agreements")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + AUTHORIZATION_TOKEN)
                .header("Authorized-Party-Token", CUSTOMER_AUTHORIZED_PARTY_TOKEN))
            .andExpect(status().isOk())
            .andReturn();

        System.out.println("PROVENANCE-CAPTURE-AGREEMENTS>>>" + result.getResponse().getContentAsString() + "<<<");
    }
}
```

---

# S-06 Addendum — BigDecimal Serialization Pin (Partner Money Amounts)

This addendum extends the same provenance discipline to the single genuinely
unproven serialization vector remaining in stream **S-06** (partner/downstream
HTTP contracts): **BigDecimal money-amount rendering** on partner-controller
response DTOs.

## Why BigDecimal needed its own pin

The global web-mapper proof rests on `spring.jackson.use-jackson2-defaults: true`,
which is a **jackson-databind** compatibility flag. BigDecimal *number rendering*
— scale preservation, trailing zeros, plain-vs-scientific notation — is partly
emitted by **jackson-core** (`JsonGenerator.writeNumber(BigDecimal)`), a layer the
databind defaults flag does not necessarily govern. The S-02/03/04 fixtures
contain no BigDecimal field, so this format was unproven until now.

A targeted DTO scan of all partner controllers (InFakt / Leasing / Wadia / NFG /
eBOK24 / LxMedic) found BigDecimal reachable in exactly three endpoints —
InFakt `GET /api/infakt/customer`, Leasing `/customer`, NFG `/customer` — via the
money types `MoneyInfakt`, `leasing.contract.Money`, `nfg.Money`,
`CustomerPrelimit`, `PrelimitDTO`. No custom serializers, no `@JsonFormat`, no
polymorphism, no date fields exist on any partner DTO. Because the render is a
**type-level** behavior of `BigDecimal`, one pin proves all three endpoints.

## Source-unchanged gate

`git diff --stat 8ea55be2 HEAD` over every BigDecimal-bearing type and its
wrapper graph (`MoneyInfakt`, `AccountHeaderInfakt`, `infakt.Customer`,
`nfg.Money/AccountHeader/AccountHeaderNfg/Customer`,
`leasing.contract.Money/AccountHeader/Customer/CustomerPrelimit/PrelimitDTO`)
returned **empty** — all unchanged across the migration. The capture at HEAD is
therefore a valid stand-in for the pre-migration source shape; only the runtime
serializer differs between the two trees.

## Capture facts (verifiable)

- **Commit:** `8ea55be225e0be67927db7ae0834a7837ad5b8b0` (Boot 3.5.13, Java 25),
  confirmed by `git -C ../vassme-35-capture rev-parse HEAD` logged in the same run.
- **Method:** throwaway full-context `@SpringBootTest` (`BigDecimalCapture35IT`)
  in the `git worktree` at `8ea55be2`, extending the 3.5 `SpringBootTestBase`,
  `@MockitoBean InFaktService` stubbed to return a `Customer` whose nested
  `MoneyInfakt.value = new BigDecimal("1234.50")` — a value with non-trivial
  scale and a **significant trailing zero** (the discriminating datum). Request:
  `GET /api/infakt/customer` with `jwt.enabled=false` (the 3.5 `JwtTokenFilter`
  skips validation, request reaches the controller with a null principal; the
  mocked service returns the fixture regardless).
- **Isolation:** captured in the worktree so the Boot 4 tree was never disturbed.
  Worktree removed after capture.
- **Package constraint:** `InFaktService` is package-private to
  `pl.santander.vas.integration.sme.infakt`, so both the capture test and the
  Boot 4 IT live in that package. InFakt was chosen over Leasing/NFG because its
  DTOs are public and only the service is package-private (cleanest placement).

## Measured 3.5 render (the baseline)

```
{... "availableFunds":{"currencyCode":"PLN","value":1234.50} ...}
```

Boot 3.5 / Jackson 2 renders `new BigDecimal("1234.50")` as the **unquoted JSON
number `1234.50`** — scale preserved, trailing zero kept, plain (non-scientific)
notation. (Note: the body includes `null` fields. The controller class-level
`@JsonInclude(NON_NULL)` has no effect on response-DTO serialization — it only
governs the controller object itself — so nulls are genuinely present in both
3.5 and Boot 4 output. The null-inclusion behavior is already in the proven set;
the BigDecimal datum is independent of it.)

## Boot 4 verification (byte-equality)

- **Fixture:** `src/test/resources/regression-proof-safety-net/partners/infakt-customer-bigdecimal-stable-json-contract.json`
  (the verbatim 3.5 body above).
- **IT:** `InFaktCustomerBigDecimalContractIT` (Boot 4 tree, same `infakt`
  package), extends `ProtectedEndpointITBase`, `@MockitoBean InFaktService`,
  builds the identical `Customer` with `new BigDecimal("1234.50")`, hits
  `GET /api/infakt/customer` with the proven valid-auth recipe, asserts full-body
  byte-equality against the fixture.
- **Result:** **GREEN** — `Tests run: 1, Failures: 0`. Boot 4 emits
  `"value":1234.50`, **identical** to the 3.5 baseline.

## Teeth check (positive control)

The fixture's BigDecimal was temporarily mutated to `"value":1234.5` (trailing
zero stripped) and the IT re-run: it **FAILED** as expected
(`expected ...1234.5... but was ...1234.50...`), proving the assertion genuinely
discriminates BigDecimal scale/trailing-zero rendering rather than passing
vacuously. The fixture was then restored to `1234.50` (re-run GREEN).

## Conclusion

**Boot 4 BigDecimal render == Boot 3.5 BigDecimal render** for partner money
amounts (`1234.50`, scale + trailing zero + plain notation preserved). This was
the last genuinely-unproven serialization vector in S-06; with it pinned, the
S-06 residual serialization risk is **zero**. All other S-06 production changes
are the Swagger-2 -> OpenAPI-3 doc-annotation swap (no payload/route/contract
change) and GSON-based downstream clients (different serializer, untouched).
