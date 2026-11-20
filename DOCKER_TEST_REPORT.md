# FinCore Docker Deployment & Integration Testing Report

**Execution Timestamp:** 2026-11-20T22:30:00+05:30<br/>
**Target Environment:** Local Windows Host + Docker Desktop (Linux Containers)<br/>
**Evaluator:** Automated Repository Inspection & Runtime Verification<br/>
**Repository Path:** `.`

---

## A. Executive Summary

A comprehensive inspection, hardening verification, and runtime integration testing of the FinCore Spring Boot microservices platform and its running Docker deployment was executed. The testing adhered strictly to all safety mandates: execution of non-mutating test cases only, zero schema modifications, row count invariance across all database tables, and absolute zero exposure of credentials or sensitive customer data.

All six microservice containers are actively running in Docker `Up` state (note: Docker engine `healthy` state is not reported as native Compose healthchecks are not yet configured). Service discovery via Netflix Eureka is active with all five client microservices reporting `UP`. All four database-backed services (`customer-service`, `auth-service`, `account-service`, and `transaction-service`) connect to PostgreSQL 18.6 on the Windows host via `host.docker.internal:5432`, and all four initialize cleanly under Hibernate schema validation (`validate` mode).

Targeted deployment improvements have been verified in the codebase:
1. **Public Auth Gateway Routing**: Gateway routes for public authentication endpoints (`POST /auth/register`, `POST /auth/login`, `POST /auth/customer-verification/send`, `POST /auth/customer-verification/verify`) are implemented in [JwtGatewayConfig.java](api-gatway/src/main/java/com/fincore/api_gatway/config/JwtGatewayConfig.java), forwarding requests via Eureka load balancing while strictly blocking `/auth/internal/**`. Live tests confirm requests reach `auth-service` (e.g., `POST /auth/login` with an empty JSON body returns an HTTP 400 validation error from the controller rather than a 404 route error).
2. **Explicit JWT Signing**: `auth-service` explicitly specifies `Jwts.SIG.HS384` during token generation in [JwtService.java](auth-service/src/main/java/com/fincore/auth_service/service/JwtService.java), ensuring algorithm consistency with downstream microservices.
3. **Container Security & Context Filtering**: All six Dockerfiles implement a dedicated non-root `fincore` system user and group (`USER fincore`), and `.dockerignore` files are present across root and all service build contexts.
4. **Log Optimization & Documentation**: Verbose Hibernate SQL logging in Docker is disabled via `SPRING_JPA_SHOW_SQL: "false"`, and complete Docker setup documentation has been added to [README.md](README.md).
5. **Testing Scope Notice**: Container image builds in Dockerfiles utilized `-DskipTests`; Maven test suites were not executed during image build. Stateful business workflows (user creation, login with valid credentials, KYC OTP delivery, and money transfer sagas) were not executed in order to preserve existing host database records.

### Test Metrics

| Status | Count | Percentage |
|---|---:|---:|
| **PASS** | 39 | 92.9% |
| **FAIL** | 0 | 0.0% |
| **WARNING** | 1 | 2.4% |
| **BLOCKED** | 0 | 0.0% |
| **NOT TESTED** | 2 | 4.8% |
| **TOTAL TESTS** | 42 | 100.0% |

### Critical Issues & Technical Findings Summary
1. **Security Finding (Historical Git Commits)**: While working-tree files (`.gitignore`, `application.yml`, `.env.example`, `compose.yaml`) properly protect credentials, Git history commits (`baef723`, `acabb49`) previously contained credentials in this public repository. Secrets remain visible in historical commits until Git history rewriting is performed. Normal commits and pushes do not scrub historical commits. Note: The Windows PostgreSQL password has already been rotated; rotation of JWT/internal secrets is recommended only if active values deployed in `.env` have not already been replaced with fresh keys.
2. **Operational Improvement (Healthchecks)**: Automated healthchecks using Spring Boot Actuator (`/actuator/health`) are not yet defined in [compose.yaml](compose.yaml). Services rely on restart policies without container-level readiness gating.
3. **Internal Feign Direct Verification (COMM-01)**: Live verification was successfully executed directly against `account-service:8082` within the container network using an authenticated request with `USER` claims for a pre-seeded test profile. The call validated Eureka dynamic resolution of `auth-service`, internal `X-Internal-Secret` header validation, and payload deserialization returning HTTP 200 with linked customer accounts, while preserving database row counts.

