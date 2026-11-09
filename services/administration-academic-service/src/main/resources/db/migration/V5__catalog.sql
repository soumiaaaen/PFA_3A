CREATE TABLE module (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id  UUID         NOT NULL REFERENCES program(id),
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    study_level INTEGER      NOT NULL CHECK (study_level BETWEEN 1 AND 3),
    description TEXT,
    objectives  TEXT,
    coefficient NUMERIC(4,2) CHECK (coefficient > 0)
);
CREATE INDEX idx_module_program ON module(program_id);

CREATE TABLE element (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id   UUID         NOT NULL REFERENCES module(id) ON DELETE CASCADE,
    period_id   UUID         NOT NULL REFERENCES period(id),
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    objectives  TEXT,
    coefficient NUMERIC(4,2) CHECK (coefficient > 0)
);
CREATE INDEX idx_element_module ON element(module_id);
CREATE INDEX idx_element_period ON element(period_id);