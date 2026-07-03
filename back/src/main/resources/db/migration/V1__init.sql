-- AllocHub schema (PostgreSQL / Flyway)

CREATE TABLE investors (
    id                        VARCHAR(36)  PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL UNIQUE,
    investment_amount         INTEGER      NOT NULL,
    allocation_ratio          DOUBLE PRECISION NOT NULL,
    status                    VARCHAR(32)  NOT NULL,
    cumulative_distribution   INTEGER      NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ  NOT NULL,
    created_by                VARCHAR(255) NOT NULL,
    updated_at                TIMESTAMPTZ,
    updated_by                VARCHAR(255)
);

CREATE INDEX idx_investors_created_at ON investors (created_at);
CREATE INDEX idx_investors_status ON investors (status);

CREATE TABLE investments (
    id                 VARCHAR(36)  PRIMARY KEY,
    company_name       VARCHAR(255) NOT NULL,
    investment_amount  INTEGER      NOT NULL,
    investment_date    TIMESTAMPTZ  NOT NULL,
    status             VARCHAR(32)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    created_by         VARCHAR(255) NOT NULL
);

CREATE INDEX idx_investments_investment_date ON investments (investment_date);

CREATE TABLE investor_investments (
    id                VARCHAR(36) PRIMARY KEY,
    investor_id       VARCHAR(36) NOT NULL REFERENCES investors (id),
    investment_id     VARCHAR(36) NOT NULL REFERENCES investments (id),
    allocated_amount  INTEGER     NOT NULL,
    CONSTRAINT uq_investor_investment UNIQUE (investor_id, investment_id)
);

CREATE INDEX idx_investor_investments_investment_id ON investor_investments (investment_id);

CREATE TABLE distributions (
    id                   VARCHAR(36)  PRIMARY KEY,
    investment_id        VARCHAR(36)  NOT NULL REFERENCES investments (id),
    distribution_amount  INTEGER      NOT NULL,
    distribution_type    VARCHAR(64)  NOT NULL,
    status               VARCHAR(32)  NOT NULL,
    distribution_date    TIMESTAMPTZ  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(255) NOT NULL
);

CREATE INDEX idx_distributions_investment_id ON distributions (investment_id);
CREATE INDEX idx_distributions_distribution_date ON distributions (distribution_date);

CREATE TABLE distribution_details (
    id                  VARCHAR(36) PRIMARY KEY,
    distribution_id     VARCHAR(36) NOT NULL REFERENCES distributions (id) ON DELETE CASCADE,
    investor_id         VARCHAR(36) NOT NULL REFERENCES investors (id),
    distributed_amount  INTEGER     NOT NULL,
    CONSTRAINT uq_distribution_investor UNIQUE (distribution_id, investor_id)
);

CREATE TABLE audit_logs (
    id           VARCHAR(36)  PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    action       VARCHAR(64)  NOT NULL,
    entity_type  VARCHAR(64)  NOT NULL,
    entity_id    VARCHAR(36)  NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, action);