---

## B. Test Results Table

| Test ID | Test Area | Test Description | Method | Expected Result | Actual Result | Status | Evidence |
|---|---|---|---|---|---|---|---|
| **IMG-01** | Docker | Verify Java 21 build stage image across all Dockerfiles | Source Inspection | Multi-stage build with `maven:3.9.11-eclipse-temurin-21 AS build` | All 6 Dockerfiles use `maven:3.9.11-eclipse-temurin-21 AS build` | **PASS** | `api-gatway/Dockerfile`, `customer-service/Dockerfile`, etc. |
| **IMG-02** | Docker | Verify Java 21 runtime stage image across all Dockerfiles | Source Inspection | Runtime image `eclipse-temurin:21-jre` | All 6 Dockerfiles use `eclipse-temurin:21-jre` | **PASS** | `account-service/Dockerfile`, `transaction-service/Dockerfile`, etc. |
| **IMG-03** | Docker | Verify presence of all 6 expected Docker images | CLI Command | 6 tagged FinCore images exist locally | 6 images found (`fincore-gateway:1.0`, `fincore-customer:1.0`, `fincore-auth:1.0`, `fincore-account:1.0`, `fincore-transaction:1.0`, `fincore-eureka:1.0`) | **PASS** | `docker images \| findstr fincore` |
| **IMG-04** | Docker | Verify container execution non-root user | Source & CLI | Container runs as non-privileged system user | All 6 Dockerfiles configure dedicated system group/user `fincore`, JAR copied with `--chown=fincore:fincore`, and switch to `USER fincore` | **PASS** | All 6 `Dockerfile` stage 2 configurations |
| **IMG-05** | Docker | Verify `.dockerignore` presence and context filtering | File Inspection | `.dockerignore` files exist to exclude `.git`, `target/`, IDE files | Root and all 6 service directories have dedicated `.dockerignore` files excluding target, logs, and credentials while preserving `pom.xml` and `src/` | **PASS** | Root and service `.dockerignore` files |
| **CMP-01** | Compose | Validate Docker Compose syntax and configuration | CLI Command | Compose config passes validation without errors | Validation succeeded with exit code 0 | **PASS** | `docker compose config --quiet` |
| **CMP-02** | Compose | Verify service definitions in Compose | CLI Command | All 5 application services declared | `customer-service`, `auth-service`, `account-service`, `transaction-service`, `api-gateway` present | **PASS** | `docker compose config --services` |
| **CMP-03** | Compose | Verify port exposure security in Compose | Source Inspection | Only Gateway (8080) and Eureka (8761) published; internal ports hidden | Gateway maps `8080:8080`, Eureka standalone maps `8761:8761`. Ports 8081-8084 not published | **PASS** | `compose.yaml` lines 96-98 |
| **CMP-04** | Compose | Verify Hibernate DDL-auto override in Compose | Source Inspection | Overridden to `validate` for all DB services | `SPRING_JPA_HIBERNATE_DDL_AUTO: validate` set for all 4 services | **PASS** | `compose.yaml` lines 8, 27, 53, 72 |
| **CMP-05** | Compose | Verify secret injection via environment variables | Source Inspection | Secrets injected via `${VAR:?error}` syntax | All credentials use `${VAR:?error}` without hardcoded fallbacks | **PASS** | `compose.yaml` lines 11-14, 30-34, 56-59, 75-78, 94 |
| **CMP-06** | Compose | Verify container healthcheck definitions | Source Inspection | Healthchecks defined for automated recovery | No healthcheck blocks defined in `compose.yaml` (outstanding enhancement) | **WARNING** | `compose.yaml` inspection |
| **RUN-01** | Runtime | Verify all FinCore containers are actively running | CLI Command | All 6 containers in `Up` state | All 6 containers actively running in `Up` state (`Up 54+ minutes`). Note: Docker engine `healthy` status is not present because Docker/Compose healthchecks are not yet defined | **PASS** | `docker compose ps` & `docker ps` |
| **RUN-02** | Runtime | Verify container logs for clean startup | CLI Command | Spring Boot starts without crash loops or exceptions | All 6 containers started cleanly in 5.7s to 12.2s. 0 fatal exceptions | **PASS** | `docker logs <container> --tail 50` |
| **RUN-03** | Runtime | Check for unhandled exceptions in service logs | CLI Command | Zero `ERROR` level logs | Zero `ERROR` logs across all 6 running containers | **PASS** | `docker logs <container> \| findstr /c:" ERROR "` |
| **NET-01** | Network | Verify container network membership | CLI Command | All 6 containers attached to `fincore-network` | All 6 containers assigned IP addresses in subnet `172.22.0.0/16` | **PASS** | `docker network inspect fincore-network` |
| **NET-02** | Network | Verify host resolution from container | CLI Command | `host.docker.internal` resolves inside container | Resolves to host IP `fdc4:f303:9324::254` | **PASS** | `docker exec fincore-customer-service-1 getent hosts host.docker.internal` |
| **NET-03** | Network | Verify Eureka DNS resolution from container | CLI Command | `eureka-server` resolves to `172.22.0.2` | Resolves to `172.22.0.2` | **PASS** | `docker exec fincore-customer-service-1 getent hosts eureka-server` |
| **NET-04** | Network | Verify Eureka internal HTTP connectivity | HTTP Request | `http://eureka-server:8761/eureka/apps` returns HTTP 200 | HTTP 200 returned from inside container | **PASS** | `docker exec fincore-customer-service-1 curl -s -o /dev/null -w "%{http_code}" http://eureka-server:8761/eureka/apps` |
| **DIS-01** | Discovery | Verify Eureka microservice registration status | HTTP Request | 5 client services registered with status `UP` | `apps__hashcode: "UP_5_"`, all 5 instances report status `UP` | **PASS** | `curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps` |
| **DB-01** | Database | Host PostgreSQL service listening on port 5432 | CLI Command | Port 5432 accepting TCP connections | Port 5432 accepting connections; TCP connection succeeded | **PASS** | `pg_isready -h localhost -p 5432` & `Test-NetConnection -Port 5432` |
| **DB-02** | Database | HikariCP pool & JPA EntityManagerFactory startup | Container Logs | Hikari initializes and JPA connects to each DB | All 4 services log `HHH10001005: Database info` and initialize JPA factory | **PASS** | Container startup logs |
| **DB-03** | Database | Hibernate schema validation in Docker | Container Logs | Validation succeeds against PostgreSQL 18.6 | All 4 services booted cleanly with `ddl-auto: validate` without schema errors | **PASS** | Container startup logs |
| **DB-04** | Database | Baseline table row count preservation | SQL Queries | Table row counts identical before and after testing | Row counts confirmed identical across all 6 tables pre/post test (Customers: 4, Users: 6, Verifications: 1, Accounts: 5, Operations: 26, Transactions: 17), verifying zero row additions or deletions during read-only testing | **PASS** | SQL count verification pre/post test |
| **GW-01** | Gateway | Verify Gateway root availability | HTTP Request | Gateway handles requests and returns Spring response | HTTP 404 with JSON error body returned | **PASS** | `curl -i http://localhost:8080/` |
| **GW-02** | Gateway | Verify Gateway actuator health endpoint | HTTP Request | HTTP 200 `{"status":"UP"}` | HTTP 200 `{"groups":["liveness","readiness"],"status":"UP"}` | **PASS** | `curl -i http://localhost:8080/actuator/health` |
| **GW-03** | Gateway | Reject requests missing Authorization header | HTTP Request | HTTP 401 Unauthorized | HTTP 401 `Missing or invalid Authorization header` | **PASS** | `curl -i http://localhost:8080/customers` |
| **GW-04** | Gateway | Reject requests with invalid / malformed JWT | HTTP Request | HTTP 401 Unauthorized | HTTP 401 `Invalid or expired token` | **PASS** | `curl -i -H "Authorization: Bearer invalid.jwt.token" http://localhost:8080/customers` |
| **GW-05** | Gateway | Reject requests with expired JWT token | HTTP Request | HTTP 401 Unauthorized | HTTP 401 `Invalid or expired token` | **PASS** | Synthesized expired token against `http://localhost:8080/customers` |
| **GW-06** | Gateway | Reject invalid role claim in JWT (`role: "GUEST"`) | HTTP Request | HTTP 401 Unauthorized | HTTP 401 `Invalid token claims` | **PASS** | Invalid role claim token test |
| **GW-07** | Gateway | Reject blank subject in JWT (`sub: ""`) | HTTP Request | HTTP 401 Unauthorized | HTTP 401 `Invalid token claims` | **PASS** | Blank subject token test |
| **GW-08** | Gateway | Enforce ADMIN role on DELETE operations | HTTP Request | HTTP 403 Forbidden for USER role | HTTP 403 `Admin access required` | **PASS** | `DELETE http://localhost:8080/customers/999999` with USER token |
| **GW-09** | Gateway | Gateway routing to Customer Service (`lb("customer-service")`) | HTTP Request | HTTP 200 OK with customer list | HTTP 200 OK returned, 4 customers parsed | **PASS** | `GET http://localhost:8080/customers` with USER/ADMIN token |
| **GW-10** | Gateway | Gateway routing to Account Service (`lb("account-service")`) | HTTP Request | HTTP 200 OK with account list | HTTP 200 OK returned, 5 accounts parsed | **PASS** | `GET http://localhost:8080/accounts` with ADMIN token |
| **GW-11** | Gateway | Gateway routing to Transaction Service (`lb("transaction-service")`) | HTTP Request | HTTP 200 OK with transaction page | HTTP 200 OK returned, Page content present | **PASS** | `GET http://localhost:8080/transactions/history` with ADMIN token |
| **GW-12** | Gateway | Verify Gateway routing for public Auth POST endpoints | HTTP Request | Gateway routes `/auth/login`, `/auth/register` to Auth Service | Route active in `JwtGatewayConfig.java`; `POST /auth/login` reaches Auth Service (returns HTTP 400 validation response from AuthController) | **PASS** | `curl -X POST -H "Content-Type: application/json" -d "{}" http://localhost:8080/auth/login` |
| **SEC-01** | Security | External blocking of internal endpoints at Gateway | HTTP Request | Gateway returns 404 for `/internal/**` | `/customers/internal/**`, `/accounts/internal/**`, and `/auth/internal/**` all return HTTP 404 through Gateway | **PASS** | `curl -i http://localhost:8080/customers/internal/...` |
| **SEC-02** | Security | Reject missing `X-Internal-Secret` on internal endpoints | HTTP Request | HTTP 403 Forbidden | HTTP 403 Forbidden returned across all internal service controllers | **PASS** | Direct container curl tests to ports 8081, 8082, 8083 |
| **SEC-03** | Security | Reject invalid `X-Internal-Secret` on internal endpoints | HTTP Request | HTTP 403 Forbidden | HTTP 403 Forbidden returned (`Invalid internal secret`) | **PASS** | Direct container curl tests with dummy header |
| **COMM-01** | Feign | Verify Account Service -> Auth Service Feign call | Live Invocation | Feign discovers `auth-service` and transfers data successfully | Verified via container network invocation: Account Service resolved Auth Service via Eureka, transmitted `X-Internal-Secret`, deserialized `CustomerLinkResponse(customerId=[REDACTED], verified=true)`, and returned HTTP 200 with customer account list | **PASS** | Direct container curl `GET http://account-service:8082/accounts` with `USER` token (`sub: [TEST_USER]`) -> HTTP 200 |
| **DOC-01** | Docs | README Docker deployment documentation | Document Review | README documents Docker Compose setup, network, and commands | Comprehensive Docker guide added covering prerequisites, `.env` vs Compose variables, Eureka standalone setup, and safe shutdown | **PASS** | `README.md` text inspection |
| **E2E-01** | E2E | End-to-end KYC OTP verification journey | Live Walkthrough | Issue and verify OTP via email | Requires live SMTP credentials and user state mutation | **NOT TESTED** | Mandated safety rule #10 & #11 |
| **E2E-02** | E2E | End-to-end distributed fund transfer saga | Live Walkthrough | Debit sender and credit receiver with compensating refund | Requires creating transactional test balances | **NOT TESTED** | Mandated safety rule #10 & #11 |

