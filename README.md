# Healthcare Management System (HMS)

A Spring Boot web application for managing patients, doctors, appointments and medical records,
with **role-based access control** (Administrator, Receptionist, Doctor).

**Stack:** Java 21 · Spring Boot 4 · Spring Security · Spring Data JPA · MySQL · plain HTML/CSS/JavaScript

---

## Quick start

### 1. Run with Docker (easiest)

```bash
cp .env.example .env        # then edit the passwords inside
docker compose up --build
```

Open <http://localhost:8080>.

### 2. Run locally

1. Install Java 21 and MySQL 8.
2. Start MySQL (the app creates the `healthcare_db` database for you).
3. Set your database login (defaults are `root` with an empty password):

   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=your-password
   ```

4. Start the app:

   ```bash
   ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
   ```

> **Upgrading from the first version?** The database layout changed (new columns, relations and user
> accounts). Drop the old `healthcare_db` database (or point `DB_URL` at a new one) before the first start.

### First login

On an empty database the app creates these accounts:

| Username    | Password        | Role          |
|-------------|-----------------|---------------|
| `admin`     | `Admin@123`     | Administrator |
| `reception` | `Reception@123` | Receptionist  |
| `doctor`    | `Doctor@123`    | Doctor (linked to the demo doctor "Dr. Anita Rao") |

These are **demo passwords**. For anything real, set `HMS_ADMIN_PASSWORD` and `HMS_SEED_DEMO=false`
before the first start, then create real users on the **Users** page.

---

## Who can do what

The rules are enforced **on the server** (`SecurityConfig` + the services). Hiding a button in the
browser is only a convenience.

| Area | Administrator | Receptionist | Doctor |
|------|:---:|:---:|:---:|
| View patients, doctors | ✅ | ✅ | ✅ |
| Add / edit patients | ✅ | ✅ | ❌ |
| Delete patients | ✅ | ❌ | ❌ |
| Add / edit / delete doctors | ✅ | ❌ | ❌ |
| View appointments | all | all | **own only** |
| Book / edit appointments | ✅ | ✅ | ❌ |
| Cancel an appointment | ✅ | ✅ | own only |
| Mark an appointment completed | ✅ | ❌ | own only |
| View medical records | ✅ | ❌ | ✅ |
| Create a medical record | ❌ | ❌ | ✅ (author = logged-in doctor) |
| Edit a medical record | ❌ | ❌ | only records they wrote |
| Delete a medical record | ✅ | ❌ | ❌ |
| Manage user accounts | ✅ | ❌ | ❌ |

Business rules that go with it:

- The author of a medical record is taken from the login, never from the request.
- A doctor's login must be linked to a doctor profile (done when the admin creates the user).
- Appointments cannot be in the past, outside the doctor's working hours, or overlap another
  appointment for the same doctor **or** the same patient (30-minute visits, configurable).
- Finished (completed/cancelled) appointments cannot change again.
- Patients/doctors with scheduled appointments cannot be deleted. Deleting is a *soft delete*:
  records are hidden, never destroyed.
- You cannot disable your own account or the last active administrator.

## Security measures

- Passwords hashed with BCrypt; session cookie is `HttpOnly` + `SameSite=Lax` (`Secure` when `COOKIE_SECURE=true`).
- CSRF protection on every write request.
- Content-Security-Policy that only allows the app's own scripts and styles.
- All data is HTML-escaped before it is shown (prevents stored XSS).
- Request validation on every input; errors never expose stack traces.
- Audit columns (`created_by`, `updated_by`, timestamps) on every table.

## Project layout

```
src/main/java/com/healthcare/hms
  config/       SecurityConfig (role matrix), CSRF filter, auditing, first-run seed data
  security/     logged-in user model and helper
  entity/       JPA entities and enums
  repository/   Spring Data repositories
  dto/          request/response objects with validation rules
  service/      business rules and ownership checks
  controller/   REST endpoints (/api/...)
  exception/    one global JSON error handler
