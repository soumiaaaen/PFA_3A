CREATE TABLE academic_year (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label      VARCHAR(20) NOT NULL UNIQUE,          -- ex. 2026-2027
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    CHECK (end_date > start_date)
);

CREATE TABLE semester (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID    NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    number           INTEGER NOT NULL CHECK (number IN (1, 2)),
    start_date       DATE    NOT NULL,
    end_date         DATE    NOT NULL,
    CHECK (end_date > start_date),
    UNIQUE (academic_year_id, number)
);

CREATE TABLE period (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    semester_id UUID    NOT NULL REFERENCES semester(id) ON DELETE CASCADE,
    number      INTEGER NOT NULL CHECK (number IN (1, 2)),
    start_date  DATE    NOT NULL,
    end_date    DATE    NOT NULL,
    CHECK (end_date >= start_date),
    UNIQUE (semester_id, number)
);