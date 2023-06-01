# !!! Under Contruction !!!

# Managed Identity Wallets <a id="introduction"></a>

The Managed Identity Wallets (MIW) service implements the Self-Sovereign-Identity (SSI)
readiness by providing a wallet hosting platform including a DID resolver,
service endpoints and the company wallets itself.

> **Warning**
> Heavily under development
>
# Developer Documentation

To run MIW locally, this section describes the tooling as well as
the local development setup.

## Tooling
To be added

## Environment Variables <a id= "environmentVariables"></a>

| name                       | description                                                                                   | default value                                                                                                                      |
|----------------------------|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| APPLICATION_PORT           | port number of application                                                                    | 8087                                                                                                                               | 
| APPLICATION_ENVIRONMENT    | Environment of the application ie. local, dev, int and prod                                   | local                                                                                                                              |
| DB_HOST                    | Database host                                                                                 | localhost                                                                                                                          |
| DB_PORT                    | Port of database                                                                              | 5432                                                                                                                               |
| DB_NAME                    | Database name                                                                                 | miw                                                                                                                                |
| USE_SSL                    | Whether SSL is enabled in database server                                                     | false                                                                                                                              |
| DB_USER_NAME               | Database username                                                                             |                                                                                                                                    |
| DB_PASSWORD                | Database password                                                                             |                                                                                                                                    |
| DB_POOL_SIZE               | Max number of database connection acquired by application                                     | 10                                                                                                                                 |
| KEYCLOAK_MIW_PUBLIC_CLIENT | Only needed if we want enable login with keyalock in swagger                                  | miw_public                                                                                                                         |
| MANAGEMENT_PORT            | Spring actuator port                                                                          | 8090                                                                                                                               |
| MIW_HOST_NAME              | Application host name, this will be used in creation of did ie. did:web:MIW_HOST_NAME:BPN     | localhost                                                                                                                          |
| ENCRYPTION_KEY             | encryption key used to encrypt and decrypt private and public key of wallet                   |                                                                                                                                    |
| AUTHORITY_WALLET_BPN       | base wallet BPN number                                                                        | BPNL000000000000                                                                                                                   |
| AUTHORITY_WALLET_NAME      | Base wallet name                                                                              | Catena-X                                                                                                                           |
| AUTHORITY_WALLET_DID       | Base wallet web did                                                                           | web:did:host:BPNL000000000000                                                                                                      |
| VC_SCHEMA_LINK             | Comma separated list of VC schema URL                                                         | https://www.w3.org/2018/credentials/v1, https://raw.githubusercontent.com/catenax-ng/product-core-schemas/main/businessPartnerData |
| VC_EXPIRY_DATE             | Expiry date of VC (dd-MM-yyyy ie.  01-01-2025 expiry date will be 2024-12-31T18:30:00Z in VC) | 01-01-2025                                                                                                                         |
| KEYCLOAK_REALM             | Realm name of keycloak                                                                        | miw_test                                                                                                                           |
| KEYCLOAK_CLIENT_ID         | Keycloak private client id                                                                    |                                                                                                                                    |
| AUTH_SERVER_URL            | Keycloak server url                                                                           |                                                                                                                                    |
|                            |                                                                                               |                                                                                                                                    |
## Local Development Setup

*Work in progress*

## Setup Summary

| Service               | URL                     | Description |
|-----------------------|-------------------------|-------------|
| postgreSQL database   | port 5432 on `localhost`| within the Docker Compose setup |
| Keycloak              | http://localhost:8081/  | within the Docker Compose setup, username: `admin` and password: `changeme`, client id: `ManagedIdentityWallets` and client secret can be found under the Clients &gt; ManagedIdentityWallets &gt; Credentials |
| MIW service           | http://localhost:8080/  | |


# Administrator Documentation

## Manual Keycloak Configuration