---

## C. Detailed Findings

### 1. [RESOLVED] API Gateway Routing for Public Auth Endpoints
- **Status:** **PASS / Resolved**
- **Resolution:** Added `publicAuthPath()` and `authRoute()` to [JwtGatewayConfig.java](api-gatway/src/main/java/com/fincore/api_gatway/config/JwtGatewayConfig.java).
- **Details:** The Gateway forwards `POST` requests for `/auth/register`, `/auth/login`, `/auth/customer-verification/send`, and `/auth/customer-verification/verify` to `auth-service` via `lb("auth-service")`. Requests to `/auth/internal/**` and non-POST methods are excluded and return HTTP 404. Live invocation of `POST /auth/login` with `{}` confirms the request reaches `auth-service` and returns an HTTP 400 validation error from the controller. Note: Successful end-to-end user login with real credentials was not performed to prevent user state changes.

### 2. [RESOLVED] Docker Deployment Guide in README
- **Status:** **PASS / Resolved**
- **Resolution:** Updated [README.md](README.md) with a dedicated `## 🐳 Complete Docker Deployment Guide`.
- **Details:** Covers Docker Desktop prerequisites, distinguishes required `.env` variables from Compose-supplied variables (`SPRING_DATASOURCE_URL_*`, `EUREKA_SERVER_URL`), documents standalone Eureka setup on `fincore-network`, explains `host.docker.internal` PostgreSQL connectivity, health monitoring commands, safe stopping without `-v`, and Hibernate `validate` mode.

