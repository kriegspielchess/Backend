# Academy Backend

REST API for the Academy application, built with Spring Boot, Spring Security, Spring Data JPA, and MySQL. The API supports account and password-reset flows, course management, student payment submissions, and administrative student/payment review.

## Requirements

- Java 25
- Maven
- MySQL (for local application runs)

Tests use an in-memory H2 database and do not require MySQL.

## Local setup

1. Create the database:

   ```sh
   mysql -u root -p < create-academy-database.sql
   ```

2. Configure the environment variables in your shell or IDE run configuration. `.env.example` lists the supported variables; Spring Boot does not load a `.env` file automatically. At minimum, set the database credentials and a private JWT key:

   ```sh
   export DB_URL='jdbc:mysql://localhost:3306/academy?serverTimezone=UTC'
   export DB_USERNAME='root'
   export DB_PASSWORD='your-local-mysql-password'
   export JWT_SECRET="$(python3 -c 'import secrets,base64; print(base64.b64encode(secrets.token_bytes(32)).decode())')"
   ```

   Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` as well to enable password-reset emails. `FRONTEND_URL` controls the reset link destination and defaults to `http://localhost:5173`.

3. Start the application:

   ```sh
   mvn spring-boot:run
   ```

   The API listens on `http://localhost:8080`.

The application creates/updates its JPA schema at startup and seeds starter courses and demo users when the corresponding tables are empty. The current seed includes fixed, weak demo passwords; use only for local development, and remove or replace these credentials and the seed behavior before deploying.

## API overview

All routes are under `/api`.

| Area | Routes | Access |
| --- | --- | --- |
| Authentication | `POST /auth/register`, `POST /auth/login`, `POST /auth/forgot-password`, `POST /auth/reset-password` | Public |
| Current user | `GET /auth/me` | Authenticated |
| Courses | `GET /courses`, `GET /courses/{id}` | Public |
| Course management | `POST /courses`, `PUT /courses/{id}`, `DELETE /courses/{id}` | Authenticated |
| Student payments | `GET /student/payments`, `POST /student/payments` | Student role |
| Admin student review | `GET /admin/students/pending`, `PATCH /admin/students/{studentId}/verify`, `GET /admin/students/{studentId}/photo` | Admin or super admin |
| Admin payments | `GET /admin/payments`, `PATCH /admin/payments/{paymentId}/status`, `GET /admin/payments/{paymentId}/slip` | Admin or super admin |
| Admin check | `GET /admin/test` | Admin or super admin |

Authenticated requests use a bearer token:

```http
Authorization: Bearer <jwt>
```

Request and response fields are defined by the DTOs in `src/main/java/com/academy/dto`. Validation errors and other API exceptions are handled centrally by `GlobalExceptionHandler`.

## Tests and coverage

Run the unit and integration tests:

```sh
mvn test
```

Run the full Maven verification lifecycle (including the configured JaCoCo coverage check):

```sh
mvn verify
```

The JaCoCo check is configured for minimum 90% line and branch coverage.

## Project layout

- `controller/` - HTTP endpoints
- `dto/` - API request and response types
- `entity/` and `repository/` - JPA domain model and data access
- `service/` - application and business logic
- `security/` - JWT authentication and Spring Security configuration
- `config/` - application configuration and initial data seeding
- `src/main/resources/application.yml` - runtime database, mail, and JWT settings
- `src/main/resources/application-test.yml` - H2 test database settings

## Security notes

Set `JWT_SECRET` to a newly generated Base64-encoded 256-bit key for each deployed environment. Do not rely on the development fallback in `application.yml`. Never commit real credentials or tokens. Configure production database, mail, CORS, and seed-account behavior for the deployment before exposing the service.
