-- ==========================================
-- FIX: Endpoint Routing & Slug Resolution
-- ==========================================
-- Problems solved:
-- 1. Ambiguous URL patterns
-- 2. Inefficient nested queries (3+ DB hits per request)
-- 3. Broken unique constraints on endpoints table
-- 4. No clear hierarchy for path resolution
-- ==========================================

-- Step 1: Drop existing endpoints table and rebuild with hierarchy
DROP TABLE IF EXISTS endpoints CASCADE;

CREATE TABLE endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Full hierarchical path for URL routing (enables single-query resolution)
    full_path VARCHAR(500) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL,

    -- Resource type for fast filtering
    resource_type VARCHAR(20) NOT NULL CHECK (resource_type IN ('ORGANIZATION', 'PROJECT', 'SCHEMA')),

    -- Parent hierarchy
    parent_endpoint_id UUID REFERENCES endpoints(id) ON DELETE CASCADE,

    -- Resource FKs (exactly ONE will be non-null)
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    schema_id UUID REFERENCES mock_schemas(id) ON DELETE CASCADE,

    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Enforce exactly ONE resource FK
    CONSTRAINT endpoints_exactly_one_fk CHECK (
        (organization_id IS NOT NULL)::int +
        (project_id IS NOT NULL)::int +
        (schema_id IS NOT NULL)::int = 1
    ),

    -- Validate resource_type matches FK
    CONSTRAINT endpoints_type_matches_fk CHECK (
        (resource_type = 'ORGANIZATION' AND organization_id IS NOT NULL) OR
        (resource_type = 'PROJECT' AND project_id IS NOT NULL) OR
        (resource_type = 'SCHEMA' AND schema_id IS NOT NULL)
    )
);

-- Indexes for fast lookups
CREATE INDEX idx_endpoints_full_path ON endpoints(full_path);
CREATE INDEX idx_endpoints_resource_type ON endpoints(resource_type);
CREATE INDEX idx_endpoints_parent ON endpoints(parent_endpoint_id);
CREATE INDEX idx_endpoints_org_id ON endpoints(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_endpoints_project_id ON endpoints(project_id) WHERE project_id IS NOT NULL;
CREATE INDEX idx_endpoints_schema_id ON endpoints(schema_id) WHERE schema_id IS NOT NULL;

-- Step 2: Populate endpoints with hierarchical paths
-- Organizations (root level)
INSERT INTO endpoints (full_path, slug, resource_type, parent_endpoint_id, organization_id)
SELECT
    slug,                    -- full_path = just the slug
    slug,
    'ORGANIZATION',
    NULL,                    -- no parent
    id
FROM organizations;

-- Projects (under organizations)
INSERT INTO endpoints (full_path, slug, resource_type, parent_endpoint_id, project_id)
SELECT
    o.slug || '/' || p.slug,  -- full_path = org/project
    p.slug,
    'PROJECT',
    ep.id,                     -- parent = org endpoint
    p.id
FROM projects p
JOIN organizations o ON p.organization_id = o.id
JOIN endpoints ep ON ep.organization_id = o.id;

-- Schemas (under projects)
INSERT INTO endpoints (full_path, slug, resource_type, parent_endpoint_id, schema_id)
SELECT
    o.slug || '/' || p.slug || '/' || s.slug,  -- full_path = org/project/schema
    s.slug,
    'SCHEMA',
    ep.id,                                       -- parent = project endpoint
    s.id
FROM mock_schemas s
JOIN projects p ON s.project_id = p.id
JOIN organizations o ON p.organization_id = o.id
JOIN endpoints ep ON ep.project_id = p.id;

-- Step 3: Validation query to check data integrity
DO $$
DECLARE
    org_count INT;
    proj_count INT;
    schema_count INT;
    endpoint_count INT;
BEGIN
    SELECT COUNT(*) INTO org_count FROM organizations;
    SELECT COUNT(*) INTO proj_count FROM projects;
    SELECT COUNT(*) INTO schema_count FROM mock_schemas;
    SELECT COUNT(*) INTO endpoint_count FROM endpoints;

    RAISE NOTICE 'Organizations: %, Projects: %, Schemas: %, Endpoints: %',
        org_count, proj_count, schema_count, endpoint_count;

    IF endpoint_count != (org_count + proj_count + schema_count) THEN
        RAISE EXCEPTION 'Endpoint count mismatch! Expected %, got %',
            (org_count + proj_count + schema_count), endpoint_count;
    END IF;
END $$;