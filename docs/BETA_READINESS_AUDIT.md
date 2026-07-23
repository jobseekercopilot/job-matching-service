# Job Matching Service beta-readiness audit

Date: 2026-07-23

## Decision

**NO-GO for private beta.** This extraction establishes private repository
ownership and a reproducible source build. It does not close the security,
identity, correctness, resilience, performance, or integration findings below.

## Verified boundary

Job Service calls Job Matching after provider aggregation, canonicalisation,
deduplication, and distance calculation. Job Matching retrieves a user’s
Application Tracker records and enriches the search response. It does not
persist jobs or documents.

## Extraction

The source directory was untracked in the legacy mixed root. The private
baseline includes source, tests, Dockerfile, POM, an OpenAPI snapshot, and
repository governance files. It excludes the local generated Application
Tracker client JAR and all build output.

The generated client dependency was unused by production code; the service
calls Application Tracker with `RestTemplate`. Its `systemPath` and the
Dockerfile’s `libs` copy were removed so a fresh clone can resolve and build
without a workspace binary. Migration to an approved versioned contract/client
remains tracked work.

## Findings

- The public enrichment request trusts a caller-supplied `userId`; there is no
  user/session or service identity and no cross-user authorization evidence.
- Application Tracker is called by raw user path with no authentication header,
  paging, timeout policy, retry/circuit breaker, or connection-failure mapping.
- Matching falls through from canonical ID to provider/external ID and then an
  exact normalized title/company/location heuristic. The fallback has no
  ambiguity rejection and can false-positive common roles.
- Missing provider values default to `REED`, potentially contaminating other
  provider identities. Source aliases and duplicate records are resolved by
  most recent update without an explicit product policy.
- The service mutates inbound job objects and rewrites `id` to a canonical
  value, which can lose source identity. A matched null status becomes
  `DOCUMENTS_GENERATED`.
- Every job repeatedly scans and sorts all application records. Input and
  downstream result sizes are unbounded.
- DTOs lack bean validation, request limits, explicit required fields, and
  precise schemas for salary/location. The OpenAPI snapshot therefore
  understates constraints.
- Logs contain raw user IDs and counts. There are no ambiguity, denial,
  downstream, latency, or match-quality operational controls.
- Existing tests cover only basic provider/external, normalized fallback,
  unmatched, and distance-preservation cases. Controller, cross-user,
  ambiguity, duplicate, boundary, failure, performance, and full-path coverage
  is absent.
- The inherited container runs as root, installs curl in the runtime image, and
  has no pinned digest, SBOM, provenance, or vulnerability evidence.

## Evidence required for a go decision

Complete the Job Matching child issues in the Job Search epic, the linked
Application Tracker identity/contract/lifecycle dependencies, and the final
Job Search integration gate. Validate from clean private clones through the
real browser-to-provider and Application Tracker fixture path. No skipped or
swallowed security, contract, failure, or scale check counts as success.
