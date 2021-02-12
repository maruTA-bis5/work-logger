CREATE TABLE work_user (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'user',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE task (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES work_user(id),
    task_code VARCHAR(255) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    available_from DATE NOT NULL,
    available_until DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE task_manhour_fact (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(id),
    target_date DATE NOT NULL,
    work_start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    work_end_at TIMESTAMP WITH TIME ZONE NULL,
    total_mins INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE attendance_fact (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES work_user(id),
    target_date DATE NOT NULL,
    work_start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    work_end_at TIMESTAMP WITH TIME ZONE NULL,
    total_mins INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE break_fact (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES work_user(id),
    target_date DATE NOT NULL,
    break_kind VARCHAR(30) NOT NULL,
    break_start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    break_end_at TIMESTAMP WITH TIME ZONE NULL,
    total_mins INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);
