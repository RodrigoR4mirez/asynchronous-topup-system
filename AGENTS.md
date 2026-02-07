# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

Event-driven phone top-up system built with Quarkus 3 and Java 21. Three independent microservices communicate via Kafka (Avro serialization) and share a MySQL database.

## Architecture

```
┌─────────────────────┐     ┌─────────────────────────┐     ┌─────────────────────┐
│ sync-topup-api-v1   │     │ async-topup-producer-v1  │     │ async-topup         │
│ (REST entry)        │     │ (Scheduler/Producer)     │     │ -consumer-v1        │
│ :9090               │     │ :8085                    │     │ (Kafka Consumer)    │
│                     │     │                          │     │ :8086               │
└─────────┬────────────┘     └────────────┬─────────────┘     └──────────┬──────────┘
         │                            │                            │
         │  persist PENDING           │ poll PENDING               │ consume TopUpEvent
         │                            │ emit to Kafka              │ validate balance
         │                            │ update SENT_TO_KAFKA       │ update COMPLETED/FAILED
         ▼                            ▼                            ▼
    ┌─────────────────────────────────────────────────────────────────┐
    │                     MySQL (phone_recharge_db)                   │
    │   Tables: recharge_requests, balance_wallets, process_audits    │
    └─────────────────────────────────────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    │   Kafka + Schema    │
                    │   Registry (Avro)   │
                    │   Topic: topup-topic│
                    └─────────────────────┘
```

### Transaction Status Flow
`PENDING` → `SENT_TO_KAFKA` → `COMPLETED` | `FAILED`

## Build & Run Commands

Each microservice is built independently from its directory:

```bash
# Build a single service
./mvnw clean package -f sync-topup-api-v1/pom.xml

# Run in dev mode (hot reload)
./mvnw quarkus:dev -f sync-topup-api-v1/pom.xml
./mvnw quarkus:dev -f async-topup-producer-v1/pom.xml
./mvnw quarkus:dev -f async-topup-consumer-v1/pom.xml

# Run tests for a service
./mvnw test -f sync-topup-api-v1/pom.xml

# Run a single test class
./mvnw test -f sync-topup-api-v1/pom.xml -Dtest=TopupResourceTest

# Build native image
./mvnw package -Pnative -f sync-topup-api-v1/pom.xml
```

## Infrastructure Setup

**Database**: MariaDB/MySQL container named `mariadb10432` on port 3307
```bash
# Query tables
docker exec -it mariadb10432 mysql -u root -p123456789 phone_recharge_db -e "SELECT * FROM recharge_requests;"
```

**Kafka**: Brokers at localhost:19092,29092 with Schema Registry at localhost:8081

**Database schema**: See `files/script.sql` for table definitions and sample data.

## Key Technical Details

- **Reactive stack**: All services use Hibernate Reactive with Panache and Mutiny (`Uni<T>`, `Multi<T>`)
- **Transactions**: Use `@WithTransaction` annotation (from `io.quarkus.hibernate.reactive.panache.common`)
- **Kafka serialization**: Avro schemas in `src/main/avro/topup.avsc` → generates `TopUpEvent` class
- **Configuration**: YAML format in `src/main/resources/application.yml`
- **Dispatcher polling**: Runs every 10 seconds via `@Scheduled(every = "10s")`

## API Endpoint

```bash
POST http://localhost:9090/v1/topups
Content-Type: application/json

{
  "phoneNumber": "987654321",
  "amount": 50.0,
  "carrier": "MOVISTAR"
}
# Returns 202 Accepted
```

## Code Patterns

- **Entities**: Public fields with Panache (no getters/setters needed)
- **Repositories**: Extend `PanacheRepository<Entity>` or `PanacheRepositoryBase<Entity, ID>`
- **Services**: `@ApplicationScoped` CDI beans with constructor injection
- **Resources**: JAX-RS endpoints returning `Uni<Response>`
