# Contributing

Use a short-lived branch from `develop` and submit changes through review.

Before review:

```bash
mvn clean verify
```

Update `contracts/openapi.json` when the API changes and include focused unit,
controller, contract, security, and failure-path tests. Never commit generated
client JARs, build output, credentials, or user/application data.
