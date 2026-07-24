# Pinned Application Tracker contract

Job Matching generates its Application Tracker client from the exact
producer-owned OpenAPI `1.1.0` document merged at the immutable revision in
`application-tracker.SOURCE`. `SHA256SUMS` protects the imported bytes.

The OpenAPI Generator version and generated Java packages are pinned in
`pom.xml`. Generated sources and binaries remain under `target/`; they are
never committed or copied between repositories.

To update the client:

1. merge and review the producer-owned contract change;
2. copy `contracts/openapi.json` from that exact immutable revision;
3. update the `.SOURCE` revision/checksum and `SHA256SUMS`;
4. run both contract-policy scripts and `mvn -B clean verify`;
5. review generated API/model changes, reader-token behavior and mapping tests;
6. merge only after producer and consumer verification are green.

Breaking producer changes require an explicit major-version review and a
coordinated consumer change. Rollback restores the previous contract snapshot,
provenance and compatible adapter together, then regenerates from source.
