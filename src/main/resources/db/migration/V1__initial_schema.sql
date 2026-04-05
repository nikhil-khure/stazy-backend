CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE cities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    country VARCHAR(120) NOT NULL DEFAULT 'India',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_city UNIQUE (name, state, country)
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_code VARCHAR(50) NOT NULL UNIQUE,
    primary_role_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    email VARCHAR(190) NOT NULL UNIQUE,
    mobile_number VARCHAR(30),
    password_hash VARCHAR(255),
    auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_identity_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    completion_percentage INTEGER NOT NULL DEFAULT 0,
    profile_photo_url TEXT,
    profile_photo_public_id VARCHAR(255),
    last_login_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_primary_role_code ON users(primary_role_code);
CREATE INDEX idx_users_account_status ON users(account_status);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(190),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_oauth_provider_subject UNIQUE (provider, provider_subject)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(255),
    ip_address VARCHAR(100),
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE otp_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    email VARCHAR(190),
    mobile_number VARCHAR(30),
    purpose VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    hashed_code VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    context_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_challenges_user_id ON otp_challenges(user_id);
CREATE INDEX idx_otp_challenges_status ON otp_challenges(status);

CREATE TABLE student_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    college_name VARCHAR(200),
    prn VARCHAR(100),
    enrollment_number VARCHAR(100),
    current_location VARCHAR(255),
    city_id BIGINT REFERENCES cities(id),
    bio TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE owner_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    pan_number VARCHAR(50),
    pg_name VARCHAR(200),
    business_name VARCHAR(200),
    address_line_one VARCHAR(255),
    address_line_two VARCHAR(255),
    locality VARCHAR(150),
    pincode VARCHAR(20),
    city_id BIGINT REFERENCES cities(id),
    signature_url TEXT,
    signature_public_id VARCHAR(255),
    bio TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    city_id BIGINT REFERENCES cities(id),
    secret_code_hash VARCHAR(255),
    employee_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    can_manage_all_cities BOOLEAN NOT NULL DEFAULT FALSE,
    last_otp_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_hiring_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    mobile_number VARCHAR(30) NOT NULL,
    email VARCHAR(190) NOT NULL,
    resume_url TEXT,
    resume_public_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    assigned_city_id BIGINT REFERENCES cities(id),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE contact_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(190) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    target_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    feedback_scope VARCHAR(50) NOT NULL,
    rating INTEGER,
    message TEXT NOT NULL,
    display_name_snapshot VARCHAR(150),
    email_snapshot VARCHAR(190),
    location_snapshot VARCHAR(190),
    is_authenticated BOOLEAN NOT NULL DEFAULT FALSE,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    visibility_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feedbacks_scope ON feedbacks(feedback_scope);