Within the development setup the Keycloak is initially prepared with the
values in `./dev-assets/dev-containers/keycloak`. The realm could also be
manually added and configured at http://localhost:8081 via the "Add realm"
button. It can be for example named `localkeycloak`. Also add an additional client,
e.g. named `ManagedIdentityWallets` with *valid redirect url* set to
`http://localhost:8080/*`. The roles
 * add_wallets
 * view_wallets
 * update_wallets
 * delete_wallets
 * view_wallet
 * update_wallet
can be added under *Clients > ManagedIdentityWallets > Roles* and then
assigned to the client using *Clients > ManagedIdentityWallets > Client Scopes*
*> Service Account Roles > Client Roles > ManagedIdentityWallets*. The
available scopes/roles are:

1. Role `add_wallets` to create a new wallet

1. Role `view_wallets`:
    * to get a list of all wallets
    * to retrieve one wallet by its identifier
    * to validate a Verifiable Presentation
    * to get all stored Verifiable Credentials

1. Role `update_wallets` for the following actions:
    * to store Verifiable Credential
    * to set the wallet DID to public on chain
    * to issue a Verifiable Credential 
    * to issue a Verifiable Presentation
    * to add, update and delete service endpoint of DIDs
    * to trigger the update of Business Partner Data of all existing wallets
  
1. Role `delete_wallets` to remove a wallet

1. Role `view_wallet` requires the BPN of Caller and it can be used:
    * to get the Wallet of the related BPN
    * to get stored Verifiable Credentials of the related BPN
    * to validate any Verifiable Presentation

1. Role `update_wallet` requires the BPN of Caller and it can be used:
    * to issue Verifiable Credentials (The BPN of issuer will be checked)
    * to issue Verifiable Presentations (The BPN of holder will be checked)
    * to store Verifiable Credentials (The BPN of holder will be checked)
    * to trigger Business Partner Data update for its own BPN

Additionally a Token mapper can to be created under *Clients* &gt;
*ManagedIdentityWallets* &gt; *Mappers* &gt; *create* with the following
configuration (using as example `BPNL000000001`):

| Key                 | Value                     |
|---------------------|---------------------------|
| Name                | StaticBPN                 |
| Mapper Type         | Hardcoded claim           |
| Token Claim Name    | BPN                       |
| Claim value         | BPNL000000001             |
| Claim JSON Type     | String                    |
| Add to ID token     | OFF                       |
| Add to access token | ON                        |
| Add to userinfo     | OFF                       |
| includeInAccessTokenResponse.label | ON         | 

If you receive an error message, that the client secret is not valid, please go into
keycloak admin and within *Clients > Credentials* recreate the secret.


## Local docker deployment

First step is to create the distribution of the application:

```bash
./gradlew installDist
```

Next step is to build and tag the Docker image, replacing the
`<VERSION>` with the app version:

```
docker build -t managed-identity-wallets:<VERSION> .
```

Finally, start the image (please make sure that there are no quotes around the
values in the env file):

```
docker run --env-file .env.docker -p 8080:8080 managed-identity-wallets:<VERSION>
```

## Deployment on Kubernetes

*Work in progress*

# End Users

See OpenAPI documentation, which is automatically created from
the source and available on each deployment at the `/docs` endpoint
(e.g. locally at http://localhost:8080/docs). 


## Test Coverage

Jacoco is used to generate the coverage report. The report generation
and the coverage verification are automatically executed after tests.

The generated HTML report can be found under `jacoco-report/html/`

To generate the report run the command

```
./gradlew jacocoTestReport
```

To check the coverage run the command

```
./gradlew jacocoTestCoverageVerification
```

Currently the minimum is 80%

Files to be excluded from the coverage calculation can be set in
`gradle.properties` using a comma-separated list of files or directories
with possible wildcards as the value for the property `coverage_excludes`.
The files in `models` and `entities` should be excluded as long as they
don't have any logic. The services that are mocked in unit tests must be
excluded. Also their interfaces need to be excluded because they have a
`companion object` that is used to create those services. Files like
`Application.kt` which are tested or simulated indirectly for example
using `withTestApplication` should also be excluded.
