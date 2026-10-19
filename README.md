# equis-api

Spring Boot backend of the equis riding-school app. It serves the API under `/api` on port 8080.

## Environment variables (prod)

With `SPRING_PROFILES_ACTIVE=prod`, the API reads these from the environment, with no defaults. If one is missing or blank, startup stops with `Missing required environment variable: X` before Flyway runs.

| Variable | Purpose | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Selects the production profile (`application-prod.properties`). | `prod` |
| `SPRING_DATASOURCE_URL` | JDBC address of the Postgres database. | `jdbc:postgresql://db:5432/equis` |
| `SPRING_DATASOURCE_USERNAME` | Database user. | `equis` |
| `SPRING_DATASOURCE_PASSWORD` | Database password. | `change-me` |
| `APP_JWT_SECRET` | Base64 HMAC key that signs access tokens; must decode to at least 32 bytes. | output of `openssl rand -base64 32` |
| `APP_BASE_URL` | Public address of the trainer app, used in invite and set-password links. | `https://equis.example.com` |
| `APP_MAIL_MODE` | `smtp` sends emails through Gmail; `log` only prints them to the log. | `smtp` |
| `APP_ADMIN_EMAIL` | Email of the admin account seeded by Flyway (migration V5) on an empty database. | `admin@example.com` |
| `APP_ADMIN_PASSWORD_HASH` | BCrypt hash of the admin's first password, used by the same seed. | `$2a$10$…` (60 characters) |

Needed only when `APP_MAIL_MODE=smtp` (startup stops if one is blank in that mode):

| Variable | Purpose | Example |
|---|---|---|
| `APP_MAIL_FROM` | Sender address; must be the same as `APP_MAIL_USERNAME` for Gmail. | `school@gmail.com` |
| `APP_MAIL_USERNAME` | Gmail address used to sign in to SMTP. | `school@gmail.com` |
| `APP_MAIL_PASSWORD` | Gmail app password (16 characters, spaces removed), never the account password. | `abcdefghijklmnop` |

The admin seed variables are read only when V5 runs on an empty database, but prod always requires them so a fresh database can never be seeded with a missing value.

Health check: `GET /api/actuator/health` returns `{"status":"UP"}` without a token and without details.

## Docker image

```
docker build -t equis-api:dev .
```

The build compiles the jar with Maven inside the image build; the final image holds only the Java 21 JRE and `app.jar`.
