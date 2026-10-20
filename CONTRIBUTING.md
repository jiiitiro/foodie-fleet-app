# Contributing to FoodieFleet

## Branching model

```
main         ← released, tagged (v1.0.0 …). Only merged from main-dev.
 └ main-dev  ← integration branch; must always build.
    └ feature/<milestone>-<short-name>   e.g. feature/m2-menu-crud
    └ fix/<short-name>                   e.g. fix/menu-repo-typo
    └ security/<short-name>              e.g. security/registration-role-whitelist
```

1. Branch from `main-dev`.
2. Keep branches small: one checklist item from [SDLC.md](SDLC.md) per branch where possible.
3. Open a PR into `main-dev`; squash-merge once green.
4. At the end of a milestone, open a release PR `main-dev → main` and tag it.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <summary in imperative mood>

feat(order): add checkout endpoint
fix(menu): correct repository method name typo
security(auth): restrict self-registration roles
test(restaurant): add ownership negative tests
docs: update deployment guide
chore(deps): bump spring-boot to 3.x.y
```

Scopes: `auth`, `security`, `restaurant`, `menu`, `order`, `customer`, `driver`, `admin`,
`db`, `docker`, `ci`, `deps`.

## Pull request checklist

- [ ] Linked to an issue / SDLC checklist item
- [ ] `./mvnw verify` passes
- [ ] Tests added (incl. authorization-negative tests for new endpoints)
- [ ] Flyway migration added for any schema change
- [ ] `.env.example` updated for new config
- [ ] `CHANGELOG.md` updated under **Unreleased**
- [ ] [SECURITY.md checklist](SECURITY.md#secure-development-checklist-use-in-prs) done if auth/authz/payment/upload code is touched

## Coding standards

See [docs/03-implementation.md](docs/03-implementation.md).
