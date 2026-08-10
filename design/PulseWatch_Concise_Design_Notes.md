# PulseWatch — Concise Design Notes

## 1. Goal

PulseWatch is an uptime-monitoring platform that answers:

> Is a website/API working, how fast is it responding, and when did outages happen?

A user creates a monitor, PulseWatch checks the target on a schedule, stores results, updates service state, creates incidents after repeated failures, and sends outage/recovery alerts.

Primary learning goals:
- Backend engineering
- Distributed systems
- DevOps
- Cloud

## 2. Monitor States

```text
PENDING   -> waiting for first scheduled check
UP        -> latest check succeeded
DEGRADED  -> failures occurred, but threshold not reached
DOWN      -> failure threshold reached
```

Rules:
- First check waits until the first scheduled interval.
- Success resets consecutive failures to `0`.
- Failure increments consecutive failures.
- Threshold example: 3 consecutive failures -> `DOWN`.
- `DOWN -> UP` after a successful recovery check.

## 3. Monitoring Flow

```text
Monitor created
↓
PENDING
↓
Wait until next_check_at
↓
Scheduler creates task
↓
Task Queue
↓
Worker checks URL
↓
Store CheckResult
↓
Update monitor / incident state
```

Success:
```text
reset failure count
status = UP
if incident open:
    resolve incident
    create RECOVERY alert
```

Failure:
```text
increment failure count

below threshold:
    status = DEGRADED

threshold reached:
    status = DOWN
    if no incident open:
        create incident
        create OUTAGE alert
```

One continuous outage should create only one open incident.

## 4. Architecture

```text
User
  ↓
Frontend ↔ Backend API ↔ PostgreSQL

Scheduler ↔ PostgreSQL
    ↓
Task Queue
    ↓
Worker
  ↙     ↘
Website  PostgreSQL
           ↓
       Alert records

Alert Service → User
```

Responsibilities:
- Backend API: handles user requests and validation.
- PostgreSQL: persists state/history.
- Scheduler: finds checks that are due.
- Task Queue: buffers pending work.
- Worker: performs checks and updates state.
- Alert Service: delivers notifications.
- Frontend: displays state and history.

## 5. Data Model

### Monitor
```text
id
name
url
check_interval
next_check_at
status
consecutive_failure_count
```

### CheckResult
```text
id
task_id
monitor_id
checked_at
status_code
latency_ms
error
```

### Incident
```text
id
monitor_id
started_at
ended_at
```

`ended_at = NULL` means the incident is open.

### Alert
```text
id
incident_id
type
delivery_status
created_at
sent_at
```

Types:
```text
OUTAGE
RECOVERY
```

Delivery states:
```text
PENDING
SENT
FAILED
```

Relationships:
```text
Monitor 1 -> many CheckResults
Monitor 1 -> many Incidents
Incident 1 -> many Alerts
```

Foreign keys:
```text
CheckResult.monitor_id -> Monitor.id
Incident.monitor_id -> Monitor.id
Alert.incident_id -> Incident.id
```

MVP delete behavior: deleting a Monitor cascades to related history.

## 6. Backend API

Responsibilities:
- Receive frontend requests
- Validate input
- Apply business rules
- Read/write PostgreSQL
- Return data/errors

Initial endpoints:
```text
POST   /monitors
GET    /monitors
GET    /monitors/{id}
PATCH  /monitors/{id}
DELETE /monitors/{id}

GET /monitors/{id}/checks?limit=50
GET /monitors/{id}/incidents?limit=10
```

MVP decision:
- Keep these endpoints separate.
- Add a combined `/dashboard` endpoint later if useful.
- Backend owns truth; frontend owns presentation.

## 7. Scheduler

Purpose:
> Find monitors whose checks are due and enqueue work.

Main condition:
```text
next_check_at <= current_time
```

After scheduling:
```text
next_check_at = next_check_at + check_interval
```

Task contains:
```text
task_id
monitor_id
url
timeout
```

Scheduler does not make HTTP requests.

Future concern:
- Multiple schedulers may schedule the same monitor.

## 8. Task Queue

```text
Scheduler = producer
Worker    = consumer
Queue     = buffer
```

