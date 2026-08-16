# EOPIS Project Audit

**Date:** 2026-08-16
**Scope:** Full repository at `D:\Projects\Spring Boot\java-spring-debugging`, audited against `enterprise-java-debugging-lab-master-prompt.md` (the governing spec for this project).
**Method:** Static review of source tree, docs, Docker/Maven/monitoring config, git history, plus a live build/test run (`mvnw compile`, `test-compile`, and a sample of unit/integration tests against H2).

**Headline:** The infrastructure layer (Docker Compose, Prometheus/Grafana, Maven deps, CI, chaos scaffolding) is solid and matches the spec closely. The gap is almost entirely in the **application/domain layer never reaching a usable, testable state**: there is no HTTP API for any business module except login, six spec-named entities were never implemented despite their tables existing in the DB schema, and the documentation/reporting trail required by the spec (Section 27–28) was written once in Phase 1 and never updated as the project diverged from that plan. None of this breaks the build — everything compiles and the sampled tests pass — but a learner cannot currently exercise the "HTTP request → Filter → Security → Controller → Service → Repository → Hibernate" debugging path (spec Section 8) for anything except authentication.

---

## 1. Critical — No REST API for any business domain

Only one controller exists in the entire codebase:

```
src/main/java/com/eopis/security/controller/AuthController.java   (POST /api/auth/login)
```

There is **no controller** for `customer`, `product`, `inventory`, `order`, `payment`, or `notification`. This directly blocks spec Section 8 ("Teach the request path: HTTP request → Servlet Filter → Spring Security → Controller → Validation → Service → `@Transactional` → Repository → Hibernate → JDBC → PostgreSQL") for every domain except login, and it means `SecurityConfig`'s `anyRequest().authenticated()` and `/api/admin/**` rules are currently untestable through real endpoints.

**Fix:** Add at minimum `OrderController`, `ProductController`, `InventoryController`, `CustomerController`, `PaymentController` with the CRUD/workflow operations the existing services already implement (`OrderServiceImpl`, `ProductServiceImpl`, `InventoryService`, `PaymentServiceImpl` all have business logic with no HTTP entry point). This is the single highest-leverage fix — it unblocks Sections 8, 9 (breakpoints on real request flow), 17 (testing RBAC against real endpoints), and most future bug exercises, which all assume a request path exists.

---

## 2. Critical — Six spec-named entities never implemented; dead schema in the database

`docs/db/migration/V1__init_schema.sql` defines tables for `coupons`, `payment_transactions`, `refunds`, `shipments`, `shipment_tracking`, and `audit_logs` — but **no corresponding `@Entity`, repository, service, or controller exists** for any of them. `Order.java` even has a lone `couponId` column referencing the nonexistent `Coupon` entity. The entire `shipment` and `audit` modules named in spec Section 5 don't exist as Java packages.

**Fix:** Either (a) implement the missing entities/modules to match the schema and `docs/ARCHITECTURE.md`, or (b) if shipment/audit/coupon/refund are intentionally deferred to a later phase, drop those tables from `V1__init_schema.sql` for now and add them in the migration for whichever phase actually implements them — a schema with six dead tables and no code touching them is confusing for a learner trying to reason about what's "intentional bug" vs. "unfinished."

---

## 3. High — Documentation frozen at Phase 1, contradicts current code

`docs/ARCHITECTURE.md` was written in commit `504fb58 docs(phase-1): propose architecture and state tracking` and has **never been amended** despite 9 subsequent feature-phase commits, violating spec Section 3 ("documented once in `docs/ARCHITECTURE.md` and only amended, never rewritten") and Section 28. Concretely, it still documents:
- `shipment` and `audit` modules with full controller/service/entity breakdowns that don't exist in code.
- `customer/controller`, `product/controller` packages that don't exist (only `security/controller` exists).
- Detailed field lists for `Coupon`, `Promotion`, `PaymentTransaction`, `Refund` entities that were never created.
- A claim that logs are output via "Logback JSON encoder" with a sample JSON line — **not implemented** (see Finding 6).

`docs/STATE.md` still has a literal placeholder: `Last Commit: Pending Phase 10 commit` — never filled in with a real hash, and now two doc commits stale versus current HEAD. It also claims *"Test Status: All 17 unit and integration tests passing cleanly across all modules"* — inaccurate, since `customer`, `inventory`, and `notification` have zero tests (see Finding 7).

**Fix:** Do a pass over `docs/ARCHITECTURE.md` to either implement or remove the sections describing shipment/audit/coupon/refund, and correct the controller-package claims. Update `docs/STATE.md` with the actual current commit hash and an accurate test-status line whenever it's next touched.

---

## 4. High — `docs/debugging-reports/` and per-bug writeups required by Section 28 are almost entirely missing

