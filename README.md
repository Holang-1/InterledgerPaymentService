# Interledger Payment Service

A standalone backend service for the Payment System. The service is
intended to provide a payment API that the wider application can call,
while keeping payment records and payment-provider integration separate
from the user-facing application.

> **Project status:** This README documents the implementation confirmed
> so far. The service currently has an Express application, health-check
> routes, environment-based configuration, and a PostgreSQL connection
> check. The database is currently empty, and the complete Open Payments
> / Interledger transaction flow is not yet documented as implemented.
> Do not treat the planned payment endpoints or external payment flow as
> working until they are implemented and tested.

## Contents

-   [Current functionality](#current-functionality)
-   [Technology stack](#technology-stack)
-   [Requirements](#requirements)
-   [Clone and run locally](#clone-and-run-locally)
-   [Environment configuration](#environment-configuration)
-   [Available endpoints](#available-endpoints)
-   [How it fits into the wider
    application](#how-it-fits-into-the-wider-application)
-   [Payment data and status](#payment-data-and-status)
-   [Project structure](#project-structure)
-   [Testing and troubleshooting](#testing-and-troubleshooting)
-   [Current limitations and next
    steps](#current-limitations-and-next-steps)

## Current functionality

Confirmed in the current project notes:

-   Node.js backend using Express.
-   JSON request parsing and CORS middleware.
-   Environment variables loaded with `dotenv`.
-   A `GET /health` endpoint for checking whether the HTTP service is
    running.
-   A `GET /health/database` endpoint for checking database
    connectivity.
-   PostgreSQL connection configuration in `src/config/database.js`.
-   Start scripts:
    -   `npm start`
    -   `npm run dev` (uses Nodemon for development)

The PostgreSQL database currently has no tables. Payment persistence and
the external Open Payments / Interledger transaction flow must therefore
be considered **in progress or planned**, unless the code has since
changed.

## Technology stack

  --------------------------------------------------------------------------
  Component               Technology                 Version information
  ----------------------- -------------------------- -----------------------
  Runtime                 Node.js                    Use the version
                                                     required by the
                                                     repository's
                                                     `package.json` /
                                                     `.nvmrc`, if present.
                                                     The exact project
                                                     runtime version was not
                                                     available in the notes
                                                     used to prepare this
                                                     README.

  Package manager         npm                        Use the npm version
                                                     bundled with your
                                                     installed Node.js,
                                                     unless the repository
                                                     specifies otherwise.

  HTTP framework          Express                    Exact installed version
                                                     is pinned in
                                                     `package-lock.json`.

  Environment             dotenv                     Exact installed version
  configuration                                      is pinned in
                                                     `package-lock.json`.

  Development runner      Nodemon                    Used by the `dev`
                                                     script; exact version
                                                     is pinned in
                                                     `package-lock.json`.

  Database                PostgreSQL                 The database engine
                                                     version has not been
                                                     confirmed in the
                                                     available project
                                                     notes.

  Database connectivity   PostgreSQL                 Confirm the exact
                          client/configuration in    driver and version in
                          `src/config/database.js`   `package.json` /
                                                     `package-lock.json`.

  API format              HTTP + JSON                REST-style endpoints.

  Payment                 Open Payments /            Intended integration; a
  interoperability target Interledger                completed protocol flow
                                                     is not yet confirmed.
  --------------------------------------------------------------------------

**Why the versions are not guessed:** `package-lock.json` records the
resolved JavaScript dependency versions. Check it into Git and use
`npm ci` so a fresh clone installs the locked dependency tree. Before
publishing this README as final, add the exact Node.js and PostgreSQL
versions tested by the team.

## Requirements

Install the following on your development machine:

1.  Git
2.  Node.js and npm
3.  PostgreSQL, either installed locally or run in a container
4.  A terminal and an API client such as `curl` or Postman

Check your installations:

``` bash
git --version
node --version
npm --version
psql --version
```

The exact PostgreSQL setup depends on the local database configuration
in `src/config/database.js` and the variables in `.env`. Use the same
values expected by the project rather than assuming a database name,
username, or password.

## Clone and run locally

### 1. Clone the repository

Replace the placeholder with the actual GitHub repository URL:

``` bash
git clone <GITHUB_REPOSITORY_URL>
cd InterledgerPaymentService
```

### 2. Install dependencies

From the repository root:

``` bash
npm ci
```

`npm ci` installs the versions locked in `package-lock.json`. If the
repository does not contain a `package-lock.json`, use `npm install` and
commit the generated lockfile so other developers can reproduce the
dependency installation.

### 3. Configure environment variables

Create a local environment file if the project has not already supplied
one:

``` bash
touch .env
```

Open `.env` and set the variables expected by the code in
`src/config/database.js` and `src/server.js`. At minimum, verify the
database connection settings and the application port. **Do not commit
`.env` or real credentials.**

Example shape only; use the exact variable names read by the project
code:

``` dotenv
PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_NAME=<your_database_name>
DB_USER=<your_database_user>
DB_PASSWORD=<your_database_password>
```

The database variable names above are illustrative, not a guarantee that
the current code uses these exact names. Confirm them in
`src/config/database.js` before running the service. If the code expects
a single connection URL instead, configure that variable instead.

### 4. Start PostgreSQL

Make sure PostgreSQL is running and that the database and user
configured in `.env` exist. For a local PostgreSQL installation, the
commands used to create a database depend on your operating system and
PostgreSQL setup.

For example, after connecting as a database administrator, create a
database and user that match your `.env` values. Do not use production
credentials for local development.

No database tables were present at the time this README was prepared. If
the application requires a schema or migrations, those must be added and
run before payment records can be stored.

### 5. Start the service

Development mode, with Nodemon:

``` bash
npm run dev
```

Normal start:

``` bash
npm start
```

The server uses `PORT` when configured and otherwise defaults to port
`3000`. Unless the port is changed, the local base URL is:

``` text
http://localhost:3000
```

Keep the terminal open while using the service. Stop it with `Ctrl+C`.

### 6. Verify that it is running

Check the HTTP service:

``` bash
curl -i http://localhost:3000/health
```

Check the database connection:

``` bash
curl -i http://localhost:3000/health/database
```

A successful health response confirms that the relevant check passed; it
does **not** prove that payment creation, settlement, or an external
Open Payments transaction works.

## Environment configuration

Configuration is loaded from `.env` by `dotenv`. Keep local secrets out
of Git.

  -----------------------------------------------------------------------
  Setting                             Purpose
  ----------------------------------- -----------------------------------
  `PORT`                              HTTP port. Defaults to `3000` if
                                      not set.

  Database settings                   Configure the PostgreSQL host,
                                      port, database, username, and
                                      password, or the connection URL,
                                      according to the actual names used
                                      in `src/config/database.js`.
  -----------------------------------------------------------------------

Recommended repository hygiene:

-   Commit `.env.example` with variable names and safe placeholder
    values.
-   Add `.env` to `.gitignore`.
-   Never commit passwords, private keys, access tokens, or other
    secrets.
-   Document required variables in `.env.example` and keep it aligned
    with the code.

## Available endpoints

The endpoints confirmed in the current implementation notes are:

  --------------------------------------------------------------------------
  Method            Path                 Purpose           Status
  ----------------- -------------------- ----------------- -----------------
  `GET`             `/health`            Checks that the   Implemented
                                         HTTP application  
                                         is responding.    

  `GET`             `/health/database`   Checks            Implemented
                                         connectivity to   
                                         PostgreSQL.       
  --------------------------------------------------------------------------

The following payment endpoints have been discussed as the intended API,
but are **not confirmed as implemented** in the available project notes:

  -----------------------------------------------------------------------
  Method                  Proposed path           Intended purpose
  ----------------------- ----------------------- -----------------------
  `POST`                  `/api/payments`         Accept a payment
                                                  request and create a
                                                  payment record.

  `GET`                   `/api/payments/:id`     Retrieve a payment by
                                                  its identifier.

  `GET`                   `/api/payments`         List payments, subject
                                                  to authentication and
                                                  authorization rules.
  -----------------------------------------------------------------------

Do not rely on these proposed routes until they exist in the current
source code and have tests.

## How it fits into the wider application

The service is designed to be a **separate backend component**. The
user-facing application should call its API over HTTP rather than
implementing payment logic directly inside the frontend.

### Intended communication flow

``` text
Customer / Shop Owner
        |
        v
Frontend or main application
        |
        | HTTP request with JSON
        v
Interledger Payment Service
        |
        +----> Validate request and apply payment rules
        |
        +----> Persist payment record in PostgreSQL
        |
        +----> Open Payments / Interledger integration
        |      (planned; not confirmed as complete)
        |
        v
Return payment identifier and current status
        |
        v
Frontend / main application displays the result
```

This diagram describes the intended architecture, not a claim that every
step is already implemented.

### Example of how the main application should call the service

Once the payment endpoint is implemented, the main application can make
an HTTP request to the payment service. For example, the intended
request shape might look like this:

``` http
POST /api/payments
Content-Type: application/json
```

``` json
{
  "amount": 100.00,
  "currency": "ZAR"
}
```

This is an illustrative contract only. The final request must match the
DTO and validation rules actually implemented by the service. Do not
send this example to the current service expecting it to create a
payment unless `POST /api/payments` has been implemented.

The frontend should not receive database credentials or private Open
Payments keys. Those belong on the backend in environment configuration
or a suitable secret store.

### Integration responsibilities

-   **Frontend / main application:** collects the payment request, calls
    the service, and displays the returned payment status.
-   **Payment Service:** validates payment requests, owns
    payment-specific business logic, and records payment state.
-   **PostgreSQL:** stores payment records and related metadata once the
    schema and persistence flow are implemented.
-   **Open Payments / Interledger:** intended interoperability layer for
    communicating with compatible payment accounts or providers. The
    exact API calls, authentication, grants, incoming/outgoing payment
    flow, and callback or polling strategy still need to be documented
    from the implementation.
-   **Other services:** should integrate through a documented HTTP API
    or an explicitly configured messaging mechanism. No messaging
    integration is confirmed in the current Node.js service notes.

For local development, the main application should use a configurable
base URL such as `http://localhost:3000`, rather than hard-coding the
URL throughout its source. In a deployed environment, configure the
service's deployed internal URL.

## Payment data and status

The broader Payment System design has discussed a payment record with
fields such as:

-   `id` --- the service's identifier for the payment.
-   `amount` --- the payment amount.
-   `currency` --- the currency code.
-   `status` --- the current state of the payment.
-   `provider_reference` --- an external transaction or payment-resource
    identifier, if one is returned by the provider.

These fields describe the intended domain model from project
discussions. They are **not a claim that a database table or API
response containing all these fields currently exists**.

A useful lifecycle for a payment integration is:

1.  Validate the incoming request.
2.  Create a local payment record with a pending state.
3.  Initiate or request the external payment through the chosen Open
    Payments flow.
4.  Store any external resource or transaction reference.
5.  Update the local status based on a verified result.
6.  Return or expose the current state to the main application.

A `provider_reference` and a payment `status` represent different
things. A reference identifies an external resource; it does not, by
itself, prove that a payment succeeded. The implementation should not
mark a payment successful solely because a reference exists.

## Project structure

The project notes identify the following structure:

``` text
InterledgerPaymentService/
├── src/
│   ├── config/
│   │   └── database.js
│   ├── controllers/
│   ├── middleware/
│   ├── routes/
│   ├── services/
│   ├── test/
│   ├── app.js
│   └── server.js
├── main/
│   └── resources/
├── test-db.js
├── package.json
├── package-lock.json
└── .env
```

Some folders may currently be empty or still under development. Check
the repository itself for the exact current tree. `src/app.js`
configures the Express application and middleware; `src/server.js` loads
environment configuration and starts listening on the configured port;
`src/config/database.js` contains the database connection setup.

## Testing and troubleshooting

### Run tests

Inspect `package.json` for the available test script. If a `test` script
is defined, run:

``` bash
npm test
```

If no test script exists yet, add tests and a corresponding script
before documenting this command as supported. `test-db.js` appears to be
a database test/helper file; run it only after checking its contents and
ensuring the configured database is safe to use.

### Common problems

**`npm ci` fails**

-   Confirm that Node.js and npm are installed.
-   Confirm that `package.json` and `package-lock.json` are in the
    repository root and in sync.
-   Use the Node.js version specified by `.nvmrc` or `package.json`, if
    the project provides one.

**The service does not start**

-   Confirm `.env` exists and contains the variables expected by the
    code.
-   Check whether the configured port is already in use.
-   Read the terminal error before changing configuration.

**`/health` works but `/health/database` fails**

-   Confirm PostgreSQL is running.
-   Confirm the database exists.
-   Check the configured host, port, database name, username, and
    password.
-   Confirm that the database user has the required permissions.

**The service responds, but payment routes return `404`**

-   The payment endpoints listed as proposed above may not yet be
    implemented.
-   Check `src/routes/` and how routes are mounted in `src/app.js`.

**The database is reachable but payment records cannot be saved**

-   The database was empty when this README was prepared.
-   Confirm that the schema/migrations have been created and applied.
-   Confirm that the payment entity/model and repository/database
    operations are implemented.

## Current limitations and next steps

Based on the available project notes, the next work needed to make this
a fully usable payment microservice includes:

1.  Confirm and document the exact Node.js, npm, Express, `dotenv`,
    PostgreSQL, and PostgreSQL-driver versions.
2.  Add `.env.example` and ensure `.env` is ignored by Git.
3.  Create the database schema and a reproducible migration/setup
    process.
4.  Implement and test payment creation and retrieval endpoints.
5.  Define request/response DTOs, validation, consistent error
    responses, and authentication/authorization.
6.  Implement the actual Open Payments / Interledger integration and
    document its authentication and payment lifecycle.
7.  Decide how payment status changes are verified, persisted, and
    communicated back to the main application.
8.  Add automated tests and document the exact test command.
9.  Add an OpenAPI/Swagger specification or equivalent API contract.
10. Document deployment and any messaging integration only once those
    components are actually present.

## Open Payments / Interledger references

-   [Open Payments: Getting
    Started](https://openpayments.dev/overview/getting-started/)
-   [Interledger: Get
    Started](https://interledger.org/developers/get-started/)
-   [Interledger developer
    portal](https://interledger.org/tech/dev-portal/)

These references explain the broader ecosystem. They do not imply that
the service already implements every Open Payments or Interledger
capability.
