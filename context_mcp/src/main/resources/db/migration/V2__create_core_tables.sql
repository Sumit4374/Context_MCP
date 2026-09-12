-- ============================================================
-- Core domain tables for the Context MCP platform
-- ============================================================

-- Projects: top-level organizational containers for context
CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    parent_project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    tags            JSONB        NOT NULL DEFAULT '[]',
    metadata        JSONB        NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_projects_name UNIQUE (name)
);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_projects_parent ON projects(parent_project_id);

-- Topics: sub-categories within or across projects
CREATE TABLE topics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_topics_name UNIQUE (name)
);

-- Project-Topic many-to-many
CREATE TABLE project_topics (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    topic_id   UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, topic_id)
);

-- Tags: reusable labels
CREATE TABLE tags (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_tags_name UNIQUE (name)
);

-- Conversations: external conversation tracking
CREATE TABLE conversations (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_conversation_id    VARCHAR(512) NOT NULL,
    source_client               VARCHAR(100),
    title                       VARCHAR(500),
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_conversations_external UNIQUE (external_conversation_id, source_client)
);
CREATE INDEX idx_conversations_external ON conversations(external_conversation_id);

-- Conversation Checkpoints: snapshots of conversation state
CREATE TABLE conversation_checkpoints (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_conversation_id    VARCHAR(512) NOT NULL,
    source_client               VARCHAR(100),
    turn_number                 INT,
    checkpoint_number           INT,
    request_payload             JSONB        NOT NULL DEFAULT '{}',
    evaluation                  JSONB        NOT NULL DEFAULT '{}',
    decision                    VARCHAR(50),
    user_confirmation           VARCHAR(50),
    suppressed                  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    confirmed_at                TIMESTAMPTZ
);
CREATE INDEX idx_checkpoints_conversation ON conversation_checkpoints(external_conversation_id);
CREATE INDEX idx_checkpoints_decision ON conversation_checkpoints(decision);

-- Memories: the core durable context records
CREATE TABLE memories (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                   VARCHAR(500),
    summary                 TEXT,
    content                 JSONB        NOT NULL DEFAULT '{}',
    memory_type             VARCHAR(50)  NOT NULL,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    importance              NUMERIC(5,4) NOT NULL DEFAULT 0.5,
    confidence              NUMERIC(5,4) NOT NULL DEFAULT 0.5,
    sensitivity             VARCHAR(50)  NOT NULL DEFAULT 'NONE',
    explicitness            VARCHAR(50)  NOT NULL DEFAULT 'CONFIRMED_RECOMMENDATION',
    source_client           VARCHAR(100),
    source_conversation_id  VARCHAR(512),
    source_checkpoint_id    UUID REFERENCES conversation_checkpoints(id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_retrieved_at       TIMESTAMPTZ,
    reinforcement_count     INT          NOT NULL DEFAULT 0,
    version                 BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_memories_type ON memories(memory_type);
CREATE INDEX idx_memories_status ON memories(status);
CREATE INDEX idx_memories_source_conv ON memories(source_conversation_id);
CREATE INDEX idx_memories_importance ON memories(importance DESC);
CREATE INDEX idx_memories_created ON memories(created_at DESC);

-- Memory-Project many-to-many
CREATE TABLE memory_projects (
    memory_id  UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, project_id)
);

-- Memory-Topic many-to-many
CREATE TABLE memory_topics (
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    topic_id  UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, topic_id)
);

-- Memory-Tag many-to-many
CREATE TABLE memory_tags (
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    tag_id    UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, tag_id)
);

-- Artifacts: tracked local files
CREATE TABLE artifacts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename   VARCHAR(500),
    canonical_path      TEXT NOT NULL,
    relative_path       TEXT,
    sha256              VARCHAR(64),
    mime_type           VARCHAR(255),
    artifact_type       VARCHAR(100),
    size_bytes          BIGINT,
    extracted_text      TEXT,
    generation_metadata JSONB        NOT NULL DEFAULT '{}',
    sensitivity         VARCHAR(50)  NOT NULL DEFAULT 'NONE',
    exists_on_disk      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    modified_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_indexed_at     TIMESTAMPTZ,
    CONSTRAINT uq_artifacts_path UNIQUE (canonical_path)
);
CREATE INDEX idx_artifacts_type ON artifacts(artifact_type);
CREATE INDEX idx_artifacts_sha256 ON artifacts(sha256);

-- Memory-Artifact many-to-many
CREATE TABLE memory_artifacts (
    memory_id   UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    artifact_id UUID NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, artifact_id)
);

-- Relationships: graph edges between any entities
CREATE TABLE relationships (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id           UUID         NOT NULL,
    source_type         VARCHAR(50)  NOT NULL,
    target_id           UUID         NOT NULL,
    target_type         VARCHAR(50)  NOT NULL,
    relationship_type   VARCHAR(50)  NOT NULL,
    confidence          NUMERIC(5,4) NOT NULL DEFAULT 1.0,
    provenance          VARCHAR(255),
    metadata            JSONB        NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_relationships_source ON relationships(source_id, source_type);
CREATE INDEX idx_relationships_target ON relationships(target_id, target_type);
CREATE INDEX idx_relationships_type ON relationships(relationship_type);

-- Suppression Rules: user decisions not to save context
CREATE TABLE suppression_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id     VARCHAR(512),
    checkpoint_id       UUID REFERENCES conversation_checkpoints(id) ON DELETE SET NULL,
    scope               VARCHAR(50) NOT NULL DEFAULT 'CONVERSATION',
    reason              TEXT,
    active              BOOLEAN     NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_suppressions_conv ON suppression_rules(conversation_id);
CREATE INDEX idx_suppressions_active ON suppression_rules(active) WHERE active = TRUE;

-- Audit Events: append-only log of material actions
CREATE TABLE audit_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   UUID,
    actor       VARCHAR(255),
    details     JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_action ON audit_events(action);
CREATE INDEX idx_audit_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_events(created_at DESC);

-- Embedding Jobs: queue for async vector embedding generation
CREATE TABLE embedding_jobs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_id  UUID        NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    status     VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_embedding_jobs_status ON embedding_jobs(status);
CREATE INDEX idx_embedding_jobs_memory ON embedding_jobs(memory_id);

-- Filesystem Watch Roots: configured artifact directories
CREATE TABLE filesystem_watch_roots (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    path         TEXT        NOT NULL,
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_scan_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_watch_roots_path UNIQUE (path)
);
