# EOPIS Local Environment & Debugging Setup Guide

[![Environment: Production-Parity](https://img.shields.io/badge/Environment-Docker%20Compose%20Ready-blue.svg)](#system-requirements--prerequisites)
[![Java 21+](https://img.shields.io/badge/Java-21%2B%20LTS-orange.svg)](https://adoptium.net/)
[![IDE: IntelliJ IDEA](https://img.shields.io/badge/IDE-IntelliJ%20IDEA%202024.1%2B-brightgreen.svg)](https://www.jetbrains.com/idea/)
[![Remote Debug: Port 5005](https://img.shields.io/badge/JDWP%20Debug-Port%205005-purple.svg)](#5-setting-up-intellij-idea-remote-debugger-port-5005)

This guide provides step-by-step instructions for establishing the **Enterprise Order, Payment & Inventory System (EOPIS)** debugging laboratory on your local machine using **IntelliJ IDEA**, **Docker**, and **Java 21+**.

> **Navigation**: [Home](README.md) / **Local Setup Guide** / [Architecture](docs/ARCHITECTURE.md) / [System State](docs/STATE.md) / [Audit Report](audit.md) / [Bug Logs](docs/bugs/BUG-001-immutable-bigdecimal-subtotal.md)

---

## Table of Contents

- [1. System Requirements & Prerequisites](#1-system-requirements--prerequisites)
- [2. Clone the Repository](#2-clone-the-repository)
- [3. Opening and Configuring IntelliJ IDEA](#3-opening-and-configuring-intellij-idea)
- [4. Running the Application](#4-running-the-application)
  - [Mode A: Full Containerized Stack (Recommended)](#mode-a-full-containerized-stack-recommended)
  - [Mode B: Hybrid Local Development](#mode-b-hybrid-local-development)
- [5. Setting Up IntelliJ IDEA Remote Debugger (Port 5005)](#5-setting-up-intellij-idea-remote-debugger-port-5005)
- [6. Accessing Services & Observability Dashboards](#6-accessing-services--observability-dashboards)
- [7. Connecting pgAdmin 4 to PostgreSQL](#7-connecting-pgadmin-4-to-postgresql)
- [8. Running the Automated Test Suite](#8-running-the-automated-test-suite)
- [9. Troubleshooting & FAQ](#9-troubleshooting--faq)

---

## 1. System Requirements & Prerequisites

Verify that the following tools are installed on your workstation:

| Tool | Recommended Version | Verification Command | Official Installation Link |
| :--- | :--- | :--- | :--- |
| **Java JDK** | OpenJDK 21 LTS or newer | `java -version` | [Eclipse Adoptium Temurin](https://adoptium.net/) |
| **Git** | 2.40+ | `git --version` | [Git SCM](https://git-scm.com/) |
| **Docker Desktop** / **OrbStack** | 4.25+ (with Compose v2) | `docker compose version` | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| **IntelliJ IDEA** | 2024.1+ (Community or Ultimate) | — | [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/) |

> [!TIP]
> **Windows Tip**: On Windows 10/11, ensure Docker Desktop is configured to use the **WSL 2 backend** (**Settings → General → Use the WSL 2 based engine**).

> [!NOTE]
> **Multiple Java Versions**: If your default system JDK is older than Java 21 (e.g. Java 17), explicitly configure `JAVA_HOME` before running Maven commands:
> - **macOS / Linux**: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (or point to your JDK 21+ installation directory).
> - **Windows**: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"`

---

## 2. Clone the Repository

Clone the project repository to your local development drive:

```bash
# macOS / Linux (Terminal)
git clone https://github.com/rohitlavate97/java-spring-debugging.git
cd java-spring-debugging
```

```powershell
# Windows (PowerShell)
git clone https://github.com/rohitlavate97/java-spring-debugging.git
cd java-spring-debugging
```

---

## 3. Opening and Configuring IntelliJ IDEA

1. Launch **IntelliJ IDEA**.
2. Click **Open** (or **File → Open...**) and select the `java-spring-debugging` project root folder (or choose `pom.xml`).
3. Select **Open as Project**.
4. Configure the Project SDK:
   - Navigate to **File → Project Structure... → Project** (or press `Ctrl+Alt+Shift+S` / `Cmd+;`).
   - Set **SDK** to **Java 21** (or Java 24/25).
   - Set **Language Level** to **21 - Sealed types, pattern matching, record patterns**.
   - Click **Apply** and **OK**.
5. Enable Annotation Processing:
   - Navigate to **Settings / Preferences → Build, Execution, Deployment → Compiler → Annotation Processors**.
   - Ensure **Enable annotation processing** is checked.
6. Allow IntelliJ to complete Maven indexing and dependency resolution.

---

## 4. Running the Application

You can execute the laboratory in either of two modes:

### Mode A: Full Containerized Stack (Recommended)

This mode runs the entire production topology inside Docker. The application container automatically exposes port `5005` for remote JDWP debugging.

```bash
# 1. Start all container services in background
docker compose up -d

# 2. Verify all containers are healthy
docker compose ps

# 3. Follow live structured JSON application logs
docker compose logs -f eopis-app
```

To gracefully terminate the containers:

```bash
docker compose down
```

---

### Mode B: Hybrid Local Development

If you prefer running Spring Boot directly inside IntelliJ while executing databases and brokers in Docker:

1. **Start infrastructure containers only**:
   ```bash
   docker compose up -d postgres redis kafka
   ```

2. **Run Spring Boot locally**:
   - In IntelliJ IDEA, navigate to [`src/main/java/com/eopis/EopisApplication.java`](src/main/java/com/eopis/EopisApplication.java) and click the green **Run** or **Debug** icon.
   - Alternatively, execute via terminal:
     ```bash
     # macOS / Linux
     ./mvnw spring-boot:run

     # Windows (PowerShell)
     .\mvnw.cmd spring-boot:run
     ```

---

## 5. Setting Up IntelliJ IDEA Remote Debugger (Port 5005)

When executing under **Mode A (Docker Compose)**, attach IntelliJ's interactive debugger to the live container:

1. In IntelliJ IDEA, open the menu: **Run → Edit Configurations...**
2. Click **+ (Add New Configuration)** and select **Remote JVM Debug**.
3. Fill in the configuration details:
   - **Name**: `EOPIS Docker Container (Port 5005)`
   - **Debugger mode**: `Attach to remote JVM`
   - **Host**: `localhost`
   - **Port**: `5005`
   - **Use module classpath**: `eopis-app`
4. Click **Apply** and **OK**.
5. Initiate the debug session:
   - Click **Debug** (or press `Shift+F9`).
   - The IntelliJ Debug Console will print:
     ```text
     Connected to the target VM, address: 'localhost:5005', transport: 'socket'
     ```

> [!IMPORTANT]
> The Docker container starts with `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`. This allows live debugging without blocking application startup.

---

## 6. Accessing Services & Observability Dashboards

Once all containers are running, the following services are available:

| Service | Host URL / Port | Credentials / Purpose |
| :--- | :--- | :--- |
| **EOPIS REST API** | `http://localhost:8080` | Main application API and Swagger/OpenAPI |
| **Actuator Health** | `http://localhost:8080/actuator/health` | Application & dependency liveness/readiness probes |
| **Chaos Fault Toggles** | `http://localhost:8080/actuator/chaos` | View and verify active runtime chaos fault flags |
| **Prometheus Metrics** | `http://localhost:8080/actuator/prometheus`| Raw Prometheus metrics endpoint |
| **pgAdmin 4** | `http://localhost:5050` | `admin@eopis.local` / `admin` (PostgreSQL Database UI) |
| **Kafka UI** | `http://localhost:8085` | Live topic browser, event payload viewer & consumer lag |
| **Prometheus Server** | `http://localhost:9090` | Telemetry scraping engine (5-second scrape interval) |
| **Grafana Dashboards** | `http://localhost:3000` | `admin` / `admin` (Pre-configured JVM, HikariCP, HTTP dashboards) |

---

## 7. Connecting pgAdmin 4 to PostgreSQL

1. Open your browser and navigate to `http://localhost:5050`.
2. Authenticate using:
   - **Email**: `admin@eopis.local`
   - **Password**: `admin`
3. In the left tree, right-click **Servers → Register → Server...**:
   - **General Tab**:
     - **Name**: `eopis-postgres`
   - **Connection Tab**:
     - **Host name/address**: `postgres` (when pgAdmin runs within the Docker network) or `localhost` / `host.docker.internal`
     - **Port**: `5432`
     - **Maintenance database**: `eopis_db`
     - **Username**: `eopis_user`
     - **Password**: `eopis_password`
4. Click **Save**. You can now inspect tables, active connection pools, locks, and run `EXPLAIN ANALYZE` on SQL queries.

---

## 8. Running the Automated Test Suite

Execute the full 24-test integration suite:

```bash
# macOS / Linux (Terminal)
./mvnw clean test

# Windows (PowerShell)
.\mvnw.cmd clean test
```

### Running a Specific Test Class

```bash
# macOS / Linux
./mvnw test -Dtest=OrderServiceIntegrationTest

# Windows (PowerShell)
.\mvnw.cmd test -Dtest=OrderServiceIntegrationTest
```

### Running the Testcontainers Postgres Migration Verification

```bash
./mvnw test -Dtest=FlywayPostgresMigrationIntegrationTest
```

---

## 9. Troubleshooting & FAQ

### Port Conflict (e.g. 5432, 8080, or 6379 already bound)

If an existing PostgreSQL instance, web server, or Redis process occupies a port:

```bash
# macOS / Linux: Identify process using port 5432
lsof -i :5432
kill -9 <PID>

# Windows: Identify process using port 5432
netstat -ano | findstr :5432
taskkill /PID <PID> /F
```

Alternatively, override the host port mapping in `docker-compose.yml` (e.g. `"5433:5432"`).

### Remote Debugger Fails with "Connection Refused"

1. Check that the container is running:
   ```bash
   docker compose ps eopis-app
   ```
2. Verify the JVM debug socket is listening inside the container logs:
   ```bash
   docker compose logs eopis-app | grep JDWP
   ```
   You should observe: `Listening for transport dt_socket at address: 5005`.

### Complete Database Reset & Re-migration

To purge all persistent data volumes and re-apply Flyway migrations from scratch:

```bash
docker compose down -v
docker compose up -d
```
