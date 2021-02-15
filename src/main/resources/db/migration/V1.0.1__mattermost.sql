CREATE TABLE mattermost_config (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES work_user(id),
    mattermost_url VARCHAR(255) NOT NULL,
    mattermost_username VARCHAR(255) NOT NULL,
    mattermost_password VARCHAR(255) NOT NULL,
    team_id VARCHAR(255),
    channel_id VARCHAR(255),
    version BIGINT NOT NULL
);
