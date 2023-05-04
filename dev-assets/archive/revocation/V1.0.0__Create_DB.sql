CREATE TABLE lists (
    list_name character varying(255),
    profile_name character varying(255),
    encoded_list character varying(1048576) NULL,
    list_credential character varying(1048576) NULL,
    last_update timestamp with time zone NOT NULL DEFAULT '1970-01-01T00:00:00.00Z',
    PRIMARY KEY (list_name),
    UNIQUE (profile_name)
);

CREATE TABLE entry_counter (
    list_name character varying(255) NOT NULL,
    last_idx bigint NOT NULL,
    PRIMARY KEY (list_name),
    FOREIGN KEY (list_name) REFERENCES lists (list_name)
);

CREATE TABLE list_entry (
    list_name character varying(255),
    index bigint,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    revoked boolean NOT NULL DEFAULT false,
    revoked_at timestamp with time zone NULL,
    processed_to_list boolean NOT NULL DEFAULT false,
    PRIMARY KEY (list_name, index),
    FOREIGN KEY (list_name) REFERENCES lists (list_name)
);


CREATE INDEX idx_list_entry_processed ON list_entry (list_name, processed_to_list, revoked)
    WHERE processed_to_list IS false AND revoked IS true;
