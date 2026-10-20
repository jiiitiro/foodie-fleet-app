# 04 — Testing (SDLC stage 5)

Back to [SDLC.md](../SDLC.md)

## 1. Current state (after M2.5)
41 tests, all passing with `./mvnw verify` (Docker required):

| Test class | Type | Covers |
|---|---|---|
| `AppApplicationTests` | Integration | Context starts; Flyway schema matches entities (`ddl-auto=validate`) |
| `service/AuthServiceTest` | Unit (Mockito) | Admin/missing role rejected, duplicate email, email normalisation, disabled user + verification email |
| `model/OrderStatusTest` | Unit | Allowed/forbidden status transitions per role |
| `controller/RestaurantControllerIntegrationTest` | Integration | Setup and profile, open/close, menu CRUD and validation, delete refused for items in past orders, order status flow, **wrong-owner → 404 on every endpoint** |
| `security/SecurityIntegrationTest` | Integration (MockMvc + Testcontainers) | Registration (S1 regression, validation, CSRF), login outcomes (unverified / wrong password / success), role-based route access, login messages, 404 error page, public landing page, register role pre-selection |

Shared setup: `TestcontainersConfiguration` (Postgres 15) + `@ActiveProfiles("test")`, with
`EmailService` mocked. Integration classes share one context and database, so each test starts with
`TestDatabase.truncateAll(...)`. `pom.xml` overrides `testcontainers.version` to 1.21.4 because the
1.19.7 that Boot 3.2.5 pins can't talk to Docker Engine 29+.

## 2. Test pyramid

| Level | Tooling | Scope | Target |
|---|---|---|---|
| Unit | JUnit 5, Mockito, AssertJ | Services, order state machine, price calculation | Fast, no Spring context |
| Slice | `@DataJpaTest` + Testcontainers | Custom repository queries, constraints | Real Postgres |
| Web slice | `@WebMvcTest` + `spring-security-test` | Controllers, validation, role rules | Mocks services |
| Integration | `@SpringBootTest` + MockMvc + Testcontainers | End-to-end flows per role | Real DB, mail mocked |
| Manual / exploratory | Browser | UI flows before each release | Release checklist |

## 3. Setup

```xml
<!-- pom.xml, test scope -->
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-testcontainers</artifactId>
  <scope>test</scope>
</dependency>
```

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean @ServiceConnection
    PostgreSQLContainer<?> postgres() { return new PostgreSQLContainer<>("postgres:15-alpine"); }
}
```

Mock `JavaMailSender` with `@MockBean` in integration tests so no real email is sent.

## 4. Mandatory test cases

### Security / authorization (every module)
- [ ] Anonymous → protected URL redirects to `/auth/login`
- [ ] Wrong role → 403 (e.g. CUSTOMER on `/restaurant/dashboard`)
- [x] **Wrong owner → 404** on menu edit/toggle/delete and order status (S2)
- [ ] Registration with `role=ADMIN` is rejected (S1)
- [ ] POST without CSRF token → 403
- [ ] Unverified user cannot log in

### Auth
- [ ] Register → user disabled, token created, email sent (mock verified)
- [ ] Duplicate email rejected
- [ ] Valid token enables the user and deletes the token; expired or unknown token fails
- [ ] Post-login redirect per role

### Restaurant
- [ ] Create/update profile; menu CRUD with validation (negative price rejected)
- [ ] Only allowed status transitions succeed

### Orders
- [ ] Total computed from DB prices, ignoring client-supplied prices
- [ ] Unavailable item or closed restaurant → rejected
- [ ] Order placement is atomic (failure mid-way leaves no rows)
- [ ] Concurrent driver claim: exactly one succeeds (`@Version`)

## 5. Coverage & quality gates
- JaCoCo: ≥ 70 % line coverage for `service` package, enforced in `./mvnw verify` from M6.
- CI blocks merge on test failure.

## 6. Release test checklist (manual)
- [ ] Fresh `docker compose up --build` on a clean machine
- [ ] Register one user per role; verify by email; log in
- [ ] Full order flow: owner adds menu → customer orders → owner prepares → driver delivers
- [ ] Logout invalidates the session
- [ ] Error pages render (404, 403, 500) without stack traces
