CREATE TABLE department (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(20)  NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    head_user_id UUID REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE program (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID         NOT NULL REFERENCES department(id),
    code          VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    head_user_id  UUID REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_program_department ON program(department_id);

CREATE TABLE student_group (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id       UUID        NOT NULL REFERENCES program(id),
    academic_year_id UUID        NOT NULL REFERENCES academic_year(id),
    study_level      INTEGER     NOT NULL CHECK (study_level BETWEEN 1 AND 3),
    name             VARCHAR(50) NOT NULL,
    UNIQUE (program_id, academic_year_id, study_level, name)
);