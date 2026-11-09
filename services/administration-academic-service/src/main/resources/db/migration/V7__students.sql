CREATE TABLE student (
    user_id        UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    student_number VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE group_enrollment (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_user_id  UUID        NOT NULL REFERENCES student(user_id) ON DELETE CASCADE,
    group_id         UUID        NOT NULL REFERENCES student_group(id),
    academic_year_id UUID        NOT NULL REFERENCES academic_year(id),
    enrolled_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_user_id, academic_year_id)
);
CREATE INDEX idx_enrollment_group ON group_enrollment(group_id);