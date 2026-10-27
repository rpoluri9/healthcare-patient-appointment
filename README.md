# Healthcare Patient and Appointment Management System

A Spring Boot backend for managing patients, providers, and appointments —
built as a portfolio project by Ramesh Poluri. All patients, providers, and
data in this project are **fictional demo content** created for the project.
It makes no claim about any employer, clinic, or real patient.

## What it does

- **Patient registration and profile updates** — create and update patients
  with validation (names, past date of birth, phone format, email format).
- **Provider management** — providers with a specialization, working days,
  working hours, and a consultation duration.
- **Appointment booking, cancellation, and search** — book, cancel, complete,
  and search appointments by patient, provider, status, or date range.
- **Scheduling rules that prevent conflicts** — a booking is rejected when:
  - the slot is in the past,
  - the day is not one of the provider's working days,
  - the slot falls outside the provider's working hours,
  - the provider already has a booking overlapping the slot (HTTP 409),
  - the patient already has a booking overlapping the slot (HTTP 409).
  The appointment end time is derived from the provider's consultation
  duration, so a client can never book a slot of the wrong length.
- **Authentication with role-based authorization** — local HTTP Basic auth
  against accounts in the database (BCrypt password hashes). Roles:
  - **ADMIN** — everything.
  - **STAFF** — manage patients, providers, and appointments.
  - **PATIENT** — only their own record: view it, book for themselves,
    cancel their own appointments, and search their own appointments.
  A PATIENT account is linked to a patient record, so ownership is proven
  server-side instead of trusting a client-sent id.
- **Secure data handling** — the patient list endpoint returns summaries only
  (id + name, no date of birth, phone, email, or address). Full details are
  returned one patient at a time and only to STAFF/ADMIN or the patient
  themselves. Service logs carry entity IDs only — never names, dates of
  birth, phone numbers, email addresses, or home addresses. Password hashes
  are never returned by any endpoint.
- **Centralized exception handling and structured logging** — every error
  leaves the API in the same `{timestamp, status, error, message, path}`
  shape (404 unknown record, 400 broken scheduling rule, 409 conflicting
  booking, 401 unauthenticated, 403 forbidden).

## Run it

```bash
cd healthcare-appointment
mvn spring-boot:run
# or, after building:
mvn package -DskipTests
java -jar target/healthcare-appointment-1.0.0.jar
```

In VS Code: open the project folder and run the
`HealthcareAppointmentApplication` class with the Java extension's Run button.

The service starts on **http://localhost:8080** with an in-memory H2
database — no database setup needed, and data resets on restart.

## Demo accounts (local demo only — change before any real use)

| Username | Password   | Role    | Linked patient |
|----------|------------|---------|----------------|
| admin    | admin123   | ADMIN   | —              |
| staff    | staff123   | STAFF   | —              |
| patient1 | patient123 | PATIENT | John Doe       |

Two demo providers are seeded: Alice Garcia (Cardiology, Mon–Fri 09:00–17:00,
30-minute visits) and Bob Lee (Dermatology, Mon/Wed/Fri 10:00–16:00,
45-minute visits).

## API list

Base URL: `http://localhost:8080`. All endpoints require HTTP Basic auth.

| Method | Path | Who | What it does |
|--------|------|-----|--------------|
| GET | `/api/auth/me` | any | Who the caller is (username, role, linked patient) |
| POST | `/api/patients` | STAFF, ADMIN | Register a patient |
| PUT | `/api/patients/{id}` | STAFF, ADMIN | Update a patient profile |
| GET | `/api/patients` | STAFF, ADMIN | List patient summaries (no sensitive fields) |
| GET | `/api/patients/{id}` | STAFF, ADMIN, or the patient | Full patient record |
| POST | `/api/providers` | STAFF, ADMIN | Add a provider |
| PUT | `/api/providers/{id}` | STAFF, ADMIN | Update a provider |
| GET | `/api/providers` | any | List providers (optional `?specialization=`) |
| GET | `/api/providers/{id}` | any | One provider |
| POST | `/api/appointments` | any | Book (patients: only for themselves) |
| GET | `/api/appointments/{id}` | STAFF, ADMIN, or the patient | One appointment |
| GET | `/api/appointments` | any | Search (`?patientId=&providerId=&status=&from=&to=`; patients see only their own) |
| POST | `/api/appointments/{id}/cancel` | STAFF, ADMIN, or the patient | Cancel a BOOKED appointment |
| POST | `/api/appointments/{id}/complete` | STAFF, ADMIN | Mark a BOOKED appointment completed |

A Postman collection covering all endpoints is at
`postman/healthcare-appointment.postman_collection.json`.

## Try it

```bash
# Who am I? (staff)
curl -u staff:staff123 localhost:8080/api/auth/me

# Register a patient (copy the id from the response)
curl -u staff:staff123 -X POST localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Mia","lastName":"Chen","dateOfBirth":"1993-07-21",
       "phone":"+1-555-0200","email":"mia@example.com","address":"1 Demo Way"}'

# Book an appointment (Monday 10:00 — inside provider working hours)
curl -u staff:staff123 -X POST localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientId":3,"providerId":1,"startTime":"2030-01-07T10:00:00",
       "reason":"Annual checkup"}'

# Book the same slot again -> 409 conflict
# Cancel it
curl -u staff:staff123 -X POST localhost:8080/api/appointments/1/cancel
```

## What runs locally vs what would need real configuration

Everything here runs **locally by default, with no cloud account and no
external services**:

| Feature | Default (what actually runs) | Production option |
|---------|------------------------------|-------------------|
| Database | In-memory H2 (data resets on restart) | PostgreSQL — the driver is on the classpath; uncomment the PostgreSQL block in `application.properties` and set real credentials |
| Authentication | Local database accounts + HTTP Basic + BCrypt | An OIDC/OAuth provider, HTTPS, and CSRF protection for browser clients — none of these are configured here, and no provider credentials are invented |
| Data | Fictional demo patients/providers | Real data would require proper consent, encryption, audit, and compliance review — none of which this demo claims |

Do not describe this project as connected to a real clinic, a real
identity provider, or a production cloud deployment unless you have actually
configured it that way.

## Tests

```bash
mvn test
```

Covers the scheduling rules (overlap, working days, working hours, past
slots, unknown patient/provider, cancel/complete transitions), patient and
provider services, and the full authenticated API flow including role and
ownership checks (401 unauthenticated, 403 wrong role, 403 other patient,
409 conflicting booking).

## Project layout

```
src/main/java/com/healthcare/appointment/
  model/       Patient, Provider, Appointment, UserAccount, Role, AppointmentStatus
  repository/  Spring Data JPA repositories (incl. overlap queries)
  service/     PatientService, ProviderService, AppointmentService, CurrentUserService
  controller/  Patient/Provider/Appointment/Auth REST controllers
  exception/   Centralized @ControllerAdvice error handling
  config/      SecurityConfig (local Basic auth + roles), DataSeeder (fictional demo data)
docs/          Architecture, appointment lifecycle, auth/role flow diagrams
postman/       Postman collection for all endpoints
```

## Diagrams

- `docs/architecture.png` — clients, controllers, services, repositories, H2
- `docs/appointment-lifecycle.png` — BOOKED → CANCELLED / COMPLETED, and every rejection
- `docs/auth-role-flow.png` — how a request passes authentication, role checks, and ownership checks
