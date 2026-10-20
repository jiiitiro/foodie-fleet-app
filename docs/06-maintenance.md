# 06 — Maintenance & Operations (SDLC stage 7)

Back to [SDLC.md](../SDLC.md)

## 1. Monitoring

| What | How |
|---|---|
| Liveness/readiness | Spring Boot Actuator `/actuator/health` (liveness + readiness groups) |
| Metrics | Micrometer → Prometheus/Grafana (optional): request latency, error rate, orders placed |
| Logs | Structured JSON logs to stdout; aggregated by the host. **No PII/secrets** |
| Alerts | Health check failing, 5xx rate > 1 %, email send failures, DB connections exhausted |

## 2. Backups

- `pg_dump` daily, retained 14 days; weekly retained 8 weeks.
- Store off-host and encrypted.
- **Test a restore every month**. A backup that has never been restored is not a backup.

```bash
docker exec foodiefleet-db pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB" > backup_$(date +%F).dump
docker exec -i foodiefleet-db pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean < backup_YYYY-MM-DD.dump
```

## 3. Dependency & platform upkeep

| Task | Frequency |
|---|---|
| Dependabot PRs (Maven, Docker, GitHub Actions) reviewed | Weekly |
| Spring Boot patch upgrades | Monthly |
| Spring Boot minor/major upgrade (stay within OSS support) | Each new line, within ~3 months |
| Base image (`eclipse-temurin`, `postgres`) refresh | Monthly |
| Review [SECURITY.md](../SECURITY.md) known issues | Each milestone |

> The project currently pins Spring Boot **3.2.5**, which is past OSS support. The upgrade is scheduled in M6.

## 4. Data housekeeping

- Scheduled job (`@Scheduled`) to purge expired `verification_tokens` and disabled accounts
  that were never verified after N days.
- Archive or partition `orders` once the volume justifies it.

## 5. Incident response

1. **Detect**: alert or user report.
2. **Triage**: severity (S1 outage / data leak, S2 degraded, S3 minor).
3. **Mitigate**: roll back the image, disable the feature, rotate secrets if exposed.
4. **Resolve**: fix forward through the normal PR flow, with an expedited review.
5. **Post-mortem** within 5 days for S1/S2: timeline, root cause, actions. Store it in `docs/incidents/`.

For security incidents, also follow [SECURITY.md](../SECURITY.md).

## 6. Change management
All changes go through PR → CI → review → `main-dev` → release, as described in
[CONTRIBUTING.md](../CONTRIBUTING.md). Hotfixes branch from `main` (`hotfix/<name>`), merge
to `main` with a patch tag, then back-merge to `main-dev`.
