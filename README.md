# FoodieFleet 🍔🚚

A food-delivery platform built with Spring Boot. Customers order from
restaurants, restaurant owners manage menus and orders, and drivers deliver them.

> **Status:** early development (`0.0.1-SNAPSHOT`). See [SDLC.md](SDLC.md) for the
> roadmap and the list of known defects.

## Features

| Role | Available now | Planned |
|---|---|---|
| All | Registration, email verification, login/logout, role-based redirect | Password reset |
| Restaurant owner | Profile + menu CRUD (in progress) | Order handling dashboard |
| Customer | Landing page | Browse, cart, checkout, order tracking |
| Driver | Landing page | Claim and deliver orders |
| Admin | — | User/restaurant moderation, metrics |

## Tech stack

- Java 17, Spring Boot 3.2 (Web MVC, Security, Data JPA, Mail, Thymeleaf)
- PostgreSQL 15
- Maven (wrapper included), Lombok
- Docker & Docker Compose

## Quick start

### Prerequisites
- JDK 17+
- Docker (for Postgres, or for running everything)

### 1. Configure environment

```bash
cp .env.example .env
# then edit .env: set DB credentials and your SMTP (Gmail app password) details
```

`.env` is git-ignored. Never commit real credentials.

### 2a. Run everything in Docker

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| App | http://localhost:8081 |
| Postgres | `localhost:5433` |

### 2b. Run the app locally (IDE or Maven)

```bash
./mvnw spring-boot:run      # or run AppApplication from IntelliJ
```

Spring Boot's [Docker Compose support](https://docs.spring.io/spring-boot/reference/features/dev-services.html)
starts Postgres from `compose.dev.yaml` automatically and connects using the credentials in your
`.env`, so you don't need to start the database or export env vars yourself. Docker must be running.
The database keeps running after the app stops; stop it with `docker compose stop postgres`.

The app runs on http://localhost:8080. For working email links and sending mail, the app process
still needs `APP_BASE_URL` and `MAIL_*` (IntelliJ EnvFile plugin, or `set -a; source .env; set +a`).

### 3. Try it
1. Open `/auth/register` and create an account.
2. Click the link in the verification email.
3. Log in at `/auth/login`. You are redirected to your role's dashboard.

## Configuration

| Variable | Required | Description |
|---|---|---|
| `POSTGRES_DB` | yes | Database name |
| `POSTGRES_USER` | yes | Database user |
| `POSTGRES_PASSWORD` | yes | Database password |
| `APP_BASE_URL` | yes | Public URL used in email links (e.g. `http://localhost:8081`) |
| `MAIL_USERNAME` | yes | SMTP username (Gmail address) |
| `MAIL_PASSWORD` | yes | SMTP password ([Gmail App Password](https://support.google.com/accounts/answer/185833)) |
| `SPRING_PROFILES_ACTIVE` | no | `dev` (default) or `prod` |
| `RESEND_API_KEY` | no | Reserved for a future switch to Resend |

## Project layout

```
src/main/java/com/foodiefleet/app
├── config/        # Security, async configuration
├── controller/    # MVC controllers (one per role + auth)
├── dto/           # Request/response objects
├── exception/     # Exceptions and global handler
├── model/         # JPA entities and enums
├── repository/    # Spring Data JPA repositories
├── security/      # UserDetailsService (JWT planned)
└── service/       # Business logic
src/main/resources
├── application.properties
└── templates/     # Thymeleaf views
```

## Development

```bash
./mvnw clean verify     # build + tests
./mvnw spring-boot:run  # run
```

Tests use [Testcontainers](https://testcontainers.com/), so Docker must be running. No
local database or `.env` is needed for `./mvnw verify`.

### Profiles

| Profile | When | Notes |
|---|---|---|
| `dev` | default when `SPRING_PROFILES_ACTIVE` is unset | Local defaults (`localhost:5433`, `postgres/postgres`), SQL logging |
| `test` | automated tests | Datasource from Testcontainers, mail mocked |
| `prod` | production | No defaults: requires `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `APP_BASE_URL`, `MAIL_*` |

### Database migrations

The schema is managed by Flyway (`src/main/resources/db/migration`). To change it, add a new
`V<n>__description.sql`. Never edit a migration that has already been applied.

> **Upgrading from a pre-Flyway checkout?** Your old dev database was created by Hibernate and
> Flyway will refuse to start on it. Reset it once with `docker compose down -v`. This deletes the
> local Postgres volume.

## Documentation

| Doc | Purpose |
|---|---|
| [SDLC.md](SDLC.md) | Master plan, current-state analysis, roadmap |
| [docs/01-requirements.md](docs/01-requirements.md) | Functional and non-functional requirements |
| [docs/02-architecture.md](docs/02-architecture.md) | Architecture, data model, order lifecycle |
| [docs/03-implementation.md](docs/03-implementation.md) | Coding standards and patterns |
| [docs/04-testing.md](docs/04-testing.md) | Test strategy |
| [docs/05-deployment.md](docs/05-deployment.md) | Environments, Docker, CI/CD, release process |
| [docs/06-maintenance.md](docs/06-maintenance.md) | Monitoring, backups, upgrades |
| [SECURITY.md](SECURITY.md) | Security policy, threat model, known issues |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branching, commits, PR process |
| [CHANGELOG.md](CHANGELOG.md) | Release history |

## License

No license has been chosen yet. All rights reserved until one is added.
