CREATE TABLE teaching_assignment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    element_id      UUID         NOT NULL REFERENCES element(id),
    teacher_user_id UUID         NOT NULL REFERENCES users(id),
    group_id        UUID         NOT NULL REFERENCES student_group(id),
    period_id       UUID         NOT NULL REFERENCES period(id),
    activity_type   VARCHAR(2)   NOT NULL CHECK (activity_type IN ('CM', 'TD', 'TP')),
    target_hours    NUMERIC(6,2) NOT NULL CHECK (target_hours > 0),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

-- Pas deux affectations actives identiques (une affectation désactivée peut être recréée)
CREATE UNIQUE INDEX uq_assignment_active
    ON teaching_assignment(element_id, teacher_user_id, group_id, activity_type)
    WHERE active;

CREATE INDEX idx_assignment_period  ON teaching_assignment(period_id);
CREATE INDEX idx_assignment_teacher ON teaching_assignment(teacher_user_id);

CREATE TABLE assignment_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id       UUID        NOT NULL REFERENCES teaching_assignment(id),
    action              VARCHAR(20) NOT NULL,
    old_teacher_user_id UUID,
    new_teacher_user_id UUID,
    old_target_hours    NUMERIC(6,2),
    new_target_hours    NUMERIC(6,2),
    changed_by          UUID        NOT NULL,
    changed_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_history_assignment ON assignment_history(assignment_id);