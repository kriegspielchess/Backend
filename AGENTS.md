# Agent Guide

## Project

This repository is a Java 25 / Spring Boot REST API using Maven, Spring MVC, Spring Security, Spring Data JPA, MySQL, and JWT authentication. Tests run against H2 using the `test` profile.

## Structure

- Keep HTTP mapping and transport concerns in `controller/`.
- Put API-specific request and response types in `dto/`; avoid exposing entities when an endpoint already uses a DTO.
- Keep business behavior in `service/`, persistence access in `repository/`, and JPA models in `entity/`.
- Authentication and authorization configuration live in `security/`. Runtime configuration and seed setup live in `config/`.
- Add tests alongside the existing `src/test/java/com/academy` package layout, using the current JUnit and Spring test patterns.

## Working conventions

- Preserve existing package names and Spring conventions. Prefer focused changes over broad refactors.
- Validate request payloads with Jakarta Bean Validation where appropriate and use the existing centralized exception handling.
- Keep authorization behavior explicit. Admin routes are restricted to `ADMIN` or `SUPER_ADMIN`; `/api/student/**` requires `STUDENT`; public course reads and authentication routes are configured in `SecurityConfig`.
- Do not hardcode secrets, credentials, tokens, or environment-specific URLs. Configure runtime values through `application.yml` environment placeholders.
- Treat seeded users and their fixed demo passwords as local-only. Do not introduce or document them as production credentials.
- Avoid changing generated files under `target/`.

## Build and test

- Run tests with `mvn test`.
- Run full verification with `mvn verify`; JaCoCo is configured with 90% line and branch coverage minimums.
- Tests use `application-test.yml` and an in-memory H2 database; local application runs use MySQL settings from `application.yml`.
- When changing security, database mappings, or endpoint behavior, run the most relevant tests and report any checks that could not be run.

## Runtime configuration

The main environment variables are `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and `FRONTEND_URL`. The `.env.example` file is documentation/template only; Spring Boot does not load `.env` automatically. Use environment variables or IDE run configuration, and keep real `.env` files untracked.
