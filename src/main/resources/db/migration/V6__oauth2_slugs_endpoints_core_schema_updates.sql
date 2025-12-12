-- Core schema updates: OAuth2 support, slug-based routing, FK constraints, updated_at tracking and new endpoints table

-- 1. USERS
ALTER TABLE users
    ADD COLUMN provider_name VARCHAR(50) DEFAULT 'local' NOT NULL,
    ADD COLUMN provider_id VARCHAR(50),
    ADD COLUMN username VARCHAR(150),
    ADD COLUMN first_name VARCHAR(150),
    ADD COLUMN last_name VARCHAR(150),
    ADD COLUMN avatar_url TEXT,
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

-- Generate usernames for existing users from email
UPDATE users
SET username = LOWER(SPLIT_PART(email, '@', 1))
WHERE username IS NULL;

ALTER TABLE users ADD CONSTRAINT users_username_unique UNIQUE(username);
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Local users must have a password, OAuth users don't need one
ALTER TABLE users ADD CONSTRAINT users_local_password_check
    CHECK (
        (provider_name = 'local' AND password IS NOT NULL)
        OR provider_name <> 'local'
    );

--
-- 2. ORGANIZATIONS
ALTER TABLE organizations
    ADD COLUMN slug VARCHAR(255),
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

-- Generate slugs for existing organizations
UPDATE organizations
SET slug = LOWER(REGEXP_REPLACE(
    REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'),
    '^-|-$', '', 'g'
))
WHERE slug IS NULL;

ALTER TABLE organizations ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE organizations ALTER COLUMN slug SET NOT NULL;

-- Unique constraint: org slugs must be globally unique
ALTER TABLE organizations ADD CONSTRAINT organizations_slug_unique UNIQUE(slug);

--
-- 3. PROJECTS
ALTER TABLE projects
    ADD COLUMN slug VARCHAR(255),
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

-- Generate slugs for existing projects
UPDATE projects
SET slug = LOWER(REGEXP_REPLACE(
    REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'),
    '^-|-$', '', 'g'
))
WHERE slug IS NULL;

ALTER TABLE projects ALTER COLUMN slug SET NOT NULL;
ALTER TABLE projects ALTER COLUMN organization_id SET NOT NULL;

-- Unique constraint: project slugs must be unique within an organization
ALTER TABLE projects ADD CONSTRAINT projects_slug_org_unique
    UNIQUE (organization_id, slug);

--
-- 4. MOCK_SCHEMAS
ALTER TABLE mock_schemas
    ADD COLUMN slug VARCHAR(255),
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

-- Generate slugs for existing schemas
UPDATE mock_schemas
SET slug = LOWER(REGEXP_REPLACE(
    REGEXP_REPLACE(name, '[^a-zA-Z0-9]+', '-', 'g'),
    '^-|-$', '', 'g'
))
WHERE slug IS NULL;

ALTER TABLE mock_schemas ALTER COLUMN slug SET NOT NULL;
ALTER TABLE mock_schemas ALTER COLUMN project_id SET NOT NULL;

-- Unique constraint: schema slugs must be unique within a project
ALTER TABLE mock_schemas ADD CONSTRAINT mock_schemas_slug_project_unique
    UNIQUE (project_id, slug);

--
-- 5. MOCK_RECORDS
ALTER TABLE mock_records
    ADD COLUMN updated_at TIMESTAMP DEFAULT NOW();

ALTER TABLE mock_records
    ALTER COLUMN mock_schema_id SET NOT NULL;

--
-- 6. ENDPOINTS — central routing for all slug-based URLs
CREATE TABLE endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Three separate FK columns - exactly ONE will be non-null
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    schema_id UUID REFERENCES mock_schemas(id) ON DELETE CASCADE,

    -- Enforce exactly ONE fk
    CONSTRAINT endpoints_exactly_one_fk CHECK (
        (organization_id IS NOT NULL)::int +
        (project_id IS NOT NULL)::int +
        (schema_id IS NOT NULL)::int = 1
    ),

    -- Scoped uniqueness rules for slugs (match existing org/project/schema rules)
    CONSTRAINT endpoints_org_slug_unique
        UNIQUE (slug) DEFERRABLE INITIALLY DEFERRED,

    CONSTRAINT endpoints_project_slug_unique
       UNIQUE (organization_id, slug),

    CONSTRAINT endpoints_schema_slug_unique
        UNIQUE (project_id, slug)
);

-- 7. Populate endpoints table from existing data
INSERT INTO endpoints (organization_id, slug)
SELECT id, slug
FROM organizations;

INSERT INTO endpoints (project_id, slug)
SELECT id, slug
FROM projects;

INSERT INTO endpoints (schema_id, slug)
SELECT id, slug
FROM mock_schemas;
