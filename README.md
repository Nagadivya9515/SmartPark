# SmartPark: Concurrent Parking Management System

A high-throughput, low-latency backend microservice designed to handle real-time vehicle parking slot allocations natively in Go. This system focuses on distributed systems principles, absolute thread safety, and framework-free performance.

## 🚀 Core Architecture Features

* **Framework-Free Native Go Engine:** Built entirely using Go’s native standard library (`net/http`) and the enhanced `ServeMux` router for clean execution speeds, low memory overhead, and minimal binary deployment sizes.
* **Race-Condition & Thread Mitigation:** Designed a thread-safe allocation pipeline combining distributed `Redis SetNX` mutex locking with database-level row locks (`PESSIMISTIC_WRITE`) to completely eliminate dual-slot assignments under heavy peak loads.
* **ACID-Compliant CRUD Pipeline:** Implemented long-lived database connection pools with managed transaction scopes to guarantee absolute rollback safety, error tracking, and strict financial auditability.

## 🛠️ Technical Stack

* **Language Core:** Go (Golang 1.22+)
* **Networking & API:** Native `net/http`, REST API, JSON Marshalling
* **Cache & Distributed Locks:** Redis (via `go-redis/v9`)
* **Persistent Storage:** MySQL (via standard `database/sql` & `go-sql-driver/mysql`)

## 📁 System Architecture Directory Layout

```text
parking-lot-management/
├── apps/                    # Front-end placeholders (Minimal / Scalable)
├── services/
│   ├── go-core-engine/      # Golang Microservice Core
│   │   ├── cmd/
│   │   │   └── main.go      # HTTP Server & Route Initializer
│   │   └── internal/
│   │       ├── cache/       # Redis Mutex Lock Logic (SetNX)
│   │       ├── db/          # MySQL Database Pool & CRUD Operations
│   │       └── reservation/ # Core Fee & Allocation Mechanics
├── shared/
│   ├── database/            # SQL Initializers (schema.sql / seed.sql)
│   └── nginx/               # Reverse Proxy configurations
├── docker-compose.yml       # Dev Platform Infrastructure Orchestrator
└── README.md                # Documentation Blueprint
```

## ⚡ Execution & Quick Start Guide

### 1. Boot the Infrastructure Topology
Spin up your pre-configured local network mapping layers (MySQL, Redis cache) automatically using Docker:
```bash
docker-compose up -d
```

### 2. Install Project Dependencies
Initialize your module configurations locally and pull down the verified driver packages:
```bash
go mod init parking-engine
go get ://github.com
go get ://github.com
```

### 3. Launch the Core Backend Engine
Run the native server instance locally on your machine:
```bash
go run app.go db.go
```
The server will boot up and begin listening natively for incoming network traffic requests on port `:8080`.

## 📡 Verified Operational REST API Routing

| HTTP Method | Route Endpoint | Action Scope |
| :--- | :--- | :--- |
| **GET** | `/api/slots` | Fetch all parking spot statuses and real-time counter metrics. |
| **POST** | `/api/sessions/reserve` | Execute atomic check-and-reserve lock tracking routines. |
| **POST** | `/api/sessions/:id/exit` | Trigger checkout operations and fee ceiling evaluations. |

## 🧪 Parallel Concurrency Validation
To verify the system's thread-safety boundaries, you can invoke Go's built-in benchmarking modules to stress-test your code under 50+ simultaneous parallel routines:
```bash
go test -v -race ./...
```
