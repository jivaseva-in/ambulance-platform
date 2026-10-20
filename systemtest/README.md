# Ambulance Platform System Tests

This project is an independent API system-test module for the ambulance platform backend.
It is intentionally separate from the main `backend` Maven module so tests can run against a live application instance without mixing test code into the production service build.

## Goals

- Validate backend REST APIs against a running service
- Keep a clear separation between production code and system tests
- Support future GUI/mobile automation using the same reusable API helpers
- Use deterministic test data and cleanup strategy for repeatable runs

## Preferred Stack

- JUnit 5
- RestAssured
- Java 21

## Run locally

1. Start the backend service:
   ```bash
   cd backend
   mvn clean spring-boot:run
   ```

2. From this module, run the suite:
   ```bash
   cd systemtest
   mvn test
   ```

3. Optional overrides:
   ```bash
   mvn test \
     -Dsystemtest.base-url=http://localhost:8080 \
     -Dsystemtest.admin-email=<your-admin-email> \
     -Dsystemtest.admin-password=<your-admin-password>
   ```

## Configuration

The repository keeps a template at `src/test/resources/systemtest.properties.example`.
For local execution, create your real file at `src/test/resources/systemtest.properties` by copying the example and replacing the placeholder values.

Example:
```bash
copy src/test/resources/systemtest.properties.example src/test/resources/systemtest.properties
```

The real file is ignored by Git and should contain your local credentials, not committed values.

The default settings are loaded from `src/test/resources/systemtest.properties`.
If you need local overrides, copy the file and change the values, or pass JVM properties as shown above.

## Cleanup Strategy

Generated test users should be prefixed with `DELETABLE_` and all related booking records should be cleaned up after execution. This keeps the test environment isolated and prevents flaky tests from leaving data behind.

## Future expansion

This module is intentionally structured to support future additions like:
- web GUI automation
- mobile automation
- performance checks
- contract validation against API schemas
