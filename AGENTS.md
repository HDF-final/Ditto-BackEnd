# DITTO project instructions (BackEnd)

## Product context

- DITTO helps visitors from China, Japan, and the United States discover country-specific K-culture trends, create an AI-assisted course, customize it, use it with a mobile indoor map, and share it with the traveler community.
- The current milestone is the backend foundation: common response format, global exception handling, project structure, security skeleton, and coding conventions. Prioritize this shared base over individual business logic.
- Do not invent business behavior or API contracts that have not been provided. Confirm the domain model before implementing it.
- Keep README.md accurate when setup, commands, endpoints, environment variables, or architecture change.

## Required stack

- Use Java 17 (JDK 17) and Spring Boot 3.x. Do not downgrade the Boot major version or the Java toolchain.
- Use Gradle as the build tool. Commit `build.gradle` and `settings.gradle`; do not switch to Maven.
- Persist data with Spring Data JPA. Use MySQL or PostgreSQL as configured in `application-local.yml`.
- Secure endpoints with Spring Security using **server-side session authentication** (`HttpSession` + `JSESSIONID` cookie). Do not introduce JWT or token-based auth unless the user explicitly changes this decision.
- Document APIs with SpringDoc OpenAPI 3. Keep Swagger annotations accurate when endpoints change.
- Use Lombok for boilerplate. Do not hand-roll getters/builders that Lombok already provides.
- The package root is `com.ditto`.

## Architecture boundaries

- Follow the layered flow `controller → service → repository`. Do not skip layers or put business logic in controllers.
- **controller**: HTTP request/response only. Always return `ApiResponse<T>`. No transactions, no persistence logic.
- **service**: business logic and transaction boundary. Throw `BusinessException` with an `ErrorCode`.
- **repository**: Spring Data JPA interfaces only.
- **domain**: JPA entities. Never expose an entity directly as a controller request or response body.
- **global / config / security**: cross-cutting concerns that do not depend on a single domain.
- Keep new code inside the matching domain package (`auth`, `user`, `course`, `community`, `news`, `navigation`, `admin`).

## API and response contract

- Base URL is `/api/v1`. Group endpoints by domain and keep them RESTful.
- Every response uses the common format: success wraps data in `ApiResponse<T>`; failure returns `ErrorResponse`.
- Do not create ad-hoc response shapes. Extend `ApiResponse` / `ErrorResponse` instead.
- Separate Request and Response DTOs. Prefer `record` DTOs with Bean Validation on requests.
- Validate all external input with Bean Validation; do not trust request bodies or params.

## Exception handling

- Throw only `BusinessException` (or a subclass) with a defined `ErrorCode`. Do not throw raw `RuntimeException`.
- Add new failure cases to `ErrorCode` (HTTP status + business code + message) rather than hardcoding messages.
- Let `GlobalExceptionHandler` (`@RestControllerAdvice`) be the single place that converts exceptions to `ErrorResponse`. Do not catch-and-swallow in controllers.

## Data and security

- Keep secrets (DB password, etc.) in `application-local.yml` or environment variables. Never commit real secrets.
- Do not log session IDs, passwords, or personal data. Use `@Slf4j`, never `System.out.println`.
- Hash passwords with the configured `PasswordEncoder` (BCrypt). Never store plaintext credentials.
- Authenticate via `HttpSession`: after login, save the `SecurityContext` to the session; rely on the `JSESSIONID` cookie for subsequent requests. Do not add JWT/token logic.
- Keep the session cookie `HttpOnly` and `SameSite`; use `Secure` cookies over HTTPS in production. Enable CORS with credentials and an explicit origin allowlist (no `*`).
- Regenerate the session ID on login (session fixation protection). Invalidate the session and clear `JSESSIONID` on logout.
- Update the public-endpoint allowlist in `SecurityConfig` deliberately; default new endpoints to authenticated.
- Keep `spring.jpa.open-in-view: false`; load data inside the service transaction.

## Persistence conventions

- Use snake_case for table and column names; singular nouns for entities.
- Annotate service classes with `@Transactional(readOnly = true)` by default and `@Transactional` on write methods.
- Do not open `@Setter` on entities; change state through meaningful domain methods.
- `ddl-auto: update` is local-only. Never use it against a shared or production database (`validate` / `none`).

## Dependency policy

- Ask before adding a new production dependency unless the user's request explicitly requires it.
- Prefer Spring Boot starters and existing capabilities before adding a third-party library.
- Pin versions through the Spring dependency management BOM where possible; avoid unmanaged version bumps.

## Verification

After changing application code:

1. Build the project (`./gradlew build`, or IntelliJ Gradle sync + build if no wrapper is present yet).
2. Run the affected tests (`./gradlew test`).
3. When runtime behavior changes, start the app (`./gradlew bootRun`) and verify the endpoint via Swagger UI at `http://localhost:8080/swagger-ui.html`.

Do not report work complete while a required check is failing. If a check cannot run (e.g. no Gradle wrapper yet), state exactly why.

## Commit and Git Flow

- Follow the shared convention: commit messages as `<type>: <content>` (`feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`).
- Branch from `dev` as `<type>/#<issue>` (e.g. `feat/#23`); open PRs into `dev`; release by merging `dev` into `main`.
- Do not commit build output (`build/`, `.gradle/`), environment secrets, or local editor files (`.idea/`).

## Change discipline

- Preserve user work and avoid unrelated rewrites.
- Keep changes small enough to review and explain non-obvious architecture decisions.
- Expand this file gradually as repeatable project conventions emerge; keep detailed product and architecture prose in README.md or dedicated docs.
