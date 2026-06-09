# Przewodnik review migracji Boot 4

## 1) TL;DR + zakres

**Zakres:** migracja platformy **Spring Boot 3.5.13 -> 4.0.6**, wyrównanie Spring Cloud oraz **Jackson 2->3**.

**Puenta dla recenzenta:** realna powierzchnia zmian behawioralnych to **5 plików prod-behavioral + 1 plik dev-config** (około **~79 linii**).  
Reszta `base..HEAD` (`117 files, +7531/-360`) to:
- mechanika migracji (package/import churn, zamiana Swagger->OpenAPI),
- testy safety-net (`+2463/-177`),
- scaffolding/provenance w `context/` (`+4847/-0`) wyekstrahowany z finalnego payloadu.

Dodatkowo: **33 mechaniczne pliki PROD** to zmiany package/import/annotacji bez intencjonalnej zmiany logiki biznesowej.  
`external/aoc` (5) + `external/wksme1` (2) to aktualizacje modeli/annotacji/importów, poza ścieżką decyzji o bridge Jacksona.

## 2) Levery zachowania kompatybilności (dlaczego teza o neutralności jest wiarygodna)

1. **Globalny lever webowy:** `spring.jackson.use-jackson2-defaults: true` w `application.yml`.
2. **Shim kompatybilności dla injected-mapper:** bridge bean `ObjectMapper` dla ścieżek direct-injection; to mechanizm kompatybilności dla tych callerów, **nie** dowód pokrycia kontraktów HTTP.
3. **Uzasadnione piny/koordynaty w POM:** rationale i ograniczenia są udokumentowane w `compatibility-decisions.md`.
4. **Model dowodowy:** zachowanie Boot 4 weryfikowane względem fixture’ów/provenance z Boot 3.5 (a nie Boot4-vs-Boot4).

## 3) Pliki behavioral-risk (z dowodem, bez deklaratywności)

| Plik | Co się zmieniło | Dlaczego bezpieczne / status dowodu |
|---|---|---|
| `src/main/java/pl/santander/vas/config/ObjectMapperConfig.java` | Bridge bean zwraca `com.fasterxml.jackson.databind.json.JsonMapper` (podtyp `ObjectMapper`) z `disable(SORT_PROPERTIES_ALPHABETICALLY)` | **Uczciwy zakres:** bridge **nie** jest dowodem pokrycia kontraktów HTTP. Pokrycie HTTP pochodzi z kontraktowych IT + baseline 3.5 na ścieżce web-converter. Dla samej ścieżki bridge: jedyny potwierdzony konsument to `CekeContextSource` (deserialize-only do scalar DTO), więc usunięcie `Jdk8Module` jest tam nieistotne (brak typów Jdk8/Optional). |
| `src/main/resources/application.yml` | `spring.jackson.use-jackson2-defaults: true` | **Udowodnione:** wektory serializacji przypięte do baseline 3.5 (`baseline-provenance.md`) i domknięte w weryfikacji (`verification-closed.md`), w tym ordering/null/BigDecimal. |
| `src/main/resources/application-dev.yml` | Konfiguracja dev: `mors.queue.enabled: false` + zmiana klucza `contextPath` -> `server.servlet.context-path` | **To nie jest niezależne ryzyko prod-runtime.** `mors.queue.enabled` to devowa strona tej samej bramki RabbitMQ egzekwowanej przez pliki poniżej (`@ConditionalOnProperty(..., matchIfMissing=true)`). `context-path` jest dev-scoped i zachowuje wartość (`/`). |
| `pom.xml` | aktualizacje wersji/koordynatów + surefire IT gate + ścieżka koordynatu validatora | **Oparte o dowody:** rationale kompatybilności jest w promowanym `compatibility-decisions.md`; intencja IT-gate jest jawna w komentarzach POM i urealnia safety-net w obecnym command path CI. |
| `src/main/java/pl/santander/vas/dictionaries/mors/adapter/mors/MorsQueueListener.java` | `@ConditionalOnProperty(..., matchIfMissing = true)` | **Kodowo bezpieczny default:** `matchIfMissing=true` utrzymuje domyślne zachowanie przy braku flagi. |
| `src/main/java/pl/santander/vas/dictionaries/mors/infrastructure/MorsRabbitMQConnectionConfiguration.java` | analogiczna bramka warunkowa | **Kodowo bezpieczny default:** ten sam argument `matchIfMissing=true`. |

