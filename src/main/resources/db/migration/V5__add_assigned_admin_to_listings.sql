-- Add assigned_admin_id column to listings table
ALTER TABLE listings ADD COLUMN assigned_admin_id UUID;

-- Add foreign key constraint
ALTER TABLE listings ADD CONSTRAINT fk_listings_assigned_admin 
    FOREIGN KEY (assigned_admin_id) REFERENCES users(id);

-- Create index for better query performance
CREATE INDEX idx_listings_assigned_admin ON listings(assigned_admin_id);
