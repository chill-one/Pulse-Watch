# PulseWatch — Technology Decision Log

## 1. Project Learning Goals

PulseWatch is being optimized for learning:

- Backend engineering
- Distributed systems
- DevOps
- Cloud

The goal is not to pick the easiest stack. The goal is to choose technologies that help build and understand a production-style backend system.

---

## 2. Backend API — Java + Spring Boot

### Decision

```text
Backend API
→ Java
→ Spring Boot
→ Maven
→ JUnit
```

### Why we chose it

- Strong fit for backend and enterprise-style systems.
- Good way to deepen Java experience.
- Encourages separation of responsibilities:
  - Controller
  - Service
  - Repository
- Strong support for REST APIs, validation, dependency injection, transactions, testing, database access, messaging, scheduling, and observability.
- Fits naturally with PostgreSQL and RabbitMQ.
- Gives broader backend experience instead of repeating FastAPI.

### Why not FastAPI?

FastAPI would work well and would likely be faster to build with. We chose Spring Boot because the goal is to learn more backend architecture rather than use the most familiar tool.

### Later updates

Potential later additions:

```text
Spring Boot Actuator
Micrometer
OpenTelemetry
```

### Questions to revisit

- How should packages/modules be organized?
- Which logic belongs in Controller vs Service?
- Where should transaction boundaries live?
- Should Scheduler, Worker, and Alert Service eventually become separate deployable Spring Boot applications?

---

## 3. Database — PostgreSQL

### Why we chose it

PulseWatch stores strongly related data:

```text
Monitor
CheckResult
Incident
Alert
```

PostgreSQL gives us:

- Primary keys
- Foreign keys
- Unique constraints
- Transactions
- Indexes
- Row locking
- Persistent storage

These are especially useful for the distributed-system problems PulseWatch will eventually encounter.

Useful future index:

```text
(monitor_id, checked_at)
```

### Later updates

- Soft deletion instead of cascade deletion
- Data retention/archiving
- Partitioning if CheckResult grows very large
- Row locking for multiple Scheduler instances
- Query tuning and performance testing

### Questions to revisit

- How long should CheckResults be retained?
- Should Monitor deletion permanently remove historical data?
- Should URLs be globally unique or unique per user?
- Which fields need database-level constraints?
- When should we use row locking?

---

## 4. Task Queue — RabbitMQ

### Why we chose it

PulseWatch needs:

- Durable queued work
- Producer/consumer model
- Multiple Workers
- ACK after successful processing
- Redelivery after Worker failure
- At-least-once delivery
- Retry/dead-letter possibilities
- Good Spring integration

RabbitMQ maps directly to our architecture:

```text
Scheduler
→ Producer
→ Exchange
→ Queue
→ Consumer
→ Worker
```

### Why not Redis Pub/Sub?

It can lose messages when consumers are unavailable, which does not fit our durability requirement.

Redis Streams could work, but RabbitMQ more directly matches the task-queue model we designed.

### Why not Kafka?

Kafka is excellent for durable event streams and multiple independent consumers. PulseWatch currently needs a simpler work-queue model where one Worker processes one task.

Kafka may become interesting later for events such as:

```text
CheckCompleted
IncidentOpened
IncidentResolved
```

### Later updates

Possible later RabbitMQ structure:

```text
check exchange
→ check.tasks queue
→ monitoring workers

alert exchange
→ alert.tasks queue
→ alert workers
```

Also consider:

```text
retry queue
dead-letter queue
prefetch tuning
publisher confirms
```

### Questions to revisit

- What queue type should we use?
- How many messages should a Worker prefetch?
- What retry policy should failed tasks use?
- When should a task be dead-lettered?
- How should RabbitMQ itself be made highly available?
- How do we prevent duplicate effects under at-least-once delivery?

---

## 5. Scheduler — Spring Scheduling

### Why we chose it

We already store scheduling information in PostgreSQL:

```text
check_interval
next_check_at
```

So the Scheduler can periodically query:

```text
next_check_at <= current_time
```

Then:

