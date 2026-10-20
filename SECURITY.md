# Security Policy

## Supported versions

FoodieFleet is pre-release. Only the latest commit on `main` receives security fixes.

| Version | Supported |
|---|---|
| `main` (latest) | ✅ |
| anything older | ❌ |

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Report privately via GitHub's **"Report a vulnerability"** button (Security tab → Advisories)
on this repository. Include:

- a description of the issue and its impact
- steps to reproduce / proof of concept
- affected endpoint, file or version

You can expect an acknowledgement within **3 working days** and a fix or mitigation plan
within **14 days** for high/critical issues. We will credit reporters unless they prefer
to stay anonymous.

---

## Security architecture

| Control | Implementation | Status |
|---|---|---|
| Authentication | Spring Security form login, HTTP session (`JSESSIONID`) | ✅ |
| Password storage | BCrypt (`BCryptPasswordEncoder`, default strength 10) | ✅ |
| Account activation | Email verification token (UUID v4, 24 h expiry, single use) | ✅ |
| Authorization (role) | URL-based RBAC in `SecurityConfig` (`/customer`, `/restaurant`, `/driver`, `/admin`) | ✅ |
| Authorization (ownership) | Owner-scoped repository queries in services (`findByIdAndRestaurant_Owner_Id`) | ✅ restaurant module (M2) |
| CSRF | Spring Security default, token injected by Thymeleaf `th:action` | ✅ |
| XSS | Thymeleaf `th:text` auto-escaping; never use `th:utext` with user data | ✅ (keep it so) |
| SQL injection | Spring Data JPA derived queries / parameter binding only | ✅ (no native string concat) |
| Input validation | Bean Validation on DTOs | ✅ (M1.5) |
| Rate limiting | — | ❌ missing (S4) |
| Secrets | Environment variables / `.env` (git-ignored) | ✅ |
| Transport | TLS terminated at the reverse proxy in production | ⬜ planned |
| Security headers | Spring defaults (X-Frame-Options, X-Content-Type-Options); CSP/HSTS planned | 🟡 |

## Threat model (STRIDE summary)

| Threat | Example in FoodieFleet | Mitigation |
|---|---|---|
| **S**poofing | Credential stuffing on `/auth/login` | BCrypt, rate limiting, account lockout (M6) |
| **T**ampering | Customer changes item price in checkout form | Server recomputes totals from DB prices (`priceAtPurchase`) |
| **R**epudiation | Admin disables a restaurant, denies it | Audit log of admin and order-status actions (M5) |
| **I**nformation disclosure | Owner views another restaurant's orders (IDOR) | Ownership checks + negative tests |
| **D**enial of service | Registration spam floods SMTP | Rate limit `/auth/register`, CAPTCHA if needed |
| **E**levation of privilege | Registering with `role=ADMIN` | Server-side role whitelist |

## Known issues & remediation plan

These were found during the SDLC analysis on 2026-10-20 and are tracked in
[SDLC.md](SDLC.md#22-defects-found-must-fix-before-new-features).

### S1 — Privilege escalation via registration 🔴 Critical — ✅ fixed in M1.5
`AuthController.registerUser` binds `RegisterRequest.role` straight from the request.
The form only offers three roles, but a hand-crafted POST with `role=ADMIN` is accepted.

**Fix (M1.5):** in `AuthService.registerUser`, reject any role outside
`EnumSet.of(CUSTOMER, RESTAURANT_OWNER, DRIVER)`. Create admins only through a startup
seeder or by an existing admin. Add a regression test.

### S2 — Insecure Direct Object References 🔴 Critical — ✅ fixed in M2
`RestaurantController` endpoints `/menu/add?restaurantId=`, `/menu/edit/{id}`,
`/menu/toggle/{id}`, `/menu/delete/{id}` and `/orders/{id}/status` act on any ID without
checking ownership. `getCurrentUserId()` is also hard-coded to `1L`.

**Fix (M2):** resolve the current `User` from the principal's email; in the service, load
the item/order and verify `item.getRestaurant().getOwner().getId().equals(currentUserId)`.
Return 404 (not 403) to avoid confirming that the resource exists. Derive `restaurantId` from
the owner instead of accepting it as a parameter.

### S3 — Missing input validation 🟠 High — ✅ fixed in M1.5
No `spring-boot-starter-validation`; DTOs accept nulls, negative prices, any password.

**Fix (M1.5):** add Bean Validation; `@Email`, `@NotBlank`, `@Size(min = 8)` for passwords,
`@Positive` for prices, `@Min(1)` for quantities; `@Valid` + `BindingResult` in controllers.

### S4 — No brute-force / abuse protection 🟠 High
Unlimited login attempts and registrations (each sends an email).

**Fix (M6):** Bucket4j (or similar) rate limiting per IP on `/auth/**`; temporary lockout
after N failed logins.

### S5 — Development settings in all environments 🟡 Medium — ✅ fixed in M1.5
`spring.jpa.hibernate.ddl-auto=update`, `show-sql=true`, and fallback credentials
`postgres/postgres` in `application.properties`.

**Fix (M1.5):** profile-specific config; `prod` uses `ddl-auto=validate`, no SQL logging,
and no credential defaults (fail fast if unset).

### S6 — Platform hardening 🟡 Medium
- Container runs as root. Add a non-root `USER` to the Dockerfile.
- Spring Boot 3.2.x is no longer in OSS support. Upgrade to a supported line.
- Postgres port 5433 is published on all interfaces. Bind to `127.0.0.1` in dev and don't publish it in prod.

### S7 — Config drift 🟢 Low
`RESEND_API_KEY` and `DATABASE_URL` are defined but unused. Remove them or wire them up.

### Other observations
- Expired verification tokens are never purged, and there is no "resend verification" flow.
- ~~`User.email` lacks a DB unique constraint.~~ Fixed in M1.5.
- Login reports "account not verified" before checking the password (Spring's default check order),
  so it reveals that an unverified email is registered. Registration already reveals that
  ("Email is already registered"), so this adds no new exposure. Revisit with S4.
- Verification-token links travel over plain HTTP in dev. Ensure `APP_BASE_URL` is `https://` in prod.

## Secure development checklist (use in PRs)

- [ ] Every new endpoint is covered by a role rule in `SecurityConfig` **and** an ownership check in the service, where applicable
- [ ] Tests include wrong-role and wrong-owner cases
- [ ] All request DTOs are validated (`@Valid`)
- [ ] No `th:utext` with user-controlled data
- [ ] No string-concatenated JPQL/SQL
- [ ] Prices/totals computed server-side
- [ ] No secrets, tokens, passwords or full emails written to logs
- [ ] New config/secrets added to `.env.example` with placeholder values only
- [ ] Dependencies added are maintained and free of known CVEs (`./mvnw dependency-check:check` once configured)

## Secrets handling

- Local: `.env` (git-ignored); copy from `.env.example`.
- CI: GitHub Actions encrypted secrets.
- Production: the host's secret manager or environment. Never bake secrets into the image.
- If a secret is ever committed: **rotate it immediately**, then purge it from history.
  Rotation comes first, because a pushed secret must be assumed compromised.