Requirements:
- Durable: tasks survive crashes.
- ACK-based: remove a task only after successful processing.
- Retry if Worker crashes before ACK.

Important consequence:
> At-least-once delivery means a task may run more than once.

Therefore Worker processing must be idempotent.

## 9. Worker

Flow:
```text
Receive task
↓
Send HTTP request
↓
Measure latency
↓
Handle response / timeout / network error
↓
Create CheckResult
↓
Update failure count
↓
Update Monitor status
↓
Create/resolve Incident
↓
Create PENDING Alert if needed
↓
Commit transaction
↓
ACK task
```

Reliability concepts:

**Transaction**
- Related database changes succeed or fail together.

**Idempotency**
- Retrying the same task should not create duplicate effects.
- Possible protection: unique `task_id` on `CheckResult`.

Difference:
```text
Transaction -> atomicity / consistency
Idempotency -> duplicate-processing protection
```

## 10. Alert Service

Worker creates a durable `PENDING` Alert instead of sending email directly.

```text
Worker transaction
↓
Create PENDING Alert
↓
Commit

Alert Service
↓
Find PENDING Alert
↓
Send notification
↓
Mark SENT or FAILED
```

Important rule:
> Alert delivery failure must not change monitor state.

Example:
```text
Monitor = DOWN
Alert = FAILED
```

Difference:
```text
Incident = what happened to the monitored service
Alert    = notification about that incident
```

Future concern:
- Duplicate email if delivery succeeds but Alert Service crashes before marking `SENT`.

## 11. Frontend

Responsibilities:
- Create monitors
- Show validation errors
- Show monitor status
- Show recent checks
- Show incidents
- Render pulse/latency visualization

Initial data:
```text
GET /monitors/{id}
GET /monitors/{id}/checks?limit=50
GET /monitors/{id}/incidents?limit=10
```

Live updates:
```text
MVP: polling
Later: SSE
Possible: WebSocket
```

SSE is attractive because updates are mostly:
```text
Backend -> Frontend
```

## 12. Pulse Visualization

Desired concept:
```text
------/\--------/\--------/\------
      180ms     220ms     160ms
```

Possible mapping:
- horizontal position -> check time
- pulse shape/height -> latency
- status -> UP / DEGRADED / DOWN

UDP is not needed; reliable delivery matters more.

## 13. HTTP Safety

Safeguards:
- Minimum check interval
- Request timeout
- Limited retries
- Backoff after failures
- Respect `429 Retry-After`
- Honest `User-Agent`
- Prefer `/health` endpoints when possible

Future concern:
- Per-domain throttling / concurrency limits

## 14. Useful Query / Index

Latest 50 checks:
```text
WHERE monitor_id = ?
ORDER BY checked_at DESC
LIMIT 50
```

Useful index:
```text
(monitor_id, checked_at)
```

## 15. MVP Scope

Build in this order:
```text
1. Create Monitor
2. Save Monitor in PostgreSQL
3. Wait until next_check_at
4. Scheduler enqueues task
5. Worker checks URL
6. Save CheckResult
7. Update Monitor status
8. Create/resolve Incident
9. Display results
10. Add alerts
```

Do not start with:
- Kubernetes
- Terraform
- multiple scheduler instances
- advanced throttling
- distributed tracing
- complicated authentication
- WebSockets

## 16. Open Questions

Still undecided:
- Backend framework
- Queue technology
- Scheduler implementation
- Exact failure threshold
- Minimum check interval
- Request timeout
- Retry/backoff policy
- Duplicate-scheduling strategy
- Full idempotency strategy
- Alert retry/deduplication
- Data-retention policy
- Authentication/user ownership
- Polling vs SSE for final live UI
- Cloud provider/services
- Infrastructure as Code approach

## 17. Learning Rule

For every feature:
```text
Understand
↓
Design
↓
Pseudocode
↓
Implement yourself
↓
Test
↓
Debug/review
↓
Explain what you built
```

AI should act as:
- mentor
- reviewer
- debugger
- design partner

—not as the developer writing the entire project.

## 18. Next Step

Technology selection, starting with the Backend API.

Primary learning priorities:
```text
Backend
Distributed Systems
DevOps
Cloud
```