src/main/resources/static   the web pages (HTML, css/, js/)
src/test                    unit + security tests (run on in-memory H2, no MySQL needed)
```

## API overview

All endpoints are under `/api` and need a login (session cookie) except the login itself.

| Method & path | Notes |
|---|---|
| `POST /api/auth/login` | form fields `username`, `password` |
| `POST /api/auth/logout`, `GET /api/auth/me` | |
| `GET/POST/PUT/DELETE /api/patients` | `GET` supports `?search=&page=&size=`; `/options?search=` for pickers |
| `GET/POST/PUT/DELETE /api/doctors` | `/options` for dropdowns |
| `GET/POST/PUT /api/appointments`, `PATCH /api/appointments/{id}/status` | filters: `doctorId, patientId, date, status` |
| `GET/POST/PUT/DELETE /api/medical-records` | `?patientId=&search=` |
| `GET/POST /api/users`, `PATCH /api/users/{id}/enabled`, `PUT /api/users/{id}/password` | admin only |
| `GET /api/dashboard/stats` | role-aware counts |
| `GET /actuator/health` | for monitoring |

Write requests must send the CSRF token from the `XSRF-TOKEN` cookie in an `X-XSRF-TOKEN` header
(the bundled JavaScript does this automatically).

## Tests

```bash
./mvnw test
```

Covers booking rules, role/ownership rules, validation, and the URL-level role matrix.
If you only want to build the jar: `./mvnw package -DskipTests`.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local MySQL, `root`, empty | database |
| `DDL_AUTO` | `update` | Hibernate schema mode (use `validate` once you adopt migrations) |
| `HMS_ADMIN_PASSWORD` | `Admin@123` | first admin password |
| `HMS_SEED_DEMO` | `true` | create demo receptionist/doctor/patient on an empty database |
| `COOKIE_SECURE` | `false` | set `true` behind HTTPS |
| `PORT` | `8080` | HTTP port |

## Ideas for next steps

- Flyway migrations instead of `ddl-auto=update`
- Login rate-limiting / account lock-out, and a "change my own password" page
- HTTPS termination (reverse proxy) with `COOKIE_SECURE=true`
- OpenAPI/Swagger documentation
- Billing, doctor day-off calendar, e-mail/SMS appointment reminders, PDF prescriptions


## Outpatient workflow UI refresh

This update refreshes the shared visual theme and dashboard and adds these first-priority screens:
- **Visit queue:** check in a patient, assign a daily token, and progress a visit through Waiting → In consultation → Completed.
- **Consultation:** record demo vitals, allergies, complaints, findings, assessment and follow-up date.
- **Prescriptions:** add structured medicine instructions and print the preview.
- **Billing:** create a simple invoice, record partial payments and view balances.

### Important prototype limitation
The new workflow screens currently store their data in the browser's `localStorage`; they are not persisted to MySQL and are not linked transactionally to the existing Spring Boot patient/appointment/medical-record entities. Use fictional data only. This is a demonstrable UI/workflow prototype, not a production clinical or billing system. Before clinical use, move these records to validated Spring Boot entities/services/controllers, enforce role access on every API, add audit logging, and define privacy, backup, retention and payment requirements.

### Rebuild and run with Docker
1. Extract the ZIP to a folder.
2. In the project folder, create `.env` from `.env.example` and set strong local values for `MYSQL_ROOT_PASSWORD`, `MYSQL_PASSWORD`, and `HMS_ADMIN_PASSWORD`.
3. Rebuild and start:
   ```bash
   docker compose up --build -d
   ```
4. Open `http://localhost:8080`.
5. To follow logs: `docker compose logs -f app`.
6. To stop: `docker compose down`. To preserve database data, do not remove the named volume.

The new prototype workflow data is browser-local, so it does not require a database migration. Existing server-backed modules continue to use the configured database.
