# EOPIS System State

- **Current Phase**: Phase 10 — Production Incident Simulation & Capstone (Completed)
- **Active Chaos Flags**: Configurable via `application-chaos.yml` / `/actuator/chaos` (e.g. `eopis.chaos.faults.payment-intermittent-failure`, `inventory-lock-contention`)
- **Last Commit**: Pending Phase 10 commit
- **Active Bugs**: Solved & Documented (see `docs/bugs/BUG-001-immutable-bigdecimal-subtotal.md`)
- **Docker Stack Status**: Ready (PostgreSQL 16, pgAdmin 4, Redis 7, Apache Kafka KRaft 3.7, Kafka-UI, Prometheus, Grafana, eopis-app with JVM debug port 5005)
- **Test Status**: All 17 unit and integration tests passing cleanly across all modules
