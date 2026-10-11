CREATE TABLE level (
                       id      smallint     NOT NULL,
                       name    varchar(50)  NOT NULL,
                       colour  varchar(7)   NOT NULL,
                       CONSTRAINT pk_level        PRIMARY KEY (id),
                       CONSTRAINT uq_level_name   UNIQUE (name),
                       CONSTRAINT ck_level_id     CHECK (id BETWEEN 1 AND 6),
                       CONSTRAINT ck_level_colour CHECK (colour ~ '^#[0-9A-Fa-f]{6}$')
    );