```text
find due monitors
→ create check tasks
→ publish tasks
→ advance next_check_at
```

This keeps PostgreSQL as the scheduling source of truth.

### Why not one timer per Monitor?

At large scale, many in-memory timers become hard to manage and restart safely.

### Why not Quartz yet?

Quartz is more sophisticated, but we already designed scheduling around `next_check_at`. Adding Quartz now would introduce another scheduling model before we need it.

### Batching decision

If many Monitors are due:

```text
fetch a bounded batch
→ publish individual RabbitMQ messages
→ fetch the next batch
```

Database batching does not mean one giant RabbitMQ message.

### Later updates

- Multiple Scheduler instances
- Safe row claiming / locking
- Configurable batch sizes
- Backpressure based on queue depth

### Questions to revisit

- How frequently should the Scheduler run?
- What should batch size be?
- Should `next_check_at` advance from the previous schedule time or from current time?
- How do multiple Scheduler instances avoid scheduling the same Monitor?
- How should Scheduler lag be measured?

---

## 6. Worker — Spring Boot + Spring AMQP

### Why we chose it

The Worker needs to:

```text
consume RabbitMQ task
→ perform HTTP health check
→ store CheckResult
→ update failure count
→ update Monitor state
→ create/resolve Incident
→ create PENDING Alert
→ ACK task
```

Spring AMQP fits naturally with RabbitMQ and lets Workers consume messages asynchronously.

### Reliability concepts

#### Transactions

Related database changes should succeed or fail together.

Example:

```text
Insert CheckResult
Update Monitor
Create Incident
Create Alert
```

#### Idempotency

RabbitMQ may redeliver a task. Retrying the same logical task should not create duplicate effects.

Possible protection:

```text
unique task_id
```

or:

```text
unique (monitor_id, scheduled_check_time)
```

Difference:

```text
Transaction → atomicity / consistency
Idempotency → duplicate-processing protection
```

### Later updates

- Multiple Worker instances
- Concurrency tuning
- Virtual threads
- Async/non-blocking HTTP if needed
- Per-domain throttling
- Retry/backoff
- Worker metrics

### Questions to revisit

- How many tasks should each Worker process concurrently?
- What should happen on HTTP 429?
- Which failures should be retried?
- Which errors should count as a failed check?
- How do we make all Worker side effects idempotent?
- How should transaction boundaries interact with RabbitMQ ACKs?

---

## 7. HTTP Client — Spring RestClient

### Why we chose it

For the first version, a synchronous client is easier to understand.

The Worker needs to:

```text
start timer
→ send HTTP request
→ receive response/error
→ stop timer
```

### Why not WebClient yet?

WebClient is useful for reactive/non-blocking HTTP, but we do not yet know that PulseWatch needs that complexity.

We should first measure the synchronous implementation.

### Later updates

Possible future options:

```text
WebClient
Java virtual threads
higher Worker concurrency
```

### Questions to revisit

- Request timeout?
- Connect timeout?
- Follow redirects?
- Which HTTP status codes mean success?
- Should TLS/certificate errors count as DOWN?
- What User-Agent should PulseWatch send?
- How should DNS errors be represented?

---

## 8. Alert Service — Spring Boot

### MVP design

```text
Worker
→ create PENDING Alert in PostgreSQL

Alert Service
→ find PENDING Alerts
→ send notification
→ mark SENT / FAILED
```

### Why we chose this

Monitoring state and alert delivery should be independent.

Example:

```text
Monitor = DOWN
Alert delivery = FAILED
```

The website is still DOWN even if email delivery fails.

Creating a durable Alert row first means the system remembers that a notification needs to be sent.

### Later update

Eventually send alert work through RabbitMQ:

```text
PENDING Alert created
→ publish alert task
→ RabbitMQ
→ Alert Worker
→ Notification Provider
```

### Important reliability concern

```text
email sent
→ Alert Service crashes before marking SENT
→ retry
→ possible duplicate email
```

### Questions to revisit

