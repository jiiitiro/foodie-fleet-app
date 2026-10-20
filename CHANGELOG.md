# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) · Versioning: [SemVer](https://semver.org/).

## [Unreleased]

### Added (M2 restaurant owner)
- Restaurant setup page on first login, and a dashboard with incoming orders, menu and profile.
- Open/close restaurant button.
- Menu item add / edit page / toggle availability / delete, with validation.
- Order status buttons that follow the order lifecycle (`OrderStatus.nextStatusesFor(Role)`).
- `CurrentUserService` to resolve the logged-in user; `ConflictException` (409).

### Security (M2)
- **S2:** all restaurant endpoints are scoped to the logged-in owner; another owner's menu items and orders return 404.

### Changed (M2)
- Menu items that appear in past orders can no longer be deleted (they can be marked unavailable).

### Added
- Restaurant owner module (work in progress): profile management, menu item CRUD, order status updates.
- `MenuItemRequest`, `RestaurantRequest` DTOs.
- Project documentation: `SDLC.md`, `README.md`, `SECURITY.md`, `CONTRIBUTING.md`, `docs/`, `.env.example`.

### Changed (M1.5 stabilisation)
- Schema is now managed by Flyway (`V1__init.sql`); Hibernate runs with `ddl-auto=validate`.
  **Existing dev databases must be reset once:** `docker compose down -v`.
- Configuration split into `dev` (default), `prod` and `test` profiles. `prod` has no credential defaults.
- `OrderStatus`: fixed `DELIVERD` → `DELIVERED`; added `ACCEPTED` and `READY_FOR_PICKUP`.
- Emails are normalised (trimmed, lower-cased) at registration and login.
- Missing resources in the restaurant module return 404 (`ResourceNotFoundException`).

### Added (M1.5 stabilisation)
- Bean Validation on registration, restaurant and menu-item forms, with field errors on the register page.
- `GlobalExceptionHandler` and `error.html`.
- Login page messages for unverified accounts, successful verification and invalid tokens.
- `restaurants.active` column; restaurant description is now saved.
- Testcontainers-based integration tests; `./mvnw verify` no longer needs a local database.

### Fixed
- App failed to start: `MenuItemRepository` method typo, wrong `Order.items` mapping, `APP_BASE_URL` required outside Docker.
- `users.email` now has a unique constraint.

### Security
- **S1:** self-registration can no longer create `ADMIN` accounts (regression-tested).

### Removed
- Empty stubs: `JwtTokenProvider`, `JwtResponse`, `LoginRequest`, `AsyncConfig`.

### Known issues
- ~~Restaurant module still uses a placeholder owner ID and has no ownership checks (SECURITY.md S2).~~ Fixed in M2.

## [0.0.1] — 2026-10-09 (untagged)

### Added
- Core JPA entities: User, Restaurant, MenuItem, Order, OrderItem, VerificationToken.
- Spring Data repositories.
- Registration, login/logout and role-based redirects (Spring Security, BCrypt).
- Asynchronous email verification workflow.
- Dockerfile and Docker Compose setup (Postgres 15).
