CREATE TABLE roles (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code  VARCHAR(30)  NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL
);

INSERT INTO roles (code, label) VALUES
    ('ADMIN',            'Administrateur'),
    ('CHEF_DEPARTEMENT', 'Chef de département'),
    ('CHEF_FILIERE',     'Chef de filière'),
    ('ENSEIGNANT',       'Enseignant'),
    ('ETUDIANT',         'Étudiant');

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);