CREATE TABLE IF NOT EXISTS open_payment_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    open_payment_id TEXT NOT NULL UNIQUE,
    wallet_address TEXT NOT NULL,

    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',

    reference VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'EXPIRED')),

    received_amount NUMERIC(12, 2) NOT NULL DEFAULT 0
        CHECK (received_amount >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_open_payment_requests_status
    ON open_payment_requests(status);

CREATE INDEX IF NOT EXISTS idx_open_payment_requests_created_at
    ON open_payment_requests(created_at DESC);