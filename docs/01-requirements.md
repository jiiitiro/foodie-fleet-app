# 01 — Requirements (SDLC stage 2)

Back to [SDLC.md](../SDLC.md)

## 1. Stakeholders & roles

| Role | Description |
|---|---|
| Customer | End user ordering food |
| Restaurant owner | Owns exactly **one** restaurant (current `@OneToOne` model) |
| Driver | Delivers orders |
| Admin | Platform operator; never self-registered |

## 2. Functional requirements

IDs are referenced from issues and PRs. **Priority:** M = must (1.0), S = should, C = could (post-1.0).

### Authentication & accounts (`AUTH`)
| ID | Requirement | Pri | Milestone | Status |
|---|---|---|---|---|
| AUTH-1 | Users register with full name, email, password, phone, role (Customer / Owner / Driver) | M | M1 | ✅ |
| AUTH-2 | Accounts stay disabled until the email is verified via a single-use link valid for 24 h | M | M1 | ✅ |
| AUTH-3 | Users log in with email + password and are redirected to their role's home | M | M1 | ✅ |
| AUTH-4 | Self-registration **cannot** create an admin | M | M1.5 | ❌ (S1) |
| AUTH-5 | Users can request a new verification email | S | M1.5 | ⬜ |
| AUTH-6 | Users can reset a forgotten password via email | S | M6 | ⬜ |
| AUTH-7 | Login page shows clear messages for: unverified, verified, invalid token, logout | S | M1.5 | ⬜ |

### Restaurant owner (`REST`)
| ID | Requirement | Pri | Milestone |
|---|---|---|---|
| REST-1 | On first login, the owner is prompted to create a restaurant profile (name, description, address, phone) | M | M2 |
| REST-2 | Owner can open or close the restaurant (`active` flag) | M | M2 |
| REST-3 | Owner can add, edit, delete and toggle availability of menu items (name, description, price, image URL) | M | M2 |
| REST-4 | Owner sees incoming orders, newest first | M | M2 |
| REST-5 | Owner can accept, prepare, mark ready, or cancel an order (allowed transitions only) | M | M2 |
| REST-6 | Owner can only see and modify their own restaurant's data | M | M2 |

### Customer (`CUST`)
| ID | Requirement | Pri | Milestone |
|---|---|---|---|
| CUST-1 | Browse active restaurants; search by name | M | M3 |
| CUST-2 | View a restaurant's available menu items | M | M3 |
| CUST-3 | Cart: add/remove/change quantity; one restaurant per cart | M | M3 |
| CUST-4 | Checkout with a delivery address; total computed server-side | M | M3 |
| CUST-5 | View own order history and live status | M | M3 |
| CUST-6 | Cancel an order while it is still `PLACED` | S | M3 |
| CUST-7 | Receive an order-confirmation email | S | M3 |
| CUST-8 | Rate a delivered order | C | M8 |

### Driver (`DRV`)
| ID | Requirement | Pri | Milestone |
|---|---|---|---|
| DRV-1 | See orders that are `READY_FOR_PICKUP` and unassigned | M | M4 |
| DRV-2 | Claim an order; only one driver can win | M | M4 |
| DRV-3 | Mark order `OUT_FOR_DELIVERY`, then `DELIVERED` | M | M4 |
| DRV-4 | View own active and completed deliveries | M | M4 |

### Admin (`ADM`)
| ID | Requirement | Pri | Milestone |
|---|---|---|---|
| ADM-1 | First admin is created from environment variables on startup | M | M5 |
| ADM-2 | List, search, enable and disable users | M | M5 |
| ADM-3 | Suspend or reinstate restaurants | M | M5 |
| ADM-4 | Dashboard: orders/day, revenue, active drivers | S | M5 |
| ADM-5 | Audit log of admin actions | S | M5 |

## 3. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Security | OWASP Top 10 addressed; see [SECURITY.md](../SECURITY.md) |
| NFR-2 | Security | Passwords hashed with BCrypt; min length 8 |
| NFR-3 | Performance | p95 page response < 500 ms at 50 concurrent users |
| NFR-4 | Reliability | Order placement is transactional; no partial orders |
| NFR-5 | Concurrency | Order claiming and status changes use optimistic locking |
| NFR-6 | Maintainability | Service-layer test coverage ≥ 70 % |
| NFR-7 | Portability | Runs anywhere with Docker; config via environment only (12-factor) |
| NFR-8 | Data integrity | Schema managed by Flyway migrations |
| NFR-9 | Privacy | No passwords, tokens or full addresses in logs |
| NFR-10 | Usability | Responsive pages, usable on mobile widths |
| NFR-11 | Availability | Health endpoint for orchestration; graceful shutdown |

## 4. Out of scope for 1.0
Online payments, real-time map tracking, multi-restaurant owners, mobile apps, i18n.

## 5. Key user stories (acceptance criteria)

**US-M3-04 — Place an order**
> As a customer, I want to check out my cart so that the restaurant receives my order.

- Given a cart with items from restaurant R that is active,
- when I submit a delivery address,
- then an `Order` is created with status `PLACED`, each `OrderItem.priceAtPurchase` equals
  the current DB price, `totalPrice` = Σ(price × qty), and the cart is emptied.
- Unavailable items or a closed restaurant → the order is rejected with a message.

**US-M4-02 — Claim a delivery**
> As a driver, I want to claim a ready order so that I can deliver it.

- Given an order in `READY_FOR_PICKUP` with no driver,
- when two drivers claim it at the same time,
- then exactly one succeeds and the other sees "Order already taken".
