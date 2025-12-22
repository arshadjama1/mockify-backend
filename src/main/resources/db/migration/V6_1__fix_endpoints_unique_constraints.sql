--  Drop the problematic constraints
ALTER TABLE endpoints
DROP CONSTRAINT IF EXISTS endpoints_org_slug_unique,
DROP CONSTRAINT IF EXISTS endpoints_project_slug_unique,
DROP CONSTRAINT IF EXISTS endpoints_schema_slug_unique;

-- Create partial unique indexes for proper scoped uniqueness
-- For organizations: globally unique slug (no parent)
CREATE UNIQUE INDEX endpoints_org_slug_unique
ON endpoints (slug)
WHERE organization_id IS NOT NULL;

-- For projects: unique slug within organization
CREATE UNIQUE INDEX endpoints_project_slug_unique
ON endpoints (organization_id, slug)
WHERE project_id IS NOT NULL;

-- For schemas: unique slug within project
CREATE UNIQUE INDEX endpoints_schema_slug_unique
ON endpoints (project_id, slug)
WHERE schema_id IS NOT NULL;