- Which notification provider should we use?
- How should failed alerts be retried?
- How do we reduce duplicate emails?
- Should Alert delivery use its own RabbitMQ queue?
- Should email, SMS, Slack, etc. share one Alert abstraction?

---

## 9. Frontend — Next.js + TypeScript

### Why we chose it

The main learning goals are backend, distributed systems, DevOps, and cloud.

Using a familiar frontend stack keeps more project time focused on those areas.

Frontend responsibilities:

```text
Monitor form
Monitor list
Monitor detail
Incident history
Pulse visualization
```

Backend owns truth. Frontend owns presentation.

### Initial API calls

```text
GET /monitors/{id}
GET /monitors/{id}/checks?limit=50
GET /monitors/{id}/incidents?limit=10
```

### Later updates

Potential combined endpoint:

```text
GET /monitors/{id}/dashboard
```

Separate endpoints are preferred for the MVP because they are easier to test independently.

### Questions to revisit

- How should the pulse visualization represent latency?
- How many historical checks should be shown?
- How should DOWN/DEGRADED affect the animation?
- How should missing checks/timeouts appear?

---

## 10. Live Dashboard Updates — Polling First, SSE Later

### MVP

```text
Frontend polling
```

Why:
- simple
- easy to debug
- easy to test
- sufficient for the MVP

### Later

```text
Server-Sent Events (SSE)
```

SSE fits because live communication is mostly:

```text
Backend → Frontend
```

### Why not UDP?

Reliable delivery matters more than ultra-low latency.

### Why not WebSocket first?

WebSockets are two-way. PulseWatch mostly needs one-way server-to-browser updates, so SSE may be simpler.

### Questions to revisit

- How often should polling occur?
- When should we switch to SSE?
- Should SSE send full Monitor state or only new CheckResults?
- How should reconnects catch up on missed events?

---

## 11. HTTP Safety / Rate Limiting

Initial safeguards:

```text
minimum check interval
request timeout
limited retries
backoff
respect HTTP 429 Retry-After
honest User-Agent
prefer /health endpoints
```

### Later updates

Possible per-domain limits:

```text
maximum concurrent requests per hostname
```

### Questions to revisit

- Minimum allowed check interval?
- Per-domain concurrency limit?
- Retry policy?
- Backoff strategy?
- How should 429 responses affect Monitor state?
- Should multiple users monitoring the same URL share one underlying check?

---

## 12. Technology Map So Far

```text
Frontend
→ Next.js + TypeScript

Backend API
→ Java + Spring Boot

Database
→ PostgreSQL

Scheduler
→ Spring Scheduling

Task Queue
→ RabbitMQ

Worker
→ Spring Boot + Spring AMQP

HTTP Checker
→ Spring RestClient

Alert Service
→ Spring Boot

Live UI (MVP)
→ Polling

Live UI (later)
→ SSE
```

Still not selected:

```text
Local container setup
Cloud provider/services
Email provider
Infrastructure as Code
CI/CD
Observability stack
Secrets management
Deployment architecture
```

---

## 13. Distributed-System Problems Already Identified

### Duplicate task delivery

```text
Worker completes DB work
→ crashes before ACK
→ RabbitMQ retries
```

Need idempotency.

### Multiple Scheduler instances

```text
Scheduler A sees Monitor #7 due
Scheduler B sees Monitor #7 due
```

Need safe claiming/locking.

### Scheduler DB + RabbitMQ dual write

```text
update next_check_at
+
publish RabbitMQ task
```

One operation may succeed while the other fails.

Potential future topic:

```text
Transactional Outbox Pattern
```

### Alert side effects

```text
email sent
→ crash before marking SENT
```

May cause duplicates.

### Queue backpressure

Thousands of tasks may become due at once.

Need:

```text
batching
queue monitoring
worker scaling
```

---

## 14. Next Technology Decisions

Next steps:

```text
1. Local development / Docker architecture
2. Cloud deployment
3. Infrastructure as Code
4. CI/CD
5. Observability
6. Alert provider
7. Scaling strategy
```

Guiding rule:

> Choose a technology only after understanding the problem it needs to solve.
