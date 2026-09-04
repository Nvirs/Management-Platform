# Cloud-Native Event Management Platform

[![CI](https://github.com/Nvirs/Management-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Nvirs/Management-Platform/actions/workflows/ci.yml)

A microservice platform where users can register, create events, sign up for them, and get
notified when they do built to demonstrate a microservice architecture:
independent services in three different stacks, a shared JWT auth model, async
messaging between services, an API gateway, and a fully hardened local
container stack.

## Architecture

```
client
  │
  ▼
gateway-service          :8080  Spring Cloud Gateway
  │  routes /api/** downstream, validates the JWT at the edge
  │
  ├──► auth-service          :8081  Spring Boot   register / login / me → issues JWT
  ├──► event-service         :8082  Spring Boot   event CRUD, organizer-only update/delete
  └──► registration-service  :8083  FastAPI       register/cancel for an event
         • looks up the event live via event-service's GET /api/events/{id}
           (capacity + existence — no data duplication)
         • on success, publishes "registration.confirmed"

auth-service / event-service / registration-service  ──►  PostgreSQL  (shared "eventplatform" DB)

RabbitMQ  (topic exchange "events")
  └──► notification-service  :8084  Node.js   consumes registration.confirmed, logs a confirmation
```

All three JVM services and the gateway share one `JWT_SECRET`, so a token issued
by `auth-service` is validated locally everywhere first at the gateway edge,
then again by whichever service handles the request (defense in depth), with no
callback to `auth-service`. `registration-service` doesn't duplicate event data;
it looks up the event live from `event-service` on every registration.

## Services

| Service | Stack | Port | Status |
|---|---|---|---|
| [`gateway-service`](gateway-service) | Spring Boot 3 / Java 21, Spring Cloud Gateway | `8080` | Ready |
| [`auth-service`](auth-service) | Spring Boot 3 / Java 21 | `8081` | Ready |
| [`event-service`](event-service) | Spring Boot 3 / Java 21 | `8082` | Ready |
| [`registration-service`](registration-service) | FastAPI / Python 3.12 | `8083` | Ready |
| [`notification-service`](notification-service) | Node.js 22 | `8084` | Ready |

Each service has its own README with its full endpoint list and environment
variables.

### Key API endpoints (through the gateway, `http://localhost:8080`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | – | Create an account, returns a JWT |
| POST | `/api/auth/login` | – | Returns a JWT |
| GET | `/api/auth/me` | Bearer | Current user |
| GET | `/api/events` | – | List all events |
| GET | `/api/events/{id}` | – | Get one event |
| POST | `/api/events` | Bearer | Create an event (caller becomes organizer) |
| PUT / DELETE | `/api/events/{id}` | Bearer, organizer only | Update / delete an event |
| POST | `/api/registrations` | Bearer | Register for an event |
| GET | `/api/registrations/me` | Bearer | Your registrations |
| DELETE | `/api/registrations/{id}` | Bearer, owner only | Cancel a registration |
| GET | `/api/events/{id}/registrations` | Bearer, organizer only | List an event's participants |

## Quickstart — run the whole stack

Prerequisites: Docker (or [colima](https://github.com/abiosoft/colima) on macOS)
with Docker Compose.

```bash
cd infra
./generate-secrets.sh        # generates infra/secrets/*.txt (gitignored), safe to re-run
docker compose up -d --build
docker compose ps            # wait until everything is "healthy"
```

This starts PostgreSQL, RabbitMQ, and all five services, each hardened
(non-root user, read-only root filesystem, dropped Linux capabilities,
CPU/memory limits — see [`infra/README.md`](infra/README.md) for details).

Stop everything with `docker compose down` (add `-v` to also wipe the database
and RabbitMQ volumes).

## Try it end-to-end

Once the stack is healthy, everything goes through the gateway on `:8080`:

```bash
BASE=http://localhost:8080

#register an organizer and grab a token
ORG_TOKEN=$(curl -s $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"organizer@example.com","password":"password123"}' | jq -r .token)

# create an event
EVENT_ID=$(curl -s $BASE/api/events -H "Authorization: Bearer $ORG_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Launch Party","startTime":"2027-01-01T18:00:00Z","endTime":"2027-01-01T22:00:00Z","capacity":50}' \
  | jq -r .id)

#register a second user for that event
USER_TOKEN=$(curl -s $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"guest@example.com","password":"password123"}' | jq -r .token)

curl -s $BASE/api/registrations -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' -d "{\"event_id\":\"$EVENT_ID\"}"

#the organizer can see who signed up
curl -s $BASE/api/events/$EVENT_ID/registrations -H "Authorization: Bearer $ORG_TOKEN"

#an unauthenticated request to a protected route is rejected at the gateway
curl -s -o /dev/null -w '%{http_code}\n' -X POST $BASE/api/registrations \
  -H 'Content-Type: application/json' -d '{"event_id":"x"}'   # -> 401
```

Watch the confirmation get consumed asynchronously:

```bash
docker compose logs -f notification-service
# [notification] confirmation email -> guest@example.com (registrationId=..., eventId=..., confirmedAt=...)
```

## Running the automated tests

Every service has its own test suite, runnable independently — no Docker or
running dependencies required 
This is exactly what [CI](.github/workflows/ci.yml) runs on every push/PR to `main`.

```bash
# Java services (auth-service, event-service, gateway-service) — H2 in-memory DB, no Postgres needed
cd auth-service && ./gradlew test
cd event-service && ./gradlew test
cd gateway-service && ./gradlew test

# registration-service  SQLite in-memory 
cd registration-service
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements-dev.txt
pytest -v

# notification-service Node's built-in test runner
cd notification-service
npm ci
npm test
```

## Project structure

```
.
├── gateway-service/       Spring Cloud Gateway — routing + edge JWT validation
├── auth-service/          register / login / me, issues JWT
├── event-service/         event CRUD, organizer authorization
├── registration-service/  event sign-up, capacity checks, publishes to RabbitMQ
├── notification-service/  consumes RabbitMQ, logs confirmations
├── infra/                 docker-compose.yml + secrets generation + infra docs
├── k8s/                   Kubernetes manifests (planned)
└── .github/workflows/     CI pipeline
```

## Tech stack

**In use today**

- Spring Boot 3 / Java 21 + Spring Cloud Gateway (gateway, auth, event)
- FastAPI / Python (registration)
- Node.js (notification)
- PostgreSQL, RabbitMQ
- Docker & Docker Compose (secrets, non-root, read-only rootfs, capability dropping, resource limits)
- GitHub Actions CI


