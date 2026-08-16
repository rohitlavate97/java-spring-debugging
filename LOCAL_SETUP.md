# EOPIS — Local Environment Setup Guide

This guide provides step-by-step instructions for setting up and running the **Enterprise Order, Payment & Inventory System (EOPIS)** debugging laboratory on your local machine using **IntelliJ IDEA**, **Docker**, and **Java 21+**.

---

## 1. System Requirements & Prerequisites

Ensure the following tools are installed before starting:

| Tool | Recommended Version | Verification Command | Installation Link |
| :--- | :--- | :--- | :--- |
| **Java JDK** | OpenJDK 21 LTS or newer | `java -version` | [Eclipse Adoptium Temurin](https://adoptium.net/) |
| **Git** | 2.40+ | `git --version` | [Git SCM](https://git-scm.com/) |
| **Docker Desktop** | 4.25+ (with Compose v2) | `docker compose version` | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| **IntelliJ IDEA** | 2024.1+ (Community or Ultimate)| — | [JetBrains IntelliJ IDEA](https://www.jetbrains.com/idea/) |

> **Windows Tip**: On Windows 10/11, ensure Docker Desktop is configured to use the **WSL 2 backend** (Settings → General → Use the WSL 2 based engine).

---

## 2. Clone the Repository

```powershell
# Clone the project repository
git clone https://github.com/rohitlavate97/java-spring-debugging.git

# Navigate into the project root directory
cd java-spring-debugging
```

---

## 3. Opening the Project in IntelliJ IDEA

1. Launch **IntelliJ IDEA**.
2. Click **Open** (or **File → Open...**) and select the `java-spring-debugging` root folder (or choose `pom.xml`).
3. Click **Open as Project**.
4. Configure Project SDK:
   - Go to **File → Project Structure... → Project** (or press `Ctrl+Alt+Shift+S`).
   - Set **SDK** to **Java 21** (or Java 25).
   - Set **Language Level** to **21 - Sealed types, pattern matching, record patterns**.
   - Click **Apply** and **OK**.
5. Allow IntelliJ to finish indexing and downloading Maven dependencies.

---

## 4. Running the Application

You can run the laboratory in either of two modes:

### Mode A: Full Containerized Stack (Recommended for Production Parity)

This mode starts the application alongside PostgreSQL, Redis, Kafka, Kafka UI, Prometheus, Grafana, and pgAdmin in Docker. The application container exposes port `5005` for remote debugging.

```powershell
# Start all containers in the background
docker compose up -d

# Verify that all containers are healthy
docker compose ps

# Follow application logs in real-time
docker compose logs -f eopis-app
```

To stop the containers:
```powershell
docker compose down
```

---

### Mode B: Hybrid Local Development (App on Host, Dependencies in Docker)

If you prefer running Spring Boot directly inside IntelliJ while running databases and brokers in Docker:

1. **Start only infrastructure containers:**
   ```powershell
   docker compose up -d postgres redis kafka
   ```

2. **Run Spring Boot locally:**
   - In IntelliJ, open [`src/main/java/com/eopis/EopisApplication.java`](file:///D:/Projects/Spring%20Boot/java-spring-debugging/src/main/java/com/eopis/EopisApplication.java) and click the green **Run** or **Debug** button.
   - Or via terminal:
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```

---

## 5. Setting Up IntelliJ IDEA Remote Debugger (Port 5005)

When running under **Mode A (Docker Compose)**, connect IntelliJ to the live container:

1. In IntelliJ IDEA, go to **Run → Edit Configurations...**
2. Click **+ (Add New Configuration)** and choose **Remote JVM Debug**.
3. Configure the settings:
   - **Name**: `EOPIS Docker Container (Port 5005)`
   - **Debugger mode**: `Attach to remote JVM`
   - **Host**: `localhost`
   - **Port**: `5005`
   - **Use module classpath**: `eopis-app`
4. Click **Apply** and **OK**.
5. Start debugging by pressing **Debug** (or `Shift+F9`).
6. IntelliJ will output: `Connected to the target VM, address: 'localhost:5005', transport: 'socket'`.

---

## 6. Accessing Services & Dashboards

| Service | URL / Port | Credentials / Purpose |
| :--- | :--- | :--- |
| **EOPIS REST API** | `http://localhost:8080` | Main application endpoint |
| **Actuator Health** | `http://localhost:8080/actuator/health` | Application & dependency healthcheck |
| **Chaos Toggles** | `http://localhost:8080/actuator/chaos` | View active runtime chaos fault injection flags |
| **pgAdmin 4** | `http://localhost:5050` | `admin@eopis.local` / `admin` (Database UI) |
| **Kafka UI** | `http://localhost:8085` | Live topic viewer and consumer lag inspector |
| **Prometheus** | `http://localhost:9090` | Scrapes JVM, HikariCP, and custom metrics every 5s |
| **Grafana** | `http://localhost:3000` | `admin` / `admin` (Pre-configured EOPIS dashboards) |

---

## 7. Connecting pgAdmin 4 to PostgreSQL

1. Open `http://localhost:5050` in your browser.
2. Log in with:
   - **Email**: `admin@eopis.local`
   - **Password**: `admin`
3. Click **Add New Server**:
   - **General Tab → Name**: `eopis-postgres`
   - **Connection Tab**:
     - **Host name/address**: `postgres` (if using Docker network) or `localhost` / `host.docker.internal`
     - **Port**: `5432`
     - **Maintenance database**: `eopis_db`
     - **Username**: `eopis_user`
     - **Password**: `eopis_password`
4. Click **Save**. You can now browse tables, indexes, and active transactions.

---

## 8. Running Automated Tests

Run the full 17-test integration suite:

```powershell
# Run all unit and integration tests via Maven Wrapper
.\mvnw.cmd clean test
```

Run a specific test class:
```powershell
.\mvnw.cmd test -Dtest=OrderServiceIntegrationTest
```

---

## 9. Troubleshooting & FAQ

### Port Already in Use (e.g. 5432 or 8080)
If you already have a local PostgreSQL or Tomcat service running:
```powershell
# Check which process is using the port (e.g., 5432)
netstat -ano | findstr :5432

# Or change the host port mapping in docker-compose.yml (e.g. "5433:5432")
```

### Remote Debugger Fails with "Connection Refused"
- Ensure `docker compose ps` shows `eopis-app` in `running` state.
- Check container logs: `docker compose logs eopis-app` to ensure the JVM has started with `-agentlib:jdwp=...`.

### Resetting the Database and Environment
To completely wipe all data volumes and re-run Flyway migrations from scratch:
```powershell
docker compose down -v
docker compose up -d
```
