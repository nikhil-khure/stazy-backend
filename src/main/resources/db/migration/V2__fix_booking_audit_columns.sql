ALTER TABLE booking_requests
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

UPDATE booking_requests
SET created_at = COALESCE(created_at, requested_at, updated_at, NOW())
WHERE created_at IS NULL;

ALTER TABLE booking_requests
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE stay_cancel_requests
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

UPDATE stay_cancel_requests
SET created_at = COALESCE(created_at, requested_at, updated_at, NOW())
WHERE created_at IS NULL;

ALTER TABLE stay_cancel_requests
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN created_at SET NOT NULL;
