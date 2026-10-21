# 03 — Implementation (SDLC stage 4)

Back to [SDLC.md](../SDLC.md) · Process rules: [CONTRIBUTING.md](../CONTRIBUTING.md)

## 1. Coding standards

- **Java 17**; use records for immutable response DTOs where Lombok isn't needed.
- **Lombok:** prefer `@Getter`/`@Setter` + `@Builder` on entities; avoid `@Data` on entities
  (its `equals`/`hashCode`/`toString` can trigger lazy loading and recursion).
- **Injection:** constructor injection only (`@RequiredArgsConstructor` + `final` fields).
- **Transactions:** on service methods, not controllers. `readOnly = true` for queries.
- **Exceptions:** throw `ResourceNotFoundException` (404), `IllegalStateException` for bad state
  transitions (409), and `IllegalArgumentException` for validation (400). `GlobalExceptionHandler`
  maps them to views.
- **Naming:** entity fields camelCase (`id`, not `Id`); enum constants spelled correctly, because
  they are persisted as strings and renaming one later needs a data migration.
- **Configuration:** read app settings through `@ConfigurationProperties` (`app.*`), not raw env
  var names in `@Value`.
- **Money:** `BigDecimal` with `numeric(10,2)`; never `double`.
- **Time:** `LocalDateTime` in UTC, or `Instant`; set via `@PrePersist` / `@PreUpdate`.

## 2. Implementation order per milestone

For each feature, work bottom-up:

1. Flyway migration (`src/main/resources/db/migration/V<n>__<desc>.sql`)
2. Entity / enum changes
3. Repository methods (+ repository test if custom query)
4. Service logic + unit tests (including authorization-negative cases)
5. DTOs with validation annotations
6. Controller + Thymeleaf template
7. Integration test (`@SpringBootTest` + MockMvc + Testcontainers)
8. Docs + CHANGELOG

## 3. M1.5 stabilisation: concrete steps

| Step | Change |
|---|---|
| 1 | `MenuItemRepository`: `List<MenuItem> findByRestaurantAndAvailableTrue(Restaurant restaurant);` |
| 2 | `Order.items`: `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)` |
| 3 | `MenuItem`: rename `Id` → `id` |
| 4 | `OrderStatus`: `DELIVERED`, add `ACCEPTED`, `READY_FOR_PICKUP` |
| 5 | `User.email`: `@Column(nullable = false, unique = true)` |
| 6 | `Restaurant`: add `@Builder.Default private Boolean active = true;` and remove the no-op setter; save `description` in the service |
| 7 | `application.properties`: `app.base-url=${APP_BASE_URL:http://localhost:8080}`; `EmailService` uses `${app.base-url}` |
| 8 | `pom.xml`: add `spring-boot-starter-validation`, `flyway-core` (plus `flyway-database-postgresql` on Flyway 10+), `org.testcontainers:postgresql`, `junit-jupiter` (test) |
| 9 | `AuthService.registerUser`: role whitelist (S1) |
| 10 | Split config: `application.properties` (common), `application-dev.properties`, `application-prod.properties`, `src/test/resources/application-test.properties` |
| 11 | Implement `GlobalExceptionHandler` (`@ControllerAdvice`) + `templates/error.html` |
| 12 | `login.html`: show `verified`, `invalid_token`, and disabled-account messages |
| 13 | Delete empty stubs that are not part of M1.5–M7 (`JwtTokenProvider`, `JwtResponse`, `LoginRequest`), or keep them with a `// TODO(M8)` note |

## 4. Patterns

**Current user resolution**
```java
@Service @RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;
    public User get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
```

**Validated form handling**
```java
@PostMapping("/menu/add")
public String add(@Valid @ModelAttribute("menuItemRequest") MenuItemRequest req,
                  BindingResult result, Model model) {
    if (result.hasErrors()) { /* repopulate model */ return "restaurant/dashboard"; }
    restaurantService.addMenuItem(currentUserService.get(), req);
    return "redirect:/restaurant/dashboard";
}
```

**Email after commit**
```java
// AuthService
eventPublisher.publishEvent(new UserRegisteredEvent(user.getEmail(), token));

// EmailService
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(UserRegisteredEvent e) { sendVerificationEmail(e.email(), e.token()); }
```

## 5. Front-end conventions

- **Layouts:** every page declares `layout:decorate="~{layouts/app}"` (or `layouts/auth`) and puts its
  markup in `layout:fragment="content"`. Set the page `<title>`; the layout appends "· FoodieFleet".
- **Components first:** use `btn btn-primary|secondary|ghost|danger|success`, `card` / `card-body`,
  `label` / `input` / `input-error` / `field-error`, `badge`, `alert alert-success|error|info`.
  Add a new component class in `src/main/frontend/app.css` when a pattern repeats three or more times.
- **No dynamic class names:** write full class names in templates (`th:classappend="${x} ? 'btn-danger' : 'btn-primary'"`).
  Tailwind only generates classes it can find literally in the templates.
- **Icons:** `<svg th:replace="~{fragments/icons :: name('size-5')}"></svg>`; add new icons to `fragments/icons.html`.
- **User-supplied images:** always render the placeholder, then the `<img data-fallback>` on top of it.
  `static/js/app.js` removes an image that fails to load, so the placeholder shows through. Don't use
  inline `onerror` handlers; they would break under the planned Content Security Policy.
- **Forms:** keep `th:action` (it adds the CSRF token); show errors with `th:errors` under each field
  and `th:classappend="${#fields.hasErrors('x')} ? 'input-error'"` on the input.
- **Flash messages:** set `success` / `error` with `RedirectAttributes`; render with `~{fragments/alerts :: flash}`.
- **Responsive:** design at 390px first, then add `sm:` / `md:` / `lg:`. No horizontal page scrolling.
- **Brand:** primary actions use `brand-600`; statuses use the colours in `fragments/status.html`.

