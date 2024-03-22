--liquibase formatted sql
--changeset pmanaras:3
ALTER TABLE public.wallet ADD signing_service_type VARCHAR(255) NOT NULL DEFAULT 'LOCAL';