### 3. [RESOLVED] Container Non-Root Runtime Execution
- **Status:** **PASS / Resolved**
- **Resolution:** Updated all six Dockerfiles ([customer-service/Dockerfile](customer-service/Dockerfile), [account-service/Dockerfile](account-service/Dockerfile), [auth-service/Dockerfile](auth-service/Dockerfile), [transaction-service/Dockerfile](transaction-service/Dockerfile), [api-gatway/Dockerfile](api-gatway/Dockerfile), [eureka-server/Dockerfile](eureka-server/Dockerfile)).
- **Details:** The runtime stage creates a dedicated system group and user (`RUN groupadd -r fincore && useradd -r -g fincore -d /app -s /sbin/nologin fincore`), copies the JAR with `--chown=fincore:fincore`, and switches execution to `USER fincore`.

### 4. [RESOLVED] Build Context Filtering (`.dockerignore`)
- **Status:** **PASS / Resolved**
- **Resolution:** Added [.dockerignore](.dockerignore) at repository root and in all six microservice directories.
- **Details:** Excludes `target/`, `.git/`, IDE configurations, `.env*`, and database dumps while preserving `pom.xml` and `src/`.

### 5. [RESOLVED] Verbose SQL Query Logging in Docker
- **Status:** **PASS / Resolved**
- **Resolution:** Injected `SPRING_JPA_SHOW_SQL: "false"` into [compose.yaml](compose.yaml) for `customer-service`, `auth-service`, `account-service`, and `transaction-service`.
- **Details:** Prevents the scheduled transfer reconciliation task from printing verbose SQL queries every 30 seconds in Docker, while preserving local development settings in `application.yml`.

