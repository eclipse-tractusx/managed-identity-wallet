# Managed Identity Wallets

The Managed Identity Wallets (MIW) service implements the Self-Sovereign-Identity (SSI) using `did:web`.

# Usage

See [INSTALL.md](INSTALL.md) 

# Developer Documentation

To run MIW locally, this section describes the tooling as well as the local development setup.

There are two possible flows, which can be used for development:

1. **local**: Run the postgresql and keycloak server inside docker. Start MIW from within your IDE (recommended for
   actual development)
2. **docker**: Run everything inside docker (use to test or check behavior inside a docker environment)

## Tooling

Following tools the MIW development team used successfully:

| Area     | Tool     | Download Link                                   | Comment                                                                                          |
|----------|----------|-------------------------------------------------|--------------------------------------------------------------------------------------------------|
| IDE      | IntelliJ | https://www.jetbrains.com/idea/download/        | Use[envfile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) to use the **local** flow |
| Build    | Gradle   | https://gradle.org/install/                     |                                                                                                  |
| Runtime  | Docker   | https://www.docker.com/products/docker-desktop/ |                                                                                                  |
| Database | DBeaver  | https://dbeaver.io/                             |                                                                                                  |
| IAM      | Keycloak | https://www.keycloak.org/                       |                                                                                                  |

## Eclipse Dash Tool

