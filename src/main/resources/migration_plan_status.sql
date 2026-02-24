-- 1) Add status column if not exists (Hibernate might have added it already)
-- ALTER TABLE plans ADD COLUMN status VARCHAR(50) DEFAULT 'ACTIVE';

-- 2) Backfill existing rows to ACTIVE
UPDATE plans SET status = 'ACTIVE' WHERE status IS NULL;

-- 3) Ensure status is NOT NULL for future
-- ALTER TABLE plans MODIFY COLUMN status VARCHAR(50) NOT NULL;