### 6. [WARNING] Missing Docker Compose Healthchecks
- **Status:** **WARNING / Outstanding Enhancement**
- **Location:** [compose.yaml](compose.yaml)
- **Impact:** **Low / Operational Resiliency.** The compose file declares `depends_on` between microservices, but lacks `healthcheck` definitions checking Spring Boot Actuator (`/actuator/health`). Adding healthcheck blocks ensures dependent containers wait for full Spring context initialization rather than container startup alone.
- **Priority:** **P3**

### 7. [RESOLVED] Verified Inter-Service OpenFeign Data Exchange (COMM-01)
- **Status:** **PASS / Resolved**
- **Location:** `account-service` -> `auth-service` via OpenFeign
- **Resolution:** Live invocation within `fincore-network` to `http://account-service:8082/accounts` using an authenticated request with `USER` role claims for an existing pre-seeded profile.
- **Details:** `account-service` dynamically resolved `auth-service` via Eureka, propagated the internal `X-Internal-Secret` header to `/auth/internal/customer-link`, and successfully deserialized the response (`CustomerLinkResponse(customerId=[REDACTED], verified=true)`), returning HTTP 200 with matching customer accounts (`[REDACTED_ACCOUNT_LIST]`). Confirmed in container logs with zero errors and invariant database row counts.

