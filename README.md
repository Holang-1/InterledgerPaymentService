# Interledger Payment Service

A standalone backend service for the Payment System. It provides a payment API that the wider application can call while keeping payment records and Open Payments integration separate from the user-facing application.

> **Project status:** The service has a working Express API, PostgreSQL persistence for Open Payments incoming-payment requests, and a successful connection to an Interledger Open Payments test wallet. Creating a request and retrieving saved requests have been tested locally. Receiving funds, settlement verification, and automatic payment-status synchronization are not yet confirmed as implemented.

## Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Environment Configuration](#environment-configuration)
- [Database Setup](#database-setup)
- [API Endpoints](#api-endpoints)
- [Demo Shortcuts](#demo-shortcuts)
- [Project Structure](#project-structure)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Next Steps](#next-steps)

## Features

- Express REST API with JSON parsing and CORS middleware.
- HTTP health check and PostgreSQL connectivity check.
- PostgreSQL connection pool.
- Open Payments SDK integration using `@interledger/open-payments`.
- Authenticated lookup of the configured Open Payments wallet.
- Creation of Open Payments incoming-payment requests.
- Persistence of incoming-payment request details in PostgreSQL.
- Retrieval of all saved payment requests.
- Retrieval of a single payment request by its database UUID.

The incoming-payment creation endpoint has been tested against an Interledger test wallet. Creating a request does not mean funds have been received.

## Technology Stack

| Component | Technology |
|---|---|
| Runtime | Node.js |
| Package manager | npm |
| HTTP framework | Express |
| Environment configuration | dotenv |
| Development runner | Nodemon |
| Database | PostgreSQL |
| PostgreSQL client | pg |
| Payment integration | Open Payments SDK |
| API format | HTTP + JSON |

## Quick Start

### 1. Clone the repository

```bash
git clone <GITHUB_REPOSITORY_URL>
cd InterledgerPaymentService
```

Replace `<GITHUB_REPOSITORY_URL>` with the actual repository URL.

### 2. Install dependencies

```bash
npm ci
```

If `package-lock.json` is unavailable, run `npm install`.

### 3. Configure environment variables

Create `.env` if it does not exist:

```bash
touch .env
```

Configure the database and Open Payments credentials:

```dotenv
PORT=3000

DB_HOST=localhost
DB_PORT=5432
DB_NAME=<your_database_name>
DB_USER=<your_database_user>
DB_PASSWORD=<your_database_password>

OPEN_PAYMENTS_CLIENT_WALLET_ADDRESS=<your_test_wallet_address>
OPEN_PAYMENTS_KEY_ID=<your_open_payments_key_id>
OPEN_PAYMENTS_PRIVATE_KEY_PATH=./secrets/private.key
```

Use the values belonging to your own environment. The private key must exist at the configured path and correspond to a developer key authorized for the wallet.

### 4. Apply the database migration

Ensure PostgreSQL is running and the configured database exists.

The incoming-payment table is defined in:

`src/main/resources/db/migrations/003_create_open_payment_requests.sql`

Apply the migration:

```bash
set -a
source .env
set +a

PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -f src/main/resources/db/migrations/003_create_open_payment_requests.sql
```

The migration creates the `open_payment_requests` table and indexes for payment status and creation time.

### 5. Run the server

**Development mode with Nodemon:**

```bash
npm run dev
```

**Normal start:**

```bash
npm start
```

The server defaults to port `3000`, unless `PORT` is configured.

Base URL:

```text
http://localhost:3000
```

Keep the server terminal open while testing. Stop the server with `Ctrl+C`.

## Environment Configuration

| Variable | Purpose |
|---|---|
| `PORT` | HTTP port; defaults to `3000`. |
| `DB_HOST` | PostgreSQL host. |
| `DB_PORT` | PostgreSQL port; defaults to `5432`. |
| `DB_NAME` | PostgreSQL database name. |
| `DB_USER` | PostgreSQL username. |
| `DB_PASSWORD` | PostgreSQL password. |
| `OPEN_PAYMENTS_CLIENT_WALLET_ADDRESS` | Configured Open Payments wallet address. |
| `OPEN_PAYMENTS_KEY_ID` | Developer key ID. |
| `OPEN_PAYMENTS_PRIVATE_KEY_PATH` | Local private key path. |

Never commit `.env`, private keys, access tokens, or other secrets. Add `.env.example` with placeholder values for other developers.

## Database Setup

The `open_payment_requests` table stores incoming-payment request information.

| Column | Purpose |
|---|---|
| `id` | Local UUID identifying the database row. |
| `open_payment_id` | External Open Payments incoming-payment resource URL. |
| `wallet_address` | Wallet associated with the request. |
| `amount` | Requested amount in major currency units. |
| `currency` | Currency code, currently defaulting to `ZAR`. |
| `reference` | Shop, order, or payment reference. |
| `status` | Local payment status. |
| `received_amount` | Amount reported as received. |
| `created_at` | Record creation timestamp. |
| `expires_at` | Optional expiry timestamp. |

The local `id` and external `open_payment_id` are different identifiers. Use the local database UUID when retrieving a request through this service's single-record endpoint.

## API Endpoints

The payment routes are mounted under `/api/payments`.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/health` | Check HTTP service health. |
| GET | `/health/database` | Check PostgreSQL connectivity. |
| POST | `/api/payments/open-payments` | Create and save an incoming-payment request. |
| GET | `/api/payments/open-payments` | List saved incoming-payment requests. |
| GET | `/api/payments/open-payments/:id` | Retrieve one request by local database UUID. |

## Demo Shortcuts

These commands make it easier to demonstrate the service during development or a hackathon presentation.

Run them from a second terminal while the server is running.

### Set the base URL

```bash
BASE_URL="http://localhost:3000"
```

### 1. Test the server

```bash
curl -i "$BASE_URL/health"
```

This checks whether the HTTP service is responding.

### 2. Test the database connection

```bash
curl -i "$BASE_URL/health/database"
```

This checks PostgreSQL connectivity using the configured database connection.

### 3. Create a payment request

```bash
curl -sS -X POST "$BASE_URL/api/payments/open-payments" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 10.00,
    "currency": "ZAR",
    "reference": "SPAZA-DEMO-001"
  }'
```

The response contains the created incoming-payment resource details.

Use a unique reference for each demonstration, such as `SPAZA-DEMO-002`, so the new record is easy to identify.

**Important:** Creating a payment request does not mean the customer has paid. This endpoint creates an incoming-payment resource; payment receipt and automatic status synchronization are separate tasks.

Do not publish responses containing shared secrets or access tokens.

### 4. List all saved payment requests

```bash
curl -sS "$BASE_URL/api/payments/open-payments"
```

The response contains a `payments` array.

For formatted JSON, if `jq` is installed:

```bash
curl -sS "$BASE_URL/api/payments/open-payments" | jq
```

Find the request you created and copy its local `id` UUID.

### 5. Retrieve one payment request

Replace `<DATABASE_UUID>` with the `id` from the previous response:

```bash
curl -sS "$BASE_URL/api/payments/open-payments/<DATABASE_UUID>"
```

For formatted JSON:

```bash
curl -sS "$BASE_URL/api/payments/open-payments/<DATABASE_UUID>" | jq
```

Use the database `id`, not the external `open_payment_id`.

### 6. Inspect saved requests in PostgreSQL

Connect to PostgreSQL using the project's environment variables:

```bash
set -a
source .env
set +a

PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" \
  -p "${DB_PORT:-5432}" \
  -U "$DB_USER" \
  -d "$DB_NAME"
```

Run this query inside `psql`:

```sql
SELECT
    id,
    reference,
    amount,
    currency,
    status,
    received_amount,
    created_at
FROM open_payment_requests
ORDER BY created_at DESC
LIMIT 10;
```

Exit PostgreSQL:

```sql
\q
```

### One-command demo sequence

This checks the server, database connection, and saved requests without creating another payment:

```bash
BASE_URL="http://localhost:3000"

echo "=== Server Health ==="
curl -sS "$BASE_URL/health"
echo -e "\n"

echo "=== Database Health ==="
curl -sS "$BASE_URL/health/database"
echo -e "\n"

echo "=== Saved Payment Requests ==="
curl -sS "$BASE_URL/api/payments/open-payments"
echo
```

Use this for a quick presentation showing that the backend is running, PostgreSQL is reachable, and payment requests can be retrieved.

## Architecture

```text
Customer / Shop Owner
        |
        v
Frontend or Main Application
        |
        | HTTP + JSON
        v
Interledger Payment Service
        |
        +----> Validate request
        |
        +----> Open Payments SDK
        |          |
        |          v
        |      Open Payments Test Wallet
        |
        +----> Save request details
                   |
                   v
               PostgreSQL
        |
        v
Return request details
        |
        v
Frontend displays payment information
```

The payment service owns payment-specific logic and database access. The frontend should communicate with it over HTTP and must never receive database credentials or private keys.

## Project Structure

```text
InterledgerPaymentService/
├── secrets/
│   └── private.key
├── src/
│   ├── config/
│   │   └── database.js
│   ├── controllers/
│   │   └── openPaymentsController.js
│   ├── integrations/
│   │   └── open-payments/
│   │       ├── wallet-lookup.mjs
│   │       ├── incomingPayment.mjs
│   │       └── checkIncomingPayment.mjs
│   ├── middleware/
│   │   └── errorHandler.js
│   ├── main/
│   │   └── resources/
│   │       └── db/
│   │           └── migrations/
│   │               ├── 001_create_users.sql
│   │               ├── 002_create_payments.sql
│   │               └── 003_create_open_payment_requests.sql
│   ├── routes/
│   │   └── paymentRoutes.js
│   ├── services/
│   │   └── openPaymentService.js
│   ├── utils/
│   │   └── logger.js
│   ├── app.js
│   └── server.js
├── tests/
│   ├── controllers/
│   │   └── openPaymentsController.test.js
│   ├── integrations/
│   │   └── open-payments/
│   │       ├── wallet-lookup.test.js
│   │       └── incomingPayment.test.js
│   └── services/
│       └── openPaymentService.test.js
├── .env
├── .gitignore
├── node-modules/
├── package.json
├── package-lock.json
└── README.md
```

This shows the main files involved in the Open Payments request flow. Other project files may also exist.

## Security

- Never commit `.env`, `secrets/`, private keys, or access tokens.
- Ensure `.gitignore` excludes `.env`, `secrets/`, and key files.
- Use test-wallet credentials for demonstrations.
- Do not expose shared secrets or access tokens in public responses or logs.
- Add authentication and authorization before exposing payment data outside a trusted development environment.

## Troubleshooting

### Server does not start

- Check the terminal error.
- Confirm `.env` exists and contains the expected variables.
- Run `npm ci` if dependencies are missing.
- Check whether port `3000` is already in use.

### PostgreSQL connection fails

- Confirm PostgreSQL is running.
- Check the database credentials in `.env`.
- Confirm the database exists and the database user has permission to connect.
- Confirm the migration has been applied.

### Open Payments request fails

- Verify the wallet address, key ID, and private key path.
- Confirm the private key is readable by the application.
- Never share the private key or access tokens when asking for help.

### Payment remains pending

Creating an incoming-payment request does not mean funds have been received. Automatic status synchronization and verified settlement handling are not yet complete.

## Next Steps

1. Connect incoming-payment status retrieval to the API and persistence layer.
2. Securely obtain and use the correct read grant/access token for each payment resource.
3. Update local payment status and received amount using verified external resource state.
4. Add polling or notification handling to keep the database up to date.
5. Add automated tests for validation, persistence, errors, and status transitions.
6. Add API authentication and authorization.
7. Add an `.env.example` and verify that secret files are excluded from Git.
8. Document the API contract using OpenAPI/Swagger.
9. Test the complete payment flow, including a test-wallet payment and verified status update.

## References

- [Open Payments: Getting Started](https://openpayments.dev/overview/getting-started/)
- [Open Payments SDK Documentation](https://openpayments.dev/sdk/)
- [Interledger: Get Started](https://interledger.org/developers/get-started/)
- [Interledger Developer Portal](https://interledger.org/tech/dev-portal/)