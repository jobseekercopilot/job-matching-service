# Job Matching Service

Job Matching Service enriches Job Service search results with a user’s existing
Application Tracker state. It sits after provider aggregation,
normalisation/deduplication and distance enrichment, and before Job Service
returns search results.

It does not find jobs, persist jobs, own application records, or store
documents.

Its position between canonical aggregation and Application Tracker enrichment
is defined in the Infrastructure
[Job Search architecture ADR](https://github.com/jobseekercopilot/infrastructure/blob/develop/docs/adr/0001-job-search-architecture-and-ownership.md).

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
./scripts/test-contract-policy.sh
./scripts/verify-contracts.sh
mvn clean verify
mvn spring-boot:run
```

The service listens on port `8097` and expects Application Tracker at
`http://localhost:8088` unless
`APPLICATION_TRACKER_SERVICE_BASE_URL` is set.

`APPLICATION_TRACKER_READER_TOKEN` is required and must contain at least 32
bytes. It is injected only at runtime and sent as exactly one
`X-Service-Token` header. Do not place a real value in source, examples, CI
logs or issue comments.

The Application Tracker client is generated during the Maven build from the
exact producer-owned OpenAPI `1.1.0` snapshot and immutable provenance under
`src/main/openapi`. OpenAPI Generator is pinned to `7.5.0`; generated sources
and binaries remain under `target/` and are never committed. Contract scripts
fail closed on missing inputs, checksum/provenance drift, removed security or
error responses, and weakened required fields.

The legacy workspace included an unused local generated-client JAR dependency.
The extraction removed that binary and its `systemPath`; MATCH-02 now replaces
the handwritten transport adapter with reproducible generation from source.

## Readiness

This baseline preserves inherited behavior and is not a private-beta approval.
Identity trust, matching ambiguity, bounded performance, downstream failure
semantics, contract ownership, privacy-safe telemetry, container hardening, and
full-path tests remain in the Job Search epic.

See `docs/BETA_READINESS_AUDIT.md`.

## Licence

Proprietary. See `LICENSE`.