Faktyczny diff `context-path` w `application-dev.yml`:

```diff
- server:
-   contextPath: /
+ server:
+   servlet:
+     context-path: /
```

### 3a) Ręczne mappery poza globalnym leverem webowym — tabela domknięcia

| Caller | Linia Jacksona | Serialize/Deserialize | Ścieżka | Finalny werdykt |
|---|---|---|---|---|
| `CekeContextSource` | `com.fasterxml` injected `ObjectMapper` bean | Deserialize-only (`readValue`) | wewnętrzna ścieżka token-validation | **Bezpieczny przez kod** (scalar DTO; brak wrażliwości na Jdk8/Optional) |
| `ContextResolver` | `com.fasterxml` lokalne `new ObjectMapper()` | Deserialize-only | parsowanie kontekstu z principal/header | **Bezpieczny przez kod** (to nie jest ścieżka serializacji outputu) |
| `WebAnalyticsQueueService` | `com.fasterxml` lokalne `new ObjectMapper()` | Serialize payloadów wychodzących na kolejkę | kolejka/integracja wewnętrzna | **Bezpieczny przez niezmienność**: zero diff vs `8ea55be2` dla serwisu i serializowanych DTO |
| `JwtTokenSpringUtil` | `com.fasterxml` lokalne `new ObjectMapper()` | wewnętrzny write+read round-trip | auth internals | **Bezpieczny przez kod** (wewnętrzna transformacja, bez zewnętrznego kontraktu payloadu) |

**Wniosek:** wszystkie cztery ścieżki są na `com.fasterxml` (linia J2 API) i żadna nie wnosi obawianego lokalnego dryfu J3 serializerów.

## 4) Trzy świadome akceptacje recenzenta

1. **Surefire IT-gate w `pom.xml`:** `*IT` uruchamia się w aktualnej ścieżce CI (`mvn package`), więc safety-net behawioralny nie jest już pomijany.
2. **Martwy `@SecureWithTokens` jest pre-existing:** eskalowany osobno, niewprowadzony przez tę migrację.
3. **Safety-net jest test-only / zero-prod:** kontrakty/smoke/fixtury to warstwa weryfikacji, nie zmiana funkcjonalna runtime.

## 5) Co zweryfikowano i jak

- Wektory serializacji zostały przypięte do capture z Boot 3.5 i porównane na Boot 4 (pełna ścieżka provenance w dokumencie baseline).
- Pokryte wektory obejmują: field ordering, naming, null handling, record output, enum/property mapping, BigDecimal rendering.
- Residuale zostały jawnie sklasyfikowane (w tym pre-existing SFTP gap i zaakceptowany residual Double) w zamkniętej weryfikacji.

Kluczowe artefakty dowodowe:
- `baseline-provenance.md` (provenance capture z Boot 3.5 i lineage fixture’ów),
- `verification-closed.md` (werdykt końcowy + dowody per-wektor),
- `compatibility-decisions.md` (rationale wersji/pinów/koordynatów).

## 6) Jak to recenzować efektywnie

1. Zacznij od **Behavior-preserving levers** (sekcja 2) — ustawia model ryzyka.
2. Przejdź linia po linii przez **pliki behavioral-risk** (sekcja 3).
3. Zweryfikuj **tabelę domknięcia ręcznych mapperów** (sekcja 3a) — odpowiada na pytanie „co poza web-leverem”.
4. Zrób spot-check safety-netu na reprezentatywnych kontraktach (CODE/AOC/Agreements + BigDecimal).
5. Na końcu potwierdź 3 świadome akceptacje (IT gate, pre-existing `@SecureWithTokens`, test-only charakter zmian).

## Plan ekstrakcji context/ (wykonany na tym branchu)

Promowane do review-facing (`docs/review/boot4-migration/`):
1. `baseline-provenance.md`
2. `verification-closed.md`
3. `compatibility-decisions.md`
4. `MIGRATION.md`

Usunięte z payloadu review:
- scaffolding `context/`
- `AGENTS.md`
