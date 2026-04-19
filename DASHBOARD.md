# Progress Dashboard

> Update this after completing each phase. Track your pace and streak.

---

## Phase Completion Summary

| Phase | Topic                          | Days      | Done  | Total   | %      | Completed On |
| ----- | ------------------------------ | --------- | ----- | ------- | ------ | ------------ |
| 1     | Spring Core Internals          | 1–21      | 0     | 21      | 0%     |              |
| 2     | Data Layer Deep Dive           | 22–42     | 0     | 21      | 0%     |              |
| 3     | Web Layer Internals            | 43–56     | 0     | 14      | 0%     |              |
| 4     | Async, Scheduling, Concurrency | 57–77     | 0     | 21      | 0%     |              |
| 5     | Production Problem Solving     | 78–105    | 0     | 28      | 0%     |              |
| 6     | Spring Security + OAuth 2.0    | 106–133   | 0     | 28      | 0%     |              |
| 7     | Spring Cloud                   | 134–161   | 0     | 28      | 0%     |              |
| 8     | Tech Lead Mastery              | 162–180   | 0     | 19      | 0%     |              |
|       | **TOTAL**                      | **1–180** | **0** | **180** | **0%** |              |

---

## Personal Stats

| Metric               | Value                               |
| -------------------- | ----------------------------------- |
| Started On           | (fill when Day 1 done)              |
| Current Day          | 0 / 180                             |
| Total Hours Invested | 0 hrs                               |
| Current Streak       | 0 days                              |
| Longest Streak       | 0 days                              |
| Last Active          |                                     |
| Estimated Completion | (calculate: today + remaining days) |

---

## Weekly Pace Log

> Every Sunday, write how many days you completed that week.

| Week # | Dates | Days Completed | Running Total | Notes |
| ------ | ----- | -------------- | ------------- | ----- |
| 1      |       |                |               |       |
| 2      |       |                |               |       |
| 3      |       |                |               |       |
| 4      |       |                |               |       |
| 5      |       |                |               |       |
| 6      |       |                |               |       |
| 7      |       |                |               |       |
| 8      |       |                |               |       |
| 9      |       |                |               |       |
| 10     |       |                |               |       |
| 11     |       |                |               |       |
| 12     |       |                |               |       |

---

## Production Scenarios Mastered

> Tick these off as you encounter each type of problem in real work.

### Memory & Resource Issues

- [ ] `OutOfMemoryError` on heap — diagnosed with MAT heap dump
- [ ] `ThreadLocal` leak across requests — fixed with `try/finally remove()`
- [ ] Unbounded cache causing OOM — fixed with Caffeine + TTL
- [ ] Static collection growing forever — leaked by listener ref
- [ ] HikariCP pool exhaustion — fixed with tuning + leak detection

### Threading & Concurrency

- [ ] Deadlock — diagnosed with `jstack`, fixed with lock ordering
- [ ] Livelock — diagnosed with thread dump pattern
- [ ] `@Async` exception swallowed — fixed with `AsyncUncaughtExceptionHandler`
- [ ] `SecurityContext` empty in async thread — fixed with delegation executor
- [ ] Thread pool saturated — fixed with queue + rejection policy tuning

### Transaction & Data

- [ ] Self-invocation trap bypasses `@Transactional` — fixed with proxy or extract
- [ ] Checked exception silently skips rollback — fixed with `rollbackFor`
- [ ] N+1 query — fixed with `JOIN FETCH` / `@EntityGraph`
- [ ] `@Version` absent → lost update in concurrent edit — fixed with optimistic lock
- [ ] Connection held too long by `OpenSessionInView` — disabled it

### Scheduling & Distributed

- [ ] Cron job runs on every cluster node — fixed with ShedLock
- [ ] Scheduled task overlaps itself — fixed with `fixedDelay` or lock
- [ ] Quartz job lost on restart — fixed with JDBC `JobStore`
- [ ] Kafka duplicate message processing — fixed with idempotency table
- [ ] Outbox event not published on TX rollback — fixed with transactional outbox

### Security

- [ ] JWT `alg: none` attack blocked at Resource Server
- [ ] Token not revoked after user banned — fixed with Redis blacklist
- [ ] CSRF on state-changing endpoint — properly configured
- [ ] CORS misconfigured — allowing any origin — fixed
- [ ] BCrypt cost too low → brute force risk — cost factor tuned

### Performance & Availability

- [ ] Slow DB query starves connection pool — fixed with query optimisation + timeout
- [ ] HTTP client with no timeout → thread stuck — fixed with timeouts
- [ ] DB-level deadlock — detected in Postgres logs, fixed lock order
- [ ] GC pressure causing latency spikes — tuned G1GC / ZGC
- [ ] Circuit breaker absent → cascading failure — fixed with Resilience4j
- [ ] Cache stampede → DB overwhelmed — fixed with probabilistic expiration

---

## Key Commands Reference (build this as you go)

```bash
# Thread dump
jstack <pid>
jcmd <pid> Thread.print

# Heap dump
jmap -dump:format=b,file=heap.hprof <pid>
jcmd <pid> GC.heap_dump heap.hprof

# Native memory
jcmd <pid> VM.native_memory detail

# CPU per thread
top -H -p <pid>

# GC log (add to JVM args)
-Xlog:gc*:file=/var/log/app/gc.log:time,uptime:filecount=5,filesize=20m

# Heap dump on OOM (add to JVM args)
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/app/

# Startup info
--debug (shows auto-config report)
GET /actuator/startup

# Hikari metrics
GET /actuator/metrics/hikaricp.connections.active
GET /actuator/metrics/hikaricp.connections.pending
```

---

## Concepts That Surprised Me Most

> Fill this in as you go — this becomes your personal "gotchas" list for tech lead interviews and code reviews.

1.
2.
3.
4.
5.
