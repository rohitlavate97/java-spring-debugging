# EOPIS System State & Runtime Dashboard

[![Phase: Post-Audit Remediations](https://img.shields.io/badge/Release%20Phase-Post--Audit%20Remediations-brightgreen.svg)](#system-overview--phase-tracking)
[![Stack Status: Healthy](https://img.shields.io/badge/Docker%20Stack-Healthy%20%26%20Ready-blue.svg)](#infrastructure--container-matrix)
[![Test Suite: Passing](https://img.shields.io/badge/Test%20Suite-23%20Passed%2C%201%20Skipped-success.svg)](#automated-test-suite-status)
[![Active Incidents: 0](https://img.shields.io/badge/Active%20Incidents-0%20(All%20Resolved)-informational.svg)](#incident-and-debugging-history)

> **Navigation**: [Home](../README.md) / [Local Setup](../LOCAL_SETUP.md) / [Architecture](ARCHITECTURE.md) / [Audit Report](../audit.md) / [Bug Logs](bugs/BUG-001-immutable-bigdecimal-subtotal.md)

---

## Table of Contents

- [System Overview & Phase Tracking](#system-overview--phase-tracking)
- [Git Repository Ledger](#git-repository-ledger)
- [Active Chaos Fault Injection Matrix](#active-chaos-fault-injection-matrix)
- [Infrastructure & Container Matrix](#infrastructure--container-matrix)
- [Automated Test Suite Status](#automated-test-suite-status)
- [Incident and Debugging History](#incident-and-debugging-history)
- [Session Bootstrap Checklist](#session-bootstrap-checklist)

---

## System Overview & Phase Tracking

The **Enterprise Order, Payment & Inventory System (EOPIS)** laboratory tracks the state of development, active runtime chaos injections, containerized infrastructure, and test execution through this ledger.

| Attribute | Current Value | Notes |
| :--- | :--- | :--- |
| **Current Milestone** | **Post-Audit Remediations (Fully Completed)** | All 11 findings from [`audit.md`](../audit.md) remediated |
| **Operational Mode** | Production Simulation & Real-Time Debugging Lab | Ready for Level 1–7 debugging scenarios |
| **Active Spring Profiles** | `default` (or `test` during Maven test runs; `chaos` for fault injection) | Profile toggles available in `src/main/resources/` |
| **Chaos Endpoint** | `GET http://localhost:8080/actuator/chaos` | Real-time runtime inspection of active faults |
| **Debug Socket** | `localhost:5005` (JDWP) | Open for remote IntelliJ IDEA debugger attachment |

---

## Git Repository Ledger

- **Current Branch**: `main`
- **Active Release Commit**: [`a0ba436`](https://github.com/rohitlavate97/java-spring-debugging/commit/a0ba436965dd451208003abf6af1e963a0694561)
- **Commit Summary**: `refactor(audit): implement comprehensive audit remediations for rest controllers, missing entities, and structured logging`
- **Repository Tree Status**: Working tree clean; all modules and migrations synchronised.

---

## Active Chaos Fault Injection Matrix

Chaos faults are configured via Spring `@ConfigurationProperties` (`com.eopis.common.chaos.ChaosProperties`) and dynamically intercepted at runtime using AOP (`ChaosFaultInjectorAspect`).

| Fault Identifier | Target Service / Component | Fault Mechanics | Default State | Activation Profile |
| :--- | :--- | :--- | :---: | :--- |
| `payment-intermittent-failure` | `com.eopis.payment.service.PaymentService` | Simulates a 1.5% probabilistic payment gateway failure | Disabled | `application-chaos.yml` (`chaos`) |
| `inventory-lock-contention` | `com.eopis.inventory.service.InventoryService` | Injects 350ms synthetic latency into stock reservation lock | Disabled | `application-chaos.yml` (`chaos`) |
| `order-placement-slowdown` | `com.eopis.order.service.OrderService` | Injects 500ms processing delay into order orchestration | Disabled | `application-chaos.yml` (`chaos`) |

> [!NOTE]
> To activate the chaos fault injection profile during startup, pass `--spring.profiles.active=chaos` or configure `SPRING_PROFILES_ACTIVE=chaos` in `docker-compose.yml`. Inspect current active faults at `http://localhost:8080/actuator/chaos`.

---

## Infrastructure & Container Matrix

When running via `docker compose up -d`, the following 8 container services run continuously:

| Container Service | Image / Base | Host Port | Protocol / Purpose | Healthcheck Status |
| :--- | :--- | :--- | :--- | :--- |
| **`eopis-app`** | Eclipse Temurin 21 (Alpine) | `8080`, `5005` | HTTP REST API + JDWP Remote JVM Debug Socket | Dependent on DB, Redis, Kafka |
| **`postgres`** | `postgres:16-alpine` | `5432` | Primary Relational DB (`eopis_db`) | `pg_isready -U eopis_user` |
| **`pgadmin`** | `dpage/pgadmin4:latest` | `5050` | Web GUI for live SQL and lock inspection | HTTP 200 on port 80 |
| **`redis`** | `redis:7-alpine` | `6379` | Distributed cache and concurrency locks | `redis-cli ping` |
| **`kafka`** | `apache/kafka:3.7.0` | `9092` | KRaft-mode Event Broker (no ZooKeeper) | Broker API versions probe |
| **`kafka-ui`** | `provectuslabs/kafka-ui:latest` | `8085` | Live topic, partition, and consumer lag inspector | HTTP 200 on port 8080 |
| **`prometheus`** | `prom/prometheus:latest` | `9090` | Actuator metrics scraper (5s interval) | HTTP 200 on port 9090 |
| **`grafana`** | `grafana/grafana:latest` | `3000` | Pre-provisioned dashboards (JVM, HikariCP, HTTP) | HTTP 200 on port 3000 |

---

## Automated Test Suite Status

The test suite covers 14 test classes containing 24 test methods across all business domains:

```text
[INFO] Results:
[WARNING] Tests run: 23, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

| Subsystem / Test Suite | Test Class | Coverage Scope | Status |
| :--- | :--- | :--- | :---: |
| **Security & Auth** | [`SecurityIntegrationTest`](../src/test/java/com/eopis/security/SecurityIntegrationTest.java) | JWT generation, filter chain, RBAC endpoint protection | ✅ PASS |
| **Order Orchestration** | [`OrderControllerIntegrationTest`](../src/test/java/com/eopis/order/controller/OrderControllerIntegrationTest.java) | Order creation HTTP POST, validation, DTO mapping | ✅ PASS |
| **Order Pricing** | [`OrderPricingServiceTest`](../src/test/java/com/eopis/order/service/OrderPricingServiceTest.java) | Subtotal calculation, VIP discount, tax & shipping | ✅ PASS |
| **Order Lifecycle** | [`OrderServiceIntegrationTest`](../src/test/java/com/eopis/order/service/OrderServiceIntegrationTest.java) | Transaction boundaries, inventory reservation link | ✅ PASS |
| **Concurrency / Locks** | [`ConcurrentOrderPlacementTest`](../src/test/java/com/eopis/order/service/ConcurrentOrderPlacementTest.java) | High-concurrency stock reservation race protection | ✅ PASS |
| **Event-Driven Kafka** | [`OrderEventIntegrationTest`](../src/test/java/com/eopis/order/event/OrderEventIntegrationTest.java) | Kafka `OrderPlacedEvent` producer and notification log | ✅ PASS |
| **Product Caching** | [`ProductCacheIntegrationTest`](../src/test/java/com/eopis/product/ProductCacheIntegrationTest.java) | Redis `@Cacheable` and `@CacheEvict` consistency | ✅ PASS |
| **Customer Domain** | [`CustomerControllerIntegrationTest`](../src/test/java/com/eopis/customer/controller/CustomerControllerIntegrationTest.java) | REST endpoints, DTO transformation, address mapping | ✅ PASS |
| **Shipment Domain** | [`ShipmentServiceIntegrationTest`](../src/test/java/com/eopis/shipment/ShipmentServiceIntegrationTest.java) | Shipment creation, tracking updates, carrier routing | ✅ PASS |
| **Audit Subsystem** | [`AuditServiceIntegrationTest`](../src/test/java/com/eopis/audit/AuditServiceIntegrationTest.java) | Asynchronous audit log persistence and correlation | ✅ PASS |
| **Fault Injection** | [`PaymentServiceChaosTest`](../src/test/java/com/eopis/payment/PaymentServiceChaosTest.java) | AOP chaos aspect simulation and error propagation | ✅ PASS |
| **Observability** | [`MetricsAndLoggingTest`](../src/test/java/com/eopis/observability/MetricsAndLoggingTest.java) | MDC correlation ID filter & Micrometer custom metrics | ✅ PASS |
| **System Actuator** | [`ActuatorHealthEndpointTest`](../src/test/java/com/eopis/ActuatorHealthEndpointTest.java) | Health indicators, liveness/readiness probes | ✅ PASS |
| **Flyway Schema** | [`FlywayPostgresMigrationIntegrationTest`](../src/test/java/com/eopis/migration/FlywayPostgresMigrationIntegrationTest.java) | Real PostgreSQL Testcontainers schema baseline | 🟡 SKIPPED (Requires Docker) |

---

## Incident and Debugging History

| Incident ID | Target Component | Symptom | Root Cause | Status & Artifacts |
| :--- | :--- | :--- | :--- | :--- |
| **`BUG-001`** | `OrderPricingService` | Order subtotal evaluates to `0.00` | Calling `.add()` on immutable `BigDecimal` discarded return value | • [`Analysis Writeup`](bugs/BUG-001-immutable-bigdecimal-subtotal.md)<br>• [`Scored Report`](debugging-reports/REPORT-001-bigdecimal-subtotal.md) |

---

## Session Bootstrap Checklist

When starting a new debugging session, execute this bootstrap routine:

1. **Verify Git HEAD**:
   ```bash
   git status
   git log -1 --oneline
   ```
2. **Inspect Docker Stack Health**:
   ```bash
   docker compose ps
   ```
3. **Check Active Chaos Configuration**:
   ```bash
   curl -s http://localhost:8080/actuator/chaos | jq .
   ```
4. **Run Regression Test Suite**:
   ```bash
   ./mvnw test
   ```