Spec Section 28 requires:
- `docs/bugs/BUG-XXX-slug.md` — one file per solved bug. **Only one exists**: `BUG-001-immutable-bigdecimal-subtotal.md`, despite the chaos system, Redis locking, Kafka consumer, and payment services (phases 6–10) each implying solvable bug scenarios per the spec's own design.
- `docs/debugging-reports/` — one scored report per exercise (Section 20's rubric). **This directory doesn't exist at all.**

**Fix:** If this is a solo learning project where scoring reports genuinely don't apply, that's a reasonable deliberate deviation — but it should be a documented decision (e.g., a line in `docs/STATE.md`) rather than a silent gap, since the spec treats it as a required deliverable per phase.

---

## 5. High — Chaos/Bug Injection System is a thin, un-targeted approximation of the spec

`common/chaos/{ChaosProperties, ChaosFaultInjectorAspect, ChaosEndpoint}` implements only two fault *shapes* — a flat `latencyMs` and a flat `failureRatePercent` — versus the spec's bug-ID-keyed model (`eopis.chaos.lazy-loading-001.enabled=true`) that ties a specific toggle to a specific named bug.

More significantly, `ChaosFaultInjectorAspect`'s pointcut is `execution(* com.eopis..service.*.*(..))` — it matches **every service method across the whole codebase**, and then iterates over *all* configured faults on every call regardless of which fault key was meant for which service. In practice this means a fault configured under the key `payment-intermittent-failure` will also probabilistically fire against unrelated `InventoryService`/`OrderService` calls, since nothing in the aspect scopes a fault to the class/method it's named after.

Only 3 faults are configured (`application-chaos.yml`): `payment-intermittent-failure`, `inventory-lock-contention`, `order-placement-slowdown` (disabled). None of the spec's called-out bug classes — lazy loading, wrong cascade type, N+1 — are represented, since those require code-shape bugs (the spec's own documented fallback for non-toggleable bugs), and no such branch/commented-block mechanism was found either.

**Fix:** Scope the `@Around` pointcut per fault (e.g. match on a custom annotation or a class-name-to-fault-key lookup) so faults only affect their intended target, and add fault keys with actual bug-ID naming (`BUG-002-...`) rather than generic descriptive names, so `GET /actuator/chaos` output is traceable back to a specific `docs/bugs/BUG-XXX` writeup once solved.

---

## 6. Medium — Structured JSON logging is documented but not implemented

`common/logging/CorrelationIdFilter.java` correctly puts `correlation_id`/`request_id` into MDC per request — the correlation-ID mechanism itself is real and works. But spec Section 3 requires **structured JSON** log output, and `docs/ARCHITECTURE.md` explicitly claims a "Logback JSON encoder" is wired in. In reality, `application.yml`'s `logging.pattern.console` is a plain-text pattern string; there's no `logback-spring.xml`, no Spring Boot 3.4 `logging.structured.format.*` property, and no JSON encoder dependency (e.g. `logstash-logback-encoder`) in `pom.xml`.

**Fix:** Either add `logging.structured.format.console=ecs` (Spring Boot 3.4+ has this built in, no extra dependency needed) or a `logback-spring.xml` with a JSON encoder, and correct the ARCHITECTURE.md claim to match whichever is actually done. This is needed for Section 10's "log-only debugging" exercises to work as designed (structured fields, not just a formatted string).

---

## 7. Medium — Test coverage gaps and unused Testcontainers dependency

- **Zero tests** for `customer`, `inventory`, and `notification` modules — `InventoryService` contains the core stock-allocation/oversell-prevention logic and is only exercised indirectly via `ConcurrentOrderPlacementTest`, never directly unit-tested.
- `pom.xml` declares `testcontainers-bom` plus `junit-jupiter`/`postgresql` Testcontainers artifacts, but a full-repo search found **zero** `@Container`/`Testcontainers` usage anywhere in `src/test`. All tests run against H2 in-memory with **Flyway disabled** (`application-test.yml`: `flyway.enabled: false`, `ddl-auto: create-drop`) — meaning the real Postgres schema (`V1__init_schema.sql`) is never actually validated by the test suite; Hibernate generates its own schema from entities instead, which can silently diverge from the Flyway migration over time.

**Fix:** Add at least one Testcontainers-backed integration test that boots against real Postgres with Flyway migrations applied (this is explicitly named in spec Section 23 as a debugging-training tool, not optional polish), and add basic unit tests for `InventoryService` and `CustomerService`/`NotificationConsumer`.

---

## 8. Medium — Seed data volume far short of spec

Spec Section 6 requires seed data to "scale to realistic debugging volume: thousands of customers, thousands of products, many orders, multiple warehouses." `DataSeeder.java` currently seeds only 3 customers, 4 products, 2 warehouses — meaningful realistic-ID and volume-based debugging exercises (Section 22, e.g. spotting a slow query only visible at scale) aren't yet possible.

