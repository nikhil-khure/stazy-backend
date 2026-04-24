-- Add blocking fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS block_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked_at TIMESTAMP WITH TIME ZONE;

-- Add revocation fields to admin_profiles table
ALTER TABLE admin_profiles ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE admin_profiles ADD COLUMN IF NOT EXISTS revoke_reason TEXT;