[Eclipse Dash Homepage](https://projects.eclipse.org/projects/technology.dash)

The Eclipse Dash tool is used to analyze the dependencies used in the project and ensure all legal requirements are met.
We've added a gradle tasks to download the latest version of Dash locally, resolve all project dependencies and then run
the tool and update the summary in the DEPENDENCIES file.

To run the license check:

```bash
./gradlew dashLicenseCheck
```

To clean all files created by the dash tasks:

```bash
./gradlew dashClean
```

This command will output all dependencies, save the to file `deps.txt`. Dash will read from the file and update the
summary in the `DEPENDENCIES` file. A committer can open and issue to resolve any problems with the dependencies.

# Administrator Documentation

## Manual Keycloak Configuration

Within the development setup the Keycloak instance is initially prepared with the values
in `./dev-assets/docker-environment/keycloak`. The realm could also be manually added and configured
at http://localhost:8080 via the "Add realm" button. It can be for example named `localkeycloak`. Also add an additional
client, e.g. named `miw_private_client` with *valid redirect url* set to `http://localhost:8080/*`. The roles

- add_wallets
- view_wallets
- update_wallets
- delete_wallets
- view_wallet
- update_wallet
- manage_app

Roles can be added under *Clients > miw_private_client > Roles* and then assigned to the client using *Clients >
miw_private_client > Client Scopes* *> Service Account Roles > Client Roles > miw_private_client*.

The available scopes/roles are:

1. Role `add_wallets` to create a new wallet
2. Role `view_wallets`:
    - to get a list of all wallets
    - to retrieve one wallet by its identifier
    - to validate a Verifiable Credential
    - to validate a Verifiable Presentation
    - to get all stored Verifiable Credentials
3. Role `update_wallets` for the following actions:
    - to store Verifiable Credential
    - to issue a Verifiable Credential
    - to issue a Verifiable Presentation
4. Role `update_wallet`:
    - to remove a Verifiable Credential
    - to store a Verifiable Credential
    - to issue a Verifiable Credential
    - to issue a Verifiable Presentation
5. Role `view_wallet` requires the BPN of Caller and it can be used:
    - to get the Wallet of the related BPN
    - to get stored Verifiable Credentials of the related BPN
    - to validate any Verifiable Credential
    - to validate any Verifiable Presentation
6. Role `manage_app` used to change the log level of the application at runtime. Check Logging in the application
   section for more details

Overview by Endpoint

| Artefact                                  | CRUD   | HTTP Verb/ Request | Endpoint                              | Roles                                        | Constraints                                                |
|-------------------------------------------|--------|--------------------|---------------------------------------|----------------------------------------------|------------------------------------------------------------|
| **Wallets**                               | Read   | GET                | /api/wallets                          | **view_wallets**                             |                                                            |
| **Wallets**                               | Create | POST               | /api/wallets                          | **add_wallets**                              | **1 BPN : 1 WALLET**(PER ONE [1] BPN ONLY ONE [1] WALLET!) |
| **Wallets**                               | Create | POST               | /api/wallets/{identifier}/credentials | **update_wallets** <br />OR**update_wallet** |                                                            |
| **Wallets**                               | Read   | GET                | /api/wallets/{identifier}             | **view_wallets** OR<br />**view_wallet**     |                                                            |
| **Verifiable Presentations - Generation** | Create | POST               | /api/presentation                     | **update_wallets** OR<br />**update_wallet** |                                                            |
| **Verifiable Presentations - Validation** | Create | POST               | /api/presentations/validation         | **view_wallets** OR<br />**view_wallet**     |                                                            |
| **Verifiable Credential - Holder**        | Read   | GET                | /api/credentials                      | **view_wallets** OR<br />**view_wallet**     |                                                            |
| **Verifiable Credential - Holder**        | Create | POST               | /api/credentials                      | **update_wallet** OR<br />**update_wallet**  |                                                            |
| **Verifiable Credential - Holder**        | Delete | DELETE             | /api/credentials                      | **update_wallet**                            |                                                            |
| **Verfiable Credential - Validation**     | Create | POST               | /api/credentials/validation           | **view_wallets** OR<br />**view_wallet**     |                                                            |
| **Verfiable Credential - Issuer**         | Read   | GET                | /api/credentials/issuer               | **view_wallets**                             |                                                            |
| **Verfiable Credential - Issuer**         | Create | POST               | /api/credentials/issuer               | **update_wallets**                           |                                                            |
| **Verfiable Credential - Issuer**         | Create | POST               | /api/credentials/issuer/membership    | **update_wallets**                           |                                                            |
| **Verfiable Credential - Issuer**         | Create | POST               | /api/credentials/issuer/framework     | **update_wallets**                           |                                                            |
| **Verfiable Credential - Issuer**         | Create | POST               | /api/credentials/issuer/distmantler   | **update_wallets**                           |                                                            |
| **DIDDocument**                           | Read   | GET                | /{bpn}/did.json                       | N/A                                          |                                                            |
| **DIDDocument**                           | Read   | GET                | /api/didDocuments/{identifier}        | N/A                                          |                                                            |

Additionally, a Token mapper can be created under *Clients* &gt; *ManagedIdentityWallets* &gt; *Mappers* &gt; *create*
with the following configuration (using as an example `BPNL000000001`):

| Key                                | Value           |
|------------------------------------|-----------------|
| Name                               | StaticBPN       |
| Mapper Type                        | Hardcoded claim |
| Token Claim Name                   | BPN             |
| Claim value                        | BPNL000000001   |
| Claim JSON Type                    | String          |
| Add to ID token                    | OFF             |
| Add to access token                | ON              |
| Add to userinfo                    | OFF             |
| includeInAccessTokenResponse.label | ON              |

If you receive an error message that the client secret is not valid, please go into keycloak admin and within *Clients >
Credentials* recreate the secret.

## Development Setup

NOTE: The MIW requires access to the internet in order to validate the JSON-LD schema of DID documents.

### Prerequisites

To simplify the dev environment, [Taskfile](https://taskfile.dev) is used as a task executor. You have to install it
first.

> **IMPORTANT**: Before executing any of th tasks, you have to choose your flow (*local* or *docker*). *local* is
> default. To change that, you need to edit the variable **ENV** in the *Taskfile.yaml*. (see below)

After that, run `task check-prereqs` to see, if any other required tool is installed or missing. If something is
missing, a link to the install docs is provided.

Now, you have to adjust the *env* files (located in *dev-assets/env-files*). To do that, copy every file to the same
directory, but without ".dist" at the end.

Description of the env files:

- **env.local**: Set up everything to get ready for flow "local". You need to fill in the passwords. 
- **env.docker**: Set up everything to get ready for flow "docker". You need to fill in the passwords.

> **IMPORTANT**: ssi-lib is resolving DID documents over the network. There are two endpoints that rely on this resolution:
> - Verifiable Credentials - Validation
> - Verifiable Presentations - Validation
>   
> The following parameters are set in env.local or env.docker file per default:
> ENFORCE_HTTPS_IN_DID_RESOLUTION=false
> MIW_HOST_NAME=localhost
> APPLICATION_PORT=80
> If you intend to change them, the DID resolving may not work properly anymore!

> **IMPORTANT**: When you are using macOS and the MIW docker container won't start up (stuck somewhere or doesn't start
> at all), you can enable the docker-desktop feature "Use Rosetta for x86/amd64 emulation on Apple Silicon" in your
> Docker settings (under "features in development"). This should fix the issue.

Note: *SKIP_GRADLE_TASKS_PARAM* is used to pass parameters to the build process of the MIW jar. Currently, it skips the
tests and code coverage, but speeds up the build time. If you want to activate it, just comment it out
like `SKIP_GRADLE_TASKS_PARAM="" #"-x jacocoTestCoverageVerification -x test"`

After every execution (either *local* or *docker* flow), run the matching "stop" task (
e.g.: `task docker:start-app` -> `task docker:stop-app`)

When you just run `task` without parameters, you will see all tasks available.

### local

1. Run `task docker:start-middleware` and wait until it shows "(main) Running the server in development mode. DO NOT use
   this configuration in production." in the terminal
2. Run `task app:build` to build the MIW application
3. Run
   [ManagedIdentityWalletsApplication.java](src/main/java/org/eclipse/tractusx/managedidentitywallets/ManagedIdentityWalletsApplication.java)
   via IDE and use the local.env file to populate environment vars (e.g. EnvFile plugin for IntelliJ)
4. Run `task app:get-token` and copy the token (including "BEARER" prefix) (Mac users have the token already in their
   clipboard)
5. Open API doc on http://localhost:8000 (or what port you configured in the *env.local* file)
6. Click on Authorize on swagger UI and on the dialog paste the token into the "value" input
7. Click on "Authorize" and "close"
8. MIW is up and running

### docker

1. Run `task docker:start-app` and wait until it shows "Started ManagedIdentityWalletsApplication in ... seconds"
2. Run `task app:get-token` and copy the token (including "BEARER" prefix) (Mac users have the token already in their
   clipboard)
3. Open API doc on http://localhost:8000 (or what port you configured in the *env.local* file)
4. Click on Authorize on swagger UI and on the dialog paste the token into the "value" input
5. Click on "Authorize" and "close"
6. MIW is up and running

### pgAdmin

This local environment contains [pgAdmin](https://www.pgadmin.org/), which is also started (default: http://localhost:8888). 
The default login is:

``` 
user: pg@admin.com (you can change it in the env.* files)
password: the one you set for "POSTGRES_PASSWORD" in the env.* files
```

#### DB connection password

When you log in into pgAdmin, the local Postgresql server is already configured.
But you will be asked to enter the DB password on the first time you connect to the DB.
(password: POSTGRES_PASSWORD in the env.* files)

#### Storage folder

The storage folder of pgAdmin is mounted to `dev-assets/docker-environment/pgAdmin/storage/`.
For example, You can save DB backups there, so you can access them on your local machine.

# End Users

See OpenAPI documentation, which is automatically created from the source and available on each deployment at
the `/docs/api-docs/docs` endpoint (e.g. locally at http://localhost:8087/docs/api-docs/docs). An export of the JSON
document can be also found in [docs/openapi_v001.json](docs/openapi_v001.json).

# Test Coverage

Jacoco is used to generate the coverage report. The report generation and the coverage verification are automatically
executed after tests.

The generated HTML report can be found under `jacoco-report/html/`

To generate the report run the command:

```
task app:test-report
```

To check the coverage run the command:

```
task app:coverage
```

Currently, the minimum is 80% coverage.

# Common issues and solutions during local setup

## 1. Can not build with test cases

Test cases are written using the Spring Boot integration test frameworks. These test frameworks start the Spring Boot
test context, which allows us to perform integration testing. In our tests, we utilize the Testcontainers
library (https://java.testcontainers.org/) for managing Docker containers. Specifically, we use Testcontainers to start
PostgreSQL and Keycloak Docker containers locally.

Before running the tests, please ensure that you have Docker runtime installed and that you have the necessary
permissions to run containers.

Alternative, you can skip test during the build with `` ./gradlew clean build -x test``

## 2. Database migration related issue

We have implemented database migration using Liquibase (https://www.liquibase.org/). Liquibase allows us to manage
database schema changes effectively.

In case you encounter any database-related issues, you can resolve them by following these steps:

1. Delete all tables from the database.
2. Restart the application.
3. Upon restart, the application will recreate the database schema from scratch.

This process ensures that any issues with the database schema are resolved by recreating it in a fresh state.

# Environment Variables

| name                            | description                                                                                  | default value                                                                                                                                       |
|---------------------------------|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| APPLICATION_PORT                | port number of application                                                                   | 8080                                                                                                                                                |
| APPLICATION_ENVIRONMENT         | Environment of the application ie. local, dev, int and prod                                  | local                                                                                                                                               |
| DB_HOST                         | Database host                                                                                | localhost                                                                                                                                           |
| DB_PORT                         | Port of database                                                                             | 5432                                                                                                                                                |
| DB_NAME                         | Database name                                                                                | miw                                                                                                                                                 |
| USE_SSL                         | Whether SSL is enabled in database server                                                    | false                                                                                                                                               |
| DB_USER_NAME                    | Database username                                                                            |                                                                                                                                                     |
| DB_PASSWORD                     | Database password                                                                            |                                                                                                                                                     |
| DB_POOL_SIZE                    | Max number of database connection acquired by application                                    | 10                                                                                                                                                  |
| KEYCLOAK_MIW_PUBLIC_CLIENT      | Only needed if we want enable login with keyalock in swagger                                 | miw_public                                                                                                                                          |
| MANAGEMENT_PORT                 | Spring actuator port                                                                         | 8090                                                                                                                                                |
| MIW_HOST_NAME                   | Application host name, this will be used in creation of did ie. did:web:MIW_HOST_NAME:BPN    | localhost                                                                                                                                           |
| ENCRYPTION_KEY                  | encryption key used to encrypt and decrypt private and public key of wallet                  |                                                                                                                                                     |
| AUTHORITY_WALLET_BPN            | base wallet BPN number                                                                       | BPNL000000000000                                                                                                                                    |
| AUTHORITY_WALLET_NAME           | Base wallet name                                                                             | Catena-X                                                                                                                                            |
| AUTHORITY_WALLET_DID            | Base wallet web did                                                                          | web:did:host:BPNL000000000000                                                                                                                       |
| VC_SCHEMA_LINK                  | Comma separated list of VC schema URL                                                        | https://www.w3.org/2018/credentials/v1, https://catenax-ng.github.io/product-core-schemas/businessPartnerData.json                                  |
| VC_EXPIRY_DATE                  | Expiry date of VC (dd-MM-yyyy ie. 01-01-2025 expiry date will be 2024-12-31T18:30:00Z in VC) | 01-01-2025                                                                                                                                          |
| KEYCLOAK_REALM                  | Realm name of keycloak                                                                       | miw_test                                                                                                                                            |
| KEYCLOAK_CLIENT_ID              | Keycloak private client id                                                                   |                                                                                                                                                     |
| AUTH_SERVER_URL                 | Keycloak server url                                                                          |                                                                                                                                                     |
| SUPPORTED_FRAMEWORK_VC_TYPES    | Supported framework VC, provide values ie type1=value1,type2=value2                          | cx-behavior-twin=Behavior Twin,cx-pcf=PCF,cx-quality=Quality,cx-resiliency=Resiliency,cx-sustainability=Sustainability,cx-traceability=ID_3.0_Trace |
| ENFORCE_HTTPS_IN_DID_RESOLUTION | Enforce https during web did resolution                                                      | true                                                                                                                                                |
| CONTRACT_TEMPLATES_URL          | Contract templates URL used in summary VC                                                    | https://public.catena-x.org/contracts/                                                                                                              |
| APP_LOG_LEVEL                   | Log level of application                                                                     | INFO                                                                                                                                                |
|                                 |                                                                                              |                                                                                                                                                     |

# Technical Debts and Known issue

1. Keys are stored in database in encrypted format, need to store keys in more secure place ie. Vault
2. Policies can be validated dynamically as per request while validating VP and
   VC. [Check this for more details](https://docs.walt.id/v/ssikit/concepts/verification-policies)

# Logging in application

Log level in application can be set using environment variable ``APP_LOG_LEVEL``. Possible values
are ``OFF, ERROR, WARN, INFO, DEBUG, TRACE`` and default value set to ``INFO``

## Change log level at runtime using Spring actuator

We can use ``/actuator/loggers`` API endpoint of actuator for log related things. This end point can be accessible with
role ``manage_app``. We can add this role to authority wallet client using keycloak as below:

![manage_app.png](docs%2Fmanage_app.png)

1. API to get current log settings
   ```bash
   curl --location 'http://localhost:8090/actuator/loggers' \
   --header 'Authorization: Bearer access_token'
   ```
2. Change log level at runtime
   ```bash
   curl --location 'http://localhost:8090/actuator/loggers/{java package name}' \
   --header 'Content-Type: application/json' \
   --header 'Authorization: Bearer access_token' \
   --data '{"configuredLevel":"INFO"}'
   ```
   i.e.
   ```bash
   curl --location 'http://localhost:8090/actuator/loggers/org.eclipse.tractusx.managedidentitywallets' \
   --header 'Content-Type: application/json' \
   --header 'Authorization: Bearer access_token' \
   --data '{"configuredLevel":"INFO"}'
   ```

## Reference of external lib

1. https://www.testcontainers.org/modules/databases/postgres/
2. https://github.com/dasniko/testcontainers-keycloak
3. https://github.com/smartSenseSolutions/smartsense-java-commons
4. https://github.com/catenax-ng/product-lab-ssi

## Notice for Docker image

See [Docker-hub-notice.md](./Docker-hub-notice.md)

## Acknowledgments

We would like to give credit to these projects, which we use in our project.

[![semantic-release: angular](https://img.shields.io/badge/semantic--release-angular-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release)
