-- Add listing_id column to feedbacks table
-- Required for Requirement 17: Feedback-Listing relationship

-- Add the listing_id column (nullable to allow existing data)
ALTER TABLE feedbacks 
ADD COLUMN listing_id UUID;

-- Add foreign key constraint
ALTER TABLE feedbacks 
ADD CONSTRAINT fk_feedbacks_listing 
FOREIGN KEY (listing_id) 
REFERENCES listings(id) 
ON DELETE SET NULL;

-- Create index for better query performance
CREATE INDEX idx_feedbacks_listing_id ON feedbacks(listing_id);
