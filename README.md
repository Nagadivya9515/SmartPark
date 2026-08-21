# Parking System

A parking-lot management platform: registration/login, a live occupancy dashboard,
slot booking, an operator gate flow (entry/exit ticketing), and an admin console.

This project is the merge of three previously separate, overlapping projects in this
workspace (`ParkingLot 2`, `auth-system`, `parkinglot 3`) into one. `auth-system` was
the most complete of the three and is the base this was built from; the other two are
archived, not deleted, alongside this folder's parent.

## Stack

- **Backend** — Spring Boot 3 (Java 17), Spring Security + JWT, Spring Data JPA, MySQL.
- **Frontend** — Angular 17 (standalone components, signals), served separately in dev.

## Structure

```
backend/   Spring Boot API — auth, inventory, booking, operator, admin modules
frontend/  Angular app — dashboard, booking flow, operator screens, admin console
```

### Backend modules (`backend/src/main/java/com/parking`)

| Package | Responsibility |
|---|---|
| `auth` | Registration/login, JWT + refresh-token cookies, session cleanup |
| `inventory` | The single shared lot/slot model — dashboard, booking, and the operator gate flow all read and write these tables |
| `booking` | End-user slot reservations against a registered vehicle |
| `operator` | Gate-staff login, entry ticketing, exit/checkout, live occupancy |
| `admin` | Admin login, revenue/occupancy overview, operator management, reports |

### What changed from `auth-system`

This wasn't a plain copy — the audited issues were fixed as part of the merge:

- **One inventory, not two.** The dashboard used to show a different, separately
  seeded lot ("GreenView Mall Parking") than the one operators worked against
  ("City Center Parking"). There is now one `ParkingLot`/`ParkingSlot` model
  (`com.parking.inventory`) that booking, the dashboard, and the operator flow all
  share — booking a slot and it showing occupied at the gate is now the same flag.
- **Session cleanup actually runs.** The nightly cleanup job's repository field was
  initialized to `null` in its declaration, which made Lombok skip generating a
  constructor parameter for it — the job NPE'd (silently, caught and logged) on
  every run. Fixed by removing the initializer.
- **Slot entry allocation is now concurrency-safe.** Generating an entry ticket used
  an unlocked `SELECT` to find an open slot; two simultaneous entries for the same
  vehicle type could double-assign one. It now runs under a `PESSIMISTIC_WRITE` lock.
- **Bookings belong to the logged-in user.** Booking used to write everything against
  a hardcoded demo user id regardless of who was authenticated. It now resolves the
  real user from the JWT.
- **Booking trusts the database, not the client.** The old request accepted slot
  number/section/floor/lot/rate as freeform fields from the browser. The new request
  only takes a slot id, a vehicle plate, and a duration — everything else (including
  the price) is resolved server-side from the real slot and the caller's own
  registered vehicle.
- **The slot-toggle endpoint is admin-only.** Any authenticated end user could
  previously free or occupy any slot in the lot via the dashboard's toggle action —
  there was no role check. It's now `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")`
  and the end-user dashboard no longer exposes it.
- **Regular users got a role authority that never matched anything.**
  `User.Role`'s enum constants are already `ROLE_USER`/`ROLE_ADMIN`, but
  `CustomUserDetailsService` prepended `"ROLE_"` again, granting authorities like
  `ROLE_ROLE_USER` — found by actually logging in and checking `GET /api/auth/me`.
- **The primary seeded admin account couldn't use the admin console at all.**
  `admin@smartpark.com` is seeded with role `SUPER_ADMIN`, but every
  `/api/admin/**` route (and the slot-toggle override above) only accepted
  `hasRole("ADMIN")` — confirmed live: that account logged in fine and then got
  403 on overview/parking-lots/operators/reports. Both checks are now
  `hasAnyRole("ADMIN", "SUPER_ADMIN")`.
- **The admin console can actually reach the backend.** Its API client pointed at
  port 8081; the backend serves everything, admin included, on 8080.
- **The booking page's deep link works.** `/booking/:id` opened directly (no
  dashboard click) used to call a bookings endpoint that never existed on the
  backend and always 404'd. There's now a real `GET /api/parking/slots/{id}`.
  It also now shows the caller's own registered vehicles instead of a freeform
  plate field the backend's ownership check would always reject.
- **A duplicate-constructor compile error in `AuthController`.** It carried both
  Lombok's `@RequiredArgsConstructor` and a hand-written constructor with the
  identical signature.
- **Two competing global exception handlers** merged into one, so a runtime
  exception's HTTP status no longer depends on undefined resolution order between
  two `@RestControllerAdvice` classes.
- **Dead code removed:** an unused `EnumModels.UserRole`, a comment-only
  `RepositoryPatchNotes` placeholder, a duplicate unused `BookingService.getBooking`
  overload, an unrouted `AdminShellComponent` and `BookingFormComponent`, an unused
  `resilience4j-retry` dependency, and the large blocks of superseded code several
  files carried commented out above their active implementation.
- **An unguarded `/entry` route removed** from the frontend router — generating a
  real parking ticket was reachable with no operator login at all.
- **Secrets are environment-overridable** (`DB_PASSWORD`, `JWT_SECRET`, etc.) with
  dev-only fallbacks, rather than only ever living in a committed properties file.

### Known limitations carried over (not fixed in this pass)

- No automated tests exist yet for either module.
- `ddl-auto=update` manages the schema; there's no migration tool (Flyway/Liquibase).
- Revenue-by-payment-method and revenue-by-vehicle-type in admin reports are still
  simulated percentage splits, not measured from real ticket data.

## Verified runnable

Both halves have actually been built and run, not just compiled:

- **Backend**: booted clean against a real MySQL instance with zero exceptions.
  Full request cycle exercised over `curl` — register, login, `/me`, dashboard,
  book a slot, confirm it shows occupied, cancel it, confirm it frees, operator
  entry picks up that same freed slot, exit completes and frees it again, admin
  login + overview + parking-lots + operators + reports all `200` for *both*
  seeded admin accounts, and the error paths (validation, duplicate username,
  bad password, double-booking) all return the right status codes instead of a
  bare 500.
- **Frontend**: a real `npm install` (804 packages) and a real `ng build`, both
  `development` and `production` configuration, zero errors under
  `strictTemplates`. `ng serve` confirmed actually serving the app and its
  client-side routes.

## Running locally

**Backend**
```
cd backend
./mvnw spring-boot:run
```
Needs a local MySQL reachable at `localhost:3306` (or override `spring.datasource.url`
and the `DB_USERNAME`/`DB_PASSWORD` env vars). Creates `parking_system_db` on first
run and seeds one lot, demo operators, admin accounts, and two vehicles for a demo user.

**Frontend**
```
cd frontend
npm install
npm start
```
Serves on `http://localhost:4200`, talking to the backend on `http://localhost:8080`.

> If `npm install` only installs ~11 packages instead of ~800, something in your
> environment has `NODE_ENV=production` set (or an npm config with `omit=dev`) —
> that silently skips `devDependencies`, which is where the Angular CLI and
> TypeScript live. Run `npm install --include=dev` instead.

Demo accounts seeded on first backend run: operators `OP001`/`12345678` (entry),
`OP002`/`operator123` (exit), `OP003`/`supervisor1` (supervisor); admins
`admin@smartpark.com`/`Admin@1234` (super admin) and `manager@smartpark.com`/
`Manager@1234` (admin) — both now work identically against every admin endpoint.
