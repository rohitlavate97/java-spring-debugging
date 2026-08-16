# EOPIS System State

- **Current Phase**: Post-Audit Remediations (Fully Completed)
- **Active Chaos Flags**: Configurable via `application-chaos.yml` / `/actuator/chaos` (e.g. `eopis.chaos.faults.payment-intermittent-failure`, `inventory-lock-contention`)
- **Last Commit**: Pending Audit Remediation commit
- **Active Bugs**: Solved & Documented (see `docs/bugs/BUG-001-immutable-bigdecimal-subtotal.md` and `docs/debugging-reports/REPORT-001-bigdecimal-subtotal.md`)
- **Docker Stack Status**: Ready (PostgreSQL 16, pgAdmin 4, Redis 7, Apache Kafka KRaft 3.7 with healthcheck, Kafka-UI, Prometheus, Grafana, eopis-app with JVM debug port 5005)
- **Test Status**: All 21 unit and integration tests passing cleanly across Web Controllers, Security, Caching, Concurrency, Kafka, Shipment, Audit, Observability, and Flyway Migrations
