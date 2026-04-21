CREATE TABLE organization_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    invited_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    joined_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT org_members_unique UNIQUE (organization_id, user_id),
    -- Using VARCHAR + CHECK instead of ENUM for operational flexibility
    CONSTRAINT org_members_role_check CHECK (role IN ('OWNER','ADMIN','DEVELOPER','VIEWER'))
);

-- Indexes
CREATE INDEX idx_org_members_org_id ON organization_members(organization_id);
CREATE INDEX idx_org_members_user_id ON organization_members(user_id);

-- Enforce single owner
CREATE UNIQUE INDEX one_owner_per_org
ON organization_members (organization_id)
WHERE role = 'OWNER';

CREATE TABLE organization_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'DEVELOPER',
    token_hash      VARCHAR(255) NOT NULL,
    invited_by      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at      TIMESTAMP NOT NULL,
    accepted_at     TIMESTAMP,
    cancelled_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT org_invitations_role_check  CHECK (role IN ('ADMIN','DEVELOPER','VIEWER')),
    CONSTRAINT org_invitations_state_check CHECK (
        NOT (accepted_at IS NOT NULL AND cancelled_at IS NOT NULL)
    )
);

-- Indexes
CREATE INDEX idx_org_invitations_org_id ON organization_invitations(organization_id);
CREATE INDEX idx_org_invitations_email ON organization_invitations(email);

-- Make token_hash globally unique
CREATE UNIQUE INDEX idx_org_invitations_token
ON organization_invitations(token_hash);

-- Enforce unique active invitation per email per org
-- NOTE: "active" invitations also depend on expires_at (enforced at app layer)
CREATE UNIQUE INDEX unique_active_invite
ON organization_invitations (organization_id, email)
WHERE accepted_at IS NULL AND cancelled_at IS NULL;

-- Index for active invite queries
CREATE INDEX idx_active_invites
ON organization_invitations (organization_id)
WHERE accepted_at IS NULL AND cancelled_at IS NULL;

-- Backfill existing org owners as OWNER members
INSERT INTO organization_members (organization_id, user_id, role, joined_at)
SELECT id, owner_id, 'OWNER', created_at
FROM organizations
ON CONFLICT (organization_id, user_id)
DO UPDATE SET role = 'OWNER';