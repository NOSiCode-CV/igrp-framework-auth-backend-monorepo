# Agent audit and call metrics

Business audit now stores the verified JWT actor chain (`act`) separately from the current `agent`, and includes both in the outbox event. Flyway migration V2 adds `audit_event.act`; older rows keep NULL. Deploy the updated AM consumer before updating producer services.

Delegated calls use `act.sub` as agent. Autonomous SERVICE_ACCOUNT calls use `client_id` (falling back to `sub`), without inventing an actor chain. Only authenticated JWT claims are used; no request headers identify an agent.

The starter includes Actuator and counts incoming authenticated calls through RouteSecurity in Micrometer counter `igrp.agent.calls`, tagged by `service` and `agent`. Counting is before HTTP authorization and independent of audit writes, so subsequent 403 responses are included. Anonymous requests and authentication failures are not counted. Async/error redispatches are not counted again.

Expose `metrics` through the service's existing secured Actuator configuration to query `/actuator/metrics/igrp.agent.calls?tag=agent:agent-a`. This change does not make management endpoints public. The series appears after the first request by that agent.

With the Prometheus registry and endpoint enabled by the service, call rate is:

```promql
sum by (service, agent) (rate(igrp_agent_calls_total[5m]))
```

Publish/install the updated starter and upgrade consumers to activate these changes. No anomaly alerts are included. Audit and agent metric unit tests are included in the non-Docker test run. The PostgreSQL integration assertions for the actor chain in the event and outbox are compiled but excluded from execution at the user's request.
