# 🚀 Distributed Job Scheduling System

A production-oriented **Distributed Job Scheduling System** built with **Java, Spring Boot, MySQL, Redis, and JWT Security**.

The system allows authenticated users to create and schedule different types of jobs, execute them using multiple worker instances, handle failures with retries and exponential backoff, prevent duplicate execution using idempotency and atomic claiming, and provide operational metrics through Prometheus.

---

# 🏗️ System Architecture

```text
                         CLIENT
                           │
                           ▼
                    ┌─────────────┐
                    │ JWT Security│
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Controllers │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │   Services  │
                    └──────┬──────┘
                           │
                     ┌─────┴─────┐
                     ▼           ▼
                   MySQL       Scheduler
                     │           │
                     │           ▼
                     │     JobExecution
                     │           │
                     │     ┌─────┴─────┐
                     │     │   Outbox  │
                     │     └─────┬─────┘
                     │           │
                     │           ▼
                     │         Redis
                     │       Priority
                     │         Queue
                     │           │
                     │      ┌────┼────┐
                     │      ▼    ▼    ▼
                     │    W-1   W-2  W-3
                     │      │    │    │
                     │      └────┼────┘
                     │           │
                     │      Atomic Claim
                     │           │
                     │      Idempotency
                     │           │
                     │       Job Handler
                     │           │
                     │      ┌────┴────┐
                     │      ▼         ▼
                     │   SUCCESS    FAILURE
                     │                │
                     │             RETRY
                     │                │
                     │             DEAD
                     │
                     └──── Execution
                           History

             ┌──────────────────────────────┐
             │     Actuator → Prometheus    │
             └──────────────────────────────┘
```


---

## 🧠 Five Core Distributed-System Problems

```text 
                     DISTRIBUTED JOB SCHEDULER
                           │
       ┌───────────────────┼───────────────────┐
       ▼                   ▼                   ▼
   WHEN TO RUN         HOW TO DELIVER      HOW TO EXECUTE
   Scheduler           Outbox + Redis      Workers
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
                  HOW TO STAY RELIABLE
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
            Retry      Idempotency   Atomic Claim
              │
              ▼
             DEAD
                           │
                           ▼
                  HOW TO OPERATE IT
                           │
             Security + Metrics + History
```

---

### 🛠️ Tech Stack

---

Backend
- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Scheduling
- Spring Mail
- Spring Actuator

Database
- MySQL
- Hibernate / JPA

Distributed Components
- Redis
- Redis Sorted Set
- Transactional Outbox
- Multiple Worker Instances

Security
- JWT
- BCrypt
- Role-Based Authorization
- Monitoring
- Spring Boot Actuator
- Prometheus

---

## 📁 Project Structure

```text 

job-scheduler/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/jobscheduler/job_scheduler/
│   │   │
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── repository/
│   │   │       ├── security/
│   │   │       └── service/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── reports/
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

```

## ⚡ Performance & Throughput

### 📊 Worker Scalability Test

| Workers | Jobs | Processing Time | Throughput | Successful |
|--------:|-----:|----------------:|-----------:|----------:|
| 1       | 100  | ~105.48 sec     | ~0.95 jobs/sec | 100 |
| 2       | 100  | ~55.86 sec      | ~1.79 jobs/sec | 100 |
| 3       | 100  | ~47.40 sec      | ~2.11 jobs/sec | 100 |

### 📈 1000 Jobs Load Test

| Metric | Result |
|---|---:|
| Total Jobs | 1,000 |
| Successful Jobs | 1,000 |
| Processing Time | ≈ 16.77 sec |
| Throughput | ≈ 59.63 jobs/sec |
| Success Rate | **100%** |

---