CREATE TABLE listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    city_id BIGINT REFERENCES cities(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    address_line_one VARCHAR(255),
    address_line_two VARCHAR(255),
    locality VARCHAR(150),
    pincode VARCHAR(20),
    room_kind VARCHAR(50) NOT NULL,
    gender_category VARCHAR(30) NOT NULL,
    rent_amount NUMERIC(12,2) NOT NULL,
    total_capacity INTEGER NOT NULL DEFAULT 1,
    available_capacity INTEGER NOT NULL DEFAULT 1,
    rating_average NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count INTEGER NOT NULL DEFAULT 0,
    amenities JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(30) NOT NULL DEFAULT 'UNDER_REVIEW',
    rejection_reason TEXT,
    latest_fake_detection_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    latest_verification_request_id UUID,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_owner_user_id ON listings(owner_user_id);
CREATE INDEX idx_listings_city_id ON listings(city_id);
CREATE INDEX idx_listings_status ON listings(status);
CREATE INDEX idx_listings_rent_amount ON listings(rent_amount);
CREATE INDEX idx_listings_locality ON listings(locality);

CREATE TABLE listing_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    media_type VARCHAR(30) NOT NULL,
    url TEXT NOT NULL,
    public_id VARCHAR(255),
    mime_type VARCHAR(120),
    file_size_bytes BIGINT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_json JSONB,
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listing_media_listing_id ON listing_media(listing_id);

CREATE TABLE verification_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    listing_id UUID REFERENCES listings(id) ON DELETE CASCADE,
    requested_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    verification_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ai_endpoint TEXT,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    request_summary_json JSONB,
    raw_response_json JSONB,
    accepted_parameters_json JSONB,
    rejected_parameters_json JSONB,
    failure_reason TEXT,
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_requests_user_id ON verification_requests(user_id);
CREATE INDEX idx_verification_requests_listing_id ON verification_requests(listing_id);
CREATE INDEX idx_verification_requests_type_status ON verification_requests(verification_type, status);

ALTER TABLE listings
    ADD CONSTRAINT fk_listings_latest_verification_request
        FOREIGN KEY (latest_verification_request_id) REFERENCES verification_requests(id);

CREATE TABLE verification_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    verification_request_id UUID NOT NULL REFERENCES verification_requests(id) ON DELETE CASCADE,
    attachment_type VARCHAR(50) NOT NULL,
    url TEXT NOT NULL,
    public_id VARCHAR(255),
    mime_type VARCHAR(120),
    file_size_bytes BIGINT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE booking_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_rent NUMERIC(12,2),
    message TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ,
    decided_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_requests_student_user_id ON booking_requests(student_user_id);
CREATE INDEX idx_booking_requests_owner_user_id ON booking_requests(owner_user_id);
CREATE INDEX idx_booking_requests_listing_id ON booking_requests(listing_id);

CREATE TABLE active_stays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_request_id UUID NOT NULL UNIQUE REFERENCES booking_requests(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_code VARCHAR(80),
    join_date DATE NOT NULL,
    current_month_start DATE,
    current_month_end DATE,
    monthly_rent NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    current_payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    next_due_date DATE,
    reminder_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_active_stays_student_user_id ON active_stays(student_user_id);
CREATE INDEX idx_active_stays_owner_user_id ON active_stays(owner_user_id);

CREATE TABLE rent_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active_stay_id UUID NOT NULL REFERENCES active_stays(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    reminder_message TEXT,
    transaction_reference VARCHAR(120),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rent_payments_active_stay_id ON rent_payments(active_stay_id);
CREATE INDEX idx_rent_payments_status ON rent_payments(status);

CREATE TABLE stay_cancel_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active_stay_id UUID NOT NULL REFERENCES active_stays(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    account_status_snapshot VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'UNDER_PROGRESS',
    owner_reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complainant_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    against_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    related_listing_id UUID REFERENCES listings(id) ON DELETE SET NULL,
    related_stay_id UUID REFERENCES active_stays(id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    created_by_role_code VARCHAR(50) NOT NULL,
    against_role_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    current_resolution_summary TEXT,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaints_complainant_user_id ON complaints(complainant_user_id);
CREATE INDEX idx_complaints_against_user_id ON complaints(against_user_id);
CREATE INDEX idx_complaints_status ON complaints(status);

CREATE TABLE complaint_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    author_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE complaint_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    complaint_message_id UUID REFERENCES complaint_messages(id) ON DELETE SET NULL,
    url TEXT NOT NULL,
    public_id VARCHAR(255),
    mime_type VARCHAR(120),
    file_size_bytes BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    action_path VARCHAR(255),
    metadata_json JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id_read ON notifications(user_id, is_read);

CREATE TABLE admin_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    city_id BIGINT REFERENCES cities(id),
    subject VARCHAR(200),
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    replied_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reply_message TEXT,
    replied_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_queries_admin_user_id ON admin_queries(admin_user_id);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_role_code VARCHAR(50),
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id VARCHAR(120),
    summary TEXT,
    metadata_json JSONB,
    ip_address VARCHAR(100),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles (code, name, description)
VALUES
    ('STUDENT', 'Student', 'Student end user'),
    ('OWNER', 'Owner', 'Room or PG owner'),
    ('ADMIN', 'Admin', 'City scoped administrator'),
    ('SUPER_ADMIN', 'Super Admin', 'Global platform administrator')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description)
VALUES
    ('listing:read', 'Read Listings', 'View room listings'),
    ('listing:write', 'Manage Listings', 'Create or update owner listings'),
    ('verification:run', 'Run Verification', 'Submit AI verification requests'),
    ('booking:manage', 'Manage Bookings', 'Manage booking lifecycle'),
    ('complaint:manage', 'Manage Complaints', 'Create and resolve complaints'),
    ('admin:moderate', 'Moderate Platform', 'Moderate users and listings'),
    ('admin:hire', 'Hire Admins', 'Approve admin hiring requests'),
    ('query:reply', 'Reply To Admin Queries', 'Reply to admin escalations')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON (r.code = 'STUDENT' AND p.code IN ('listing:read', 'verification:run', 'complaint:manage'))
    OR (r.code = 'OWNER' AND p.code IN ('listing:read', 'listing:write', 'verification:run', 'booking:manage', 'complaint:manage'))
    OR (r.code = 'ADMIN' AND p.code IN ('listing:read', 'admin:moderate', 'verification:run', 'query:reply'))
    OR (r.code = 'SUPER_ADMIN' AND p.code IN ('listing:read', 'admin:moderate', 'admin:hire', 'query:reply'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO cities (name, state, country)
VALUES ('Pune', 'Maharashtra', 'India')
ON CONFLICT (name, state, country) DO NOTHING;