---

## D. Security Findings

| Level | Finding | Description | Recommendation |
|---|---|---|---|
| **Critical** | **Historical Git Commit Credentials** | Previous Git commits (`baef723`, `acabb49`) committed database credentials and JWT keys before environment variable refactoring. In a public GitHub repository, working-tree remediation does not purge historical commits. Normal commit/push operations do not sanitize history. | The Windows PostgreSQL password has already been rotated. For historically exposed JWT and internal secrets, rotation is recommended only if the currently active secrets deployed in `.env` have not already been replaced with fresh keys. If complete historical cleanup is required, use `git-filter-repo` with coordinated team force-push. |
| **Informational** | **East-West Constant-Time Comparison** | Internal microservice endpoints (`/customers/internal/**`, `/accounts/internal/**`, `/auth/internal/**`) correctly validate `X-Internal-Secret` using `MessageDigest.isEqual()`, mitigating timing attacks. | Maintain this pattern for all future internal microservices. |
| **Informational** | **Explicit HMAC-SHA384 Token Signing** | `auth-service`'s `JwtService.java` explicitly specifies `Jwts.SIG.HS384` during signing, preventing JJWT algorithm auto-inference mismatches with downstream resource servers. | Retain explicit algorithm declarations across all JWT issuance logic. |
| **Informational** | **Dual-Perimeter Gateway Enforcement** | API Gateway routes public traffic via `lb(...)` while internal endpoints (`/internal/**`) are excluded from route predicates and return HTTP 404. | Continue enforcing endpoint segregation at the Gateway router function level. |

---

## E. Database Safety & Row Count Verification

In compliance with Critical Safety Rules:
- No database schemas, tables, sequences, or constraints were created, altered, or deleted.
- No `docker compose down -v` or Docker volume modifications were executed.
- All containers ran strictly with `SPRING_JPA_HIBERNATE_DDL_AUTO: validate`.
- Executed integration tests were strictly non-mutating (read queries and rejected validation calls); stateful write flows (user registration, OTP generation, fund transfers) were intentionally omitted.
- Live SQL row count audits verified that record counts remained strictly invariant across all 4 databases and 6 application tables before and after testing.

### Row Count Audit (Pre-Test vs Post-Test)

| Database | Table Name | Pre-Test Row Count | Post-Test Row Count | Row Count Status |
|---|---|---:|---:|---|
| **customer_db** | `customers` | 4 | 4 | **INVARIANT (Identical)** |
| **auth_db** | `customer_verifications` | 1 | 1 | **INVARIANT (Identical)** |
| **auth_db** | `users` | 6 | 6 | **INVARIANT (Identical)** |
| **account_db** | `account` | 5 | 5 | **INVARIANT (Identical)** |
| **account_db** | `account_operations` | 26 | 26 | **INVARIANT (Identical)** |
| **transaction_db** | `transactions` | 17 | 17 | **INVARIANT (Identical)** |

