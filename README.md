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

Following tools the MIW development team used successfully:

| Area        | Tool               | Download Link    | Comment     |
|-------------|--------------------|------------------|-------------|
| IDE         | IntelliJ           | https://www.jetbrains.com/idea/download/ | Additionally the [envfile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) is suggested |
|             | Visual Studio Code | https://code.visualstudio.com/download | Test with version 1.71.2, additionally Git, Kotlin, Kubernetes plugins are suggested |
| Build       | Gradle             | https://gradle.org/install/ | Tested with version 7.3.3 |
| Runtime     | Docker Desktop     | https://www.docker.com/products/docker-desktop/ | |
|             | Rancher Desktop    | https://rancherdesktop.io | Tested with version 1.5.1, and Docker cli version `Docker version 20.10.17-rd, build c2e4e01` and Docker Compose cli version `Docker Compose version v2.6.1` |
| API Testing | Postman            | https://www.postman.com/downloads/ | Tested with version 9.31.0 |
| Database    | DBeaver            | https://dbeaver.io/ | Tested with version 22.2.0.202209051344 |

## Environment Variables <a id= "environmentVariables"></a>

Please see the file `.env.example` for the environment examples that are used
below. Here a few hints on how to set it up:

| Key                       | Type   | Description                                                                                                                                             |
|---------------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MIW_DB_JDBC_URL`          | URL    | database connection string, most commonly postgreSQL is used                                                                                            |
| `MIW_DB_JDBC_DRIVER`       | URL    | database driver to use, most commonly postgreSQL is used                                                                                                |
| `MIW_AUTH_JWKS_URL`        | URL    | IAM certs url                                                                                                                                           |
| `MIW_AUTH_ISSUER_URL`      | URL    | IAM token issuer url                                                                                                                                    |
| `MIW_AUTH_REDIRECT_URL`    | URL    | IAM redirect url to the MIW                                                                                                                             |
| `MIW_AUTH_REALM`           | String | IAM realm                                                                                                                                               |
| `MIW_AUTH_ROLE_MAPPINGS`   | String | IAM role mapping                                                                                                                                        |
| `MIW_AUTH_RESOURCE_ID`     | String | IAM resource id                                                                                                                                         |
| `MIW_AUTH_CLIENT_ID`       | String | IAM client id                                                                                                                                           |
| `MIW_AUTH_CLIENT_SECRET`   | String | It can be extracted from keycloak under *realms* &gt;*localkeycloak* &gt; *clients* &gt; *ManagedIdentityWallets* &gt; *credentials*                    |
| `APP_VERSION`             | String | application version, this should be in-line with the version in the deployment                                                                          |
| `LOG_LEVEL_KTOR_ROOT`     | String | the log level of Ktor                                                                                                                                   |
| `LOG_LEVEL_NETTY`     | String | the log level of used netty server                                                                                                                      |
| `LOG_LEVEL_ECLIPSE_JETTY`     | String | the log level of used eclipse jetty                                                                                                                     |
| `LOG_LEVEL_EXPOSED`     | String | the log level of used exposed framework                                                                                                                 |
| `LOG_LEVEL_SERVICES_CALLS`     | String | the log level of http client used in internal services. OPTIONS: ALL, HEADERS, BODY, INFO, NONE                                                         |
| `MIW_BPN`                  | String | BPN of the base wallet                                                                                                                                  |
| `MIW_DID`                  | String | DID of the base wallet, this wallet must be registered on ledger with the endorser role                                                                 |
| `MIW_VERKEY`               | String | Verification key of the base wallet, this wallet must be registered on ledger with the endorser role                                                    |
| `MIW_NAME`                 | String | Name of the base wallet                                                                                                                                 |
|`MIW_ALLOWLIST_DIDS`       | String | List of full DIDs seperated by comma ",". Those DIDs are allowed to send a connection request to managed wallets. Empty for public invitation allowance |
| `MIW_MEMBERSHIP_ORG`  | String | The name used in the Membership credential                                                                                                              |

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
