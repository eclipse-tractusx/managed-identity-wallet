--liquibase formatted sql

--changeset nitin:1
CREATE TABLE IF NOT EXISTS public.wallet
(
    id            bigserial    NOT NULL,
    name          varchar(255) NOT NULL,
    did           varchar(255) NOT NULL,
    bpn           varchar(255) NOT NULL,
    algorithm     varchar(255) NOT NULL DEFAULT 'ED25519'::character varying,
    did_document  text         NOT NULL,
    created_at    timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at   timestamp(6) NULL,
    modified_from varchar(255) NULL,
    CONSTRAINT uk_bpn UNIQUE (bpn),
    CONSTRAINT uk_did UNIQUE (did),
    CONSTRAINT wallet_pkey PRIMARY KEY (id),
    CONSTRAINT wallet_fk FOREIGN KEY (modified_from) REFERENCES public.wallet (bpn) ON DELETE SET NULL
);
COMMENT ON TABLE public.wallet IS 'This table will store wallets';

CREATE TABLE IF NOT EXISTS public.wallet_key
(
    id                 bigserial     NOT NULL,
    wallet_id          bigserial     NOT NULL,
    vault_access_token varchar(1000) NOT NULL,
    reference_key      varchar(255)  NOT NULL,
    private_key        text          NOT NULL,
    public_key         text          NOT NULL,
    created_at         timestamp(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at        timestamp(6)  NULL,
    modified_from      varchar(255)  NULL,
    CONSTRAINT wallet_key_pkey PRIMARY KEY (id),
    CONSTRAINT wallet_fk_2 FOREIGN KEY (wallet_id) REFERENCES public.wallet (id) ON DELETE CASCADE,
    CONSTRAINT wallet_key_fk FOREIGN KEY (modified_from) REFERENCES public.wallet (bpn) ON DELETE CASCADE
);
COMMENT ON TABLE public.wallet_key IS 'This table will store key pair of wallets';


CREATE TABLE IF NOT EXISTS public.issuers_credential
(
    id            bigserial    NOT NULL,
    holder_did    varchar(255) NOT NULL,
    issuer_did    varchar(255) NOT NULL,
    credential_id varchar(255) NOT NULL,
    credential_data        text         NOT NULL,
    credential_type        varchar(255) NULL,
    created_at    timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at   timestamp(6) NULL,
    modified_from varchar(255) NULL,
    CONSTRAINT issuers_credential_pkey PRIMARY KEY (id),
    CONSTRAINT issuers_credential_fk FOREIGN KEY (modified_from) REFERENCES public.wallet (bpn) ON DELETE SET NULL,
    CONSTRAINT issuers_credential_holder_wallet_fk FOREIGN KEY (holder_did) REFERENCES public.wallet (did) ON DELETE CASCADE
);
COMMENT ON TABLE public.issuers_credential IS 'This table will store issuers credentials';


CREATE TABLE IF NOT EXISTS public.holders_credential
(
    id             bigserial    NOT NULL,
    holder_did     varchar(255) NOT NULL,
    issuer_did     varchar(255) NOT NULL,
    credential_id  varchar(255) NOT NULL,
    credential_data         text         NOT NULL,
    credential_type         varchar(255) NULL,
    is_self_issued bool         NOT null default false,
    is_stored      bool         NOT null default false,
    created_at     timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at    timestamp(6) NULL,
    modified_from  varchar(255) NULL,
    CONSTRAINT holders_credential_pkey PRIMARY KEY (id),
    CONSTRAINT holders_credential_fk FOREIGN KEY (modified_from) REFERENCES public.wallet (bpn) ON DELETE SET NULL,
    CONSTRAINT holders_credential_holder_wallet_fk FOREIGN KEY (holder_did) REFERENCES public.wallet (did) ON DELETE CASCADE
);
COMMENT ON TABLE public.holders_credential IS 'This table will store holders credentials';

COMMENT ON COLUMN public.holders_credential.is_stored IS 'true is VC is stored using store VC api(Not issued by MIW)';

--changeset nitin:2
ALTER TABLE public.wallet_key ADD key_id varchar(255) NULL;
