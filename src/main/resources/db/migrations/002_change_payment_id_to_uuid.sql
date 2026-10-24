BEGIN;

-- Add a UUID column and generate an ID for every existing payment.
ALTER TABLE payments
    ADD COLUMN new_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Remove the existing primary key constraint.
DO $$
DECLARE
pk_name TEXT;
BEGIN
SELECT conname
INTO pk_name
FROM pg_constraint
WHERE conrelid = 'payments'::regclass
      AND contype = 'p';

IF pk_name IS NOT NULL THEN
        EXECUTE format(
            'ALTER TABLE payments DROP CONSTRAINT %I',
            pk_name
        );
END IF;
END $$;

-- Replace the numerical ID with the UUID.
ALTER TABLE payments DROP COLUMN id;

ALTER TABLE payments RENAME COLUMN new_id TO id;

-- Make the UUID the primary key.
ALTER TABLE payments ADD PRIMARY KEY (id);

COMMIT;