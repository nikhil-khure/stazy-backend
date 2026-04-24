-- Add gender column to student_profiles table
ALTER TABLE student_profiles ADD COLUMN gender VARCHAR(10);

-- Add comment to explain the column
COMMENT ON COLUMN student_profiles.gender IS 'Student gender: MALE or FEMALE';
