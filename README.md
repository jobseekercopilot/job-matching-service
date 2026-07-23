# Job Matching Service

Job Matching Service enriches Job Service search results with a user’s existing
Application Tracker state. It sits after provider aggregation,
normalisation/deduplication and distance enrichment, and before Job Service
returns search results.

It does not find jobs, persist jobs, own application records, or store
documents.

## Runtime flow

`Job Finder Gateway → Job Service → Job Matching Service → Application Tracker`

Job Service calls:

```text
POST /api/v1/job-matches/enrich
```

The service queries Application Tracker and marks each returned job with the
matched application identifier, status, document references, and timestamps.
See `contracts/openapi.json`.

## Build

Requires Java 17 and Maven.

```bash
mvn clean verify
mvn spring-boot:run
```

The service listens on port `8097` and expects Application Tracker at
`http://localhost:8088` unless
`APPLICATION_TRACKER_SERVICE_BASE_URL` is set.

The legacy workspace included an unused local generated-client JAR dependency.
The extraction removed that binary and its `systemPath`: production code uses a
manual `RestTemplate` client, and the clean repository now builds from declared
dependencies. Replacing the manual client with a versioned generated package is
tracked as future work.

## Readiness

This baseline preserves inherited behavior and is not a private-beta approval.
Identity trust, matching ambiguity, bounded performance, downstream failure
semantics, contract ownership, privacy-safe telemetry, container hardening, and
full-path tests remain in the Job Search epic.

See `docs/BETA_READINESS_AUDIT.md`.

## Licence

Proprietary. See `LICENSE`.
