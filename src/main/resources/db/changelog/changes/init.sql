--liquibase formatted sql

--changeset nitin:1
CREATE TABLE public.wallet (
  id bigserial NOT NULL,
  did varchar(255) NOT NULL,
  bpn varchar(255) NOT NULL,
  algorithm varchar(255) NOT NULL DEFAULT 'ED25519'::character varying,
  active bool NOT NULL,
  authority bool NOT NULL,
  did_document text NOT NULL,
  created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_at timestamp(6) NULL,
  modified_from varchar(255) NULL,
  CONSTRAINT uk_bpn UNIQUE (bpn),
  CONSTRAINT uk_did UNIQUE (did),
  CONSTRAINT wallet_pkey PRIMARY KEY (id),
  CONSTRAINT wallet_fk FOREIGN KEY (modified_from) REFERENCES public.wallet(bpn)
);


CREATE TABLE public.wallet_key (
  id bigserial NOT NULL,
  wallet_id bigserial NOT NULL,
  vault_access_token varchar(1000) NOT NULL,
  reference_key varchar(255) NOT NULL,
  created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_at timestamp(6) NULL,
  modified_from varchar(255) NULL,
  CONSTRAINT wallet_key_pkey PRIMARY KEY (id),
  CONSTRAINT wallet_fk FOREIGN KEY (wallet_id) REFERENCES public.wallet(id),
  CONSTRAINT wallet_key_fk FOREIGN KEY (modified_from) REFERENCES public.wallet(bpn)
);


CREATE TABLE public.credential (
  id bigserial NOT NULL,
  holder bigserial NOT NULL,
  issuer bigserial NOT NULL,
  "data" text NOT NULL,
  "type" varchar(255) NULL,
  created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_at timestamp(6) NULL,
  modified_from varchar(255) NULL,
  CONSTRAINT credential_pkey PRIMARY KEY (id),
  CONSTRAINT credential_fk FOREIGN KEY (modified_from) REFERENCES public.wallet(bpn),
  CONSTRAINT holder_wallet_fk FOREIGN KEY (holder) REFERENCES public.wallet(id),
  CONSTRAINT issuer_wallet_fk FOREIGN KEY (issuer) REFERENCES public.wallet(id)
);

--changeset nitin:2
ALTER TABLE public.wallet_key ADD private_key text NOT NULL;
ALTER TABLE public.wallet_key ADD public_key text NOT NULL;