**Fix:** Extend the seeder to generate on the order of thousands of records (e.g. via `net.datafaker` or a simple loop with varied realistic data), gated behind a profile/flag so normal dev startup isn't slowed down.

---

## 9. Low — Docker Compose: Kafka has no healthcheck, and `eopis-app` doesn't wait for it

Spec Section 3 requires all services (explicitly including "Kafka broker ready") to reach a healthy state before an exercise begins. `docker-compose.yml`'s `kafka` service has no `healthcheck` block, and `eopis-app`'s `depends_on.kafka` uses `condition: service_started` rather than `service_healthy` — so the app can start before Kafka is actually accepting connections, which will surface as flaky/nondeterministic startup-time Kafka connection errors rather than the intended debugging scenarios.

**Fix:** Add a healthcheck to the `kafka` service (e.g. `kafka-broker-api-versions.sh --bootstrap-server localhost:9092` or an equivalent probe for the `apache/kafka:3.7.0` image) and change `eopis-app`'s dependency condition to `service_healthy`.

---

## 10. Low — Dead Lombok property in `pom.xml`

`pom.xml` declares `<lombok.version>1.18.36</lombok.version>` in `<properties>` but there is no actual `lombok` `<dependency>` block anywhere — the property is unused. Not a functional bug (no code currently uses Lombok annotations), just a small piece of leftover config.

**Fix:** Either add the dependency if Lombok is intended for use, or remove the dangling property.

---

## 11. Low — Commit-message convention drift on the two most recent commits

Commits through Phase 10 (`0509e70`) consistently follow `type(phase-N): description` per spec Rule 10. The two most recent commits (`82c7e4d docs: update README.md...`, `82199bc docs: add LOCAL_SETUP.md...`) drop the phase scope. Minor, but worth flagging since Rule 10 exists specifically so `git bisect`-style exercises can rely on consistent commit semantics.

---

## What's already working well (no action needed)

- **Docker Compose stack** matches spec Section 3's diagram 1:1 — Postgres, Redis, Kafka (KRaft), Kafka UI, pgAdmin, Prometheus, Grafana, and `eopis-app` with the JDWP debug port (5005) exposed via the Dockerfile's `JAVA_TOOL_OPTIONS`. No missing or extraneous services.
- **`pom.xml`** covers every dependency the spec's Section 2 stack requires (Spring Boot 3.4.2, Java 21, Spring Data JPA, Postgres driver, Redis, Kafka, Flyway, Spring Security + JJWT, Actuator, Micrometer/Prometheus, Testcontainers present even if unused in tests).
- **Dockerfile** is a proper multi-stage build (Maven build stage → `eclipse-temurin:21-jre-alpine` runtime stage), runs as a non-root user, exposes both the app and debug ports.
- **Grafana/Prometheus** are genuinely pre-provisioned, not just configured: one datasource, one dashboard JSON with 4 real panels (JVM memory, HTTP latency P95/P99, HikariCP pool, custom business metrics), scraping `/actuator/prometheus` every 5s.
- **RBAC/Security** (`JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, `User`/`Role`/`Permission` with proper join tables, `CustomUserDetailsService` correctly prefixing `ROLE_`) is coherent and verified end-to-end by `SecurityIntegrationTest` — this is one of the more complete slices of the domain, aside from lacking business controllers to protect.
- **CI pipeline** (`.github/workflows/ci.yml`) does a real build → test on every push/PR with JDK 21 + Maven cache; matches spec Section 26's minimum bar.
- **Build health**: `mvnw compile` and `test-compile` both succeed cleanly (60 + 10 source files, zero warnings), and a sample of unit/integration tests (`OrderPricingServiceTest`, `ActuatorHealthEndpointTest`, `MetricsAndLoggingTest`) all pass — the project is in a genuinely runnable state per Rule 5, it just doesn't yet cover the full domain surface.
- **Optimistic locking** (`@Version`) and cascade/orphanRemoval mappings on `Order`, `Customer`, `Inventory`, `Product`, `Payment`, `User` are correctly applied where they exist.
- **No TODO/FIXME/stub markers** anywhere in `src/main/java` — what has been built is genuinely finished, not scaffolded and abandoned mid-method.

---

## Suggested priority order for next work session

1. Add controllers for `order`, `product`, `inventory`, `customer`, `payment` (Finding 1) — unblocks the most spec sections at once.
2. Decide shipment/audit/coupon/refund: implement or prune the dead schema (Finding 2).
3. Refresh `docs/ARCHITECTURE.md` and `docs/STATE.md` to match reality (Finding 3).
4. Scope the chaos aspect per-fault and add 2–3 more bug-ID-keyed faults + matching `docs/bugs/BUG-00X-*.md` writeups (Findings 4–5).
5. Everything else (JSON logging, test gaps, seed volume, compose healthcheck, Lombok cleanup, commit convention) can be picked up incrementally without blocking the above.
