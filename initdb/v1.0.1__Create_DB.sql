-- Use a schema to organize database objects
CREATE SCHEMA IF NOT EXISTS identity_wallet;

-- Create a table to store the authority wallets
CREATE TABLE IF NOT EXISTS identity_wallet.authority_wallets (
    id SERIAL PRIMARY KEY,
    did TEXT UNIQUE NOT NULL,
    private_key TEXT NOT NULL
);

-- Create a table to store the registered tenant wallets
CREATE TABLE IF NOT EXISTS identity_wallet.tenant_wallets (
    id SERIAL PRIMARY KEY,
    uuid TEXT UNIQUE NOT NULL,
    did TEXT UNIQUE NOT NULL,
    subdomain TEXT NOT NULL,
    did_document TEXT NOT NULL,
    authority_wallet_id INTEGER NOT NULL,
    private_key_id INTEGER NOT NULL,
    FOREIGN KEY (authority_wallet_id) REFERENCES identity_wallet.authority_wallets (id),
    FOREIGN KEY (private_key_id) REFERENCES identity_wallet.private_keys (id)
);

-- Create a table to store the unregistered tenant wallets
CREATE TABLE IF NOT EXISTS identity_wallet.unregistered_wallets (
    id SERIAL PRIMARY KEY,
    uuid TEXT UNIQUE NOT NULL,
    did TEXT UNIQUE NOT NULL,
    subdomain TEXT NOT NULL,
    did_document TEXT NOT NULL
);

-- Create a table to store the private keys for the tenant wallets
CREATE TABLE IF NOT EXISTS identity_wallet.private_keys (
    id SERIAL PRIMARY KEY,
    private_key_data TEXT NOT NULL,
    wallet_id INTEGER NOT NULL,
    FOREIGN KEY (wallet_id) REFERENCES identity_wallet.tenant_wallets (id)
);

-- Create a table to store the credentials issued by the authority
CREATE TABLE IF NOT EXISTS identity_wallet.credentials (
    id SERIAL PRIMARY KEY,
    credential_data TEXT NOT NULL,
    tenant_wallet_id INTEGER NOT NULL,
    authority_wallet_id INTEGER NOT NULL,
    FOREIGN KEY (tenant_wallet_id) REFERENCES identity_wallet.tenant_wallets (id),
    FOREIGN KEY (authority_wallet_id) REFERENCES identity_wallet.authority_wallets (id)
);

-- Add indexes to improve performance
CREATE UNIQUE INDEX IF NOT EXISTS idx_tenant_wallets_did ON identity_wallet.tenant_wallets (did);
CREATE UNIQUE INDEX IF NOT EXISTS idx_unregistered_wallets_did ON identity_wallet.unregistered_wallets (did);
CREATE UNIQUE INDEX IF NOT EXISTS idx_authority_wallets_did ON identity_wallet.authority_wallets (did);