**Audit Conclusion & Scope Boundary:** Invariant row counts confirm that zero rows were inserted or deleted during test execution. Because testing was designed to be non-mutating (read-only queries and negative input validation), existing records were not updated. Note that equal row counts demonstrate unchanged counts rather than unconditionally proving unmutated field contents across all individual table columns (which would require cryptographic row hashing or full-table column diffing); the claim of data preservation is supported by row count invariance combined with the non-mutating nature of the executed tests.

---

## F. Missing Tests (Requiring Approval / Extra Infrastructure)

The following tests could not be executed safely under the active safety rules:

1. **Live User Registration & Login End-to-End Test (`POST /auth/register`, `POST /auth/login`)**:
   - *Reason:* Creating a new test user mutates `auth_db.users`. Calling login repeatedly with guessing credentials risks locking existing accounts.
   - *Requirement for Execution:* Approval to create a designated staging test user with known test credentials.
2. **End-to-End KYC Email OTP Delivery (`POST /auth/customer-verification/send`)**:
   - *Reason:* Requires sending real outbound SMTP emails via Gmail and creating verification records in `auth_db.customer_verifications`.
   - *Requirement for Execution:* Mailhog or greenmail mock SMTP container, and approval for test data creation.
3. **End-to-End Distributed Money Transfer Saga (`POST /transactions/transfer`)**:
   - *Reason:* Modifies account balances in `account_db`, inserts debit/credit operations in `account_operations`, and records transfer ledgers in `transaction_db`.
   - *Requirement for Execution:* Dedicated sandbox customer accounts with test funds and approval for transactional tests.

---

## G. Recommendations in Priority Order

### Priority 1: Git Security & Secret Hygiene
1. **Historical Git Exposure Notice**:
   The Windows PostgreSQL password has already been rotated. For historically committed JWT and internal secrets, rotation is recommended only if the currently active secrets deployed in `.env` have not already been replaced with fresh keys. If complete historical cleanup is required on this public repository, purge Git history using `git-filter-repo` and force-push with team alignment.

### Priority 2: Operational Enhancements
2. **Add Docker Compose Healthchecks**:
   Configure healthchecks in [compose.yaml](compose.yaml) using Spring Boot Actuator (`curl -f http://localhost:<port>/actuator/health || exit 1`) and update `depends_on` conditions to `service_healthy`.
3. **Automate End-to-End Sandbox Testing**:
   Establish an isolated automated integration test suite that spins up an ephemeral test database to validate the complete KYC registration, login, and money transfer saga without touching production/host databases.

---

## H. Final Deployment Verdict

### **Verdict: DEPLOYED (INFRASTRUCTURE VERIFIED; BUSINESS WORKFLOWS UNVERIFIED)**

### Justification:
- **Deployment Infrastructure**: Complete and verified. The container orchestration, custom bridge networking (`fincore-network`), host PostgreSQL 18.6 connectivity via `host.docker.internal:5432`, standalone Eureka registration, OpenFeign inter-service communication discovery, Hikari connection pooling, Hibernate schema validation mode (`validate`), and disabled Docker SQL query noise are all operational.
- **Security Hardening**: All six Dockerfiles implement a non-root system user (`fincore`), `.dockerignore` files prevent build context leakage, internal endpoints are blocked at the Gateway, constant-time internal secret validation is active, and token signing uses explicit HMAC-SHA384.
- **Routing & Documentation**: API Gateway routes all required services (including public `/auth/**` POST endpoints), and [README.md](README.md) provides complete, accurate documentation distinguishing required root `.env` variables from Compose-supplied variables.
- **Remaining Scope**: State-modifying business workflows (KYC email OTP delivery and fund transfer sagas) remain untested by design to preserve database integrity. Unit and integration test suites were not executed during Docker builds (`-DskipTests`).
