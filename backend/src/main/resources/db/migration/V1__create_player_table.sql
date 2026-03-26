CREATE SEQUENCE player_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE player (
    id            BIGINT       PRIMARY KEY DEFAULT nextval('player_seq'),
    username      VARCHAR(32)  NOT NULL UNIQUE,
    password_hash VARCHAR(72)  NOT NULL,
    wins          INT          NOT NULL DEFAULT 0,
    losses        INT          NOT NULL DEFAULT 0
);

