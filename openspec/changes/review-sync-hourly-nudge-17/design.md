## Context

One `@Scheduled` method currently runs daily 17:00: sync then always nudge. Need hourly Grafana overwrite without hourly WeCom.

Constraints: Java 8, 8086, POST `notify` unchanged, `sli-snapshot-sync` unchanged.

## Goals / Non-Goals

**Goals:**

- Default `review-sync.cron` hourly at minute 0 Shanghai.
- After successful cron sync, nudge only when local hour is 17.
- `enabled=false` skips sync and cron nudge.

**Non-Goals:**

- Split into two `@Scheduled` methods.
- Change nudge payload, three-send, or `@` rules.
- Change POST `notify` behavior or annual SLI cron.

## Decisions

1. **Single hourly cron + hour gate**  
   `0 0 * * * *` / zone Shanghai. After `runNow()` succeeds, if `LocalDateTime.now(zone).getHour() == 17` then `nudgeAfterSync()`. Avoids double-sync at 17:00.

2. **Keep `review-sync.cron` overridable**  
   If ops set cron back to daily 17:00, the hour==17 gate still nudges that run. If they set a non-17 hourly pattern, cron would never nudge (ops responsibility). Shipped default is hourly.

**Alternative considered:** separate 17:00 nudge-only job. Rejected: race with hourly sync.

## Risks / Trade-offs

- [Grafana load ×24] → same window query; acceptable.  
- [Long sync straddling 17:00] → hour checked after sync completes; if started 16:00 and finishes 17:01, would nudge (rare). Check time at **start** of the job instead so 16:00 run never nudges. **Decision: use trigger/start instant hour.**

## Migration Plan

Ship JAR with new default cron. Rollback previous JAR.

## Open Questions

- None.
