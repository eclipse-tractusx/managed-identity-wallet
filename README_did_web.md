# Managed Identity Wallets <a id="introduction"></a>

The Managed Identity Wallets (MIW) service implements the Self-Sovereign-Identity (SSI) using did:web

# Developer Documentation

To run MIW locally, this section describes the tooling as well as
the local development setup.

## Tooling

Following tools the MIW development team used successfully:

| Area     | Tool     | Download Link                                   | Comment                                                                                           |
|----------|----------|-------------------------------------------------|---------------------------------------------------------------------------------------------------|
| IDE      | IntelliJ | https://www.jetbrains.com/idea/download/        | Additionally the [envfile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) is suggested |
| Build    | Gradle   | https://gradle.org/install/                     |
| Runtime  | Docker   | https://www.docker.com/products/docker-desktop/ |                                                                                                   |
| Database | DBeaver  | https://dbeaver.io/                             |
| IAM      | Keycloak | https://www.keycloak.org/                       |                                                                                                   |

## Local Development Setup

1. Run keycloak and database server using [docker-compose.yaml](dev-assets%2Fdid-web%2Fdocker-compose.yaml)
2. Connect database and create miw database and user to access the database
3. Update DB_USER_NAME, DB_PASSWORD, and DB_NAME in  [dev.env](dev-assets%2Fdid-web%2Fdev.env)
4. Create personal access token(classic) with `read:packages` access (ref: https://github.com/settings/tokens/new)
5. set ORG_GRADLE_PROJECT_githubUserName and ORG_GRADLE_PROJECT_githubToken values
   in [dev.env](dev-assets%2Fdid-web%2Fdev.env)
6. Setup [dev.env](dev-assets%2Fdid-web%2Fdev.env) values either in application.yaml or in IDE
7. Run [ManagedIdentityWalletsApplication.java](src%2Fmain%2Fjava%2Forg%2Feclipse%2Ftractusx%2Fmanagedidentitywallets%2FManagedIdentityWalletsApplication.java) in IDE
8. Open API doc on http://localhost:8080
9. Click on Authorize on swagger UI and on the dialog click again on Authorize.
10. Login with username=catena-x and password=password

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

## Environment Variables <a id= "environmentVariables"></a>

| name                         | description                                                                                   | default value                                                                                                                                       |
|------------------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| APPLICATION_PORT             | port number of application                                                                    | 8080                                                                                                                                                | 
| APPLICATION_ENVIRONMENT      | Environment of the application ie. local, dev, int and prod                                   | local                                                                                                                                               |
| DB_HOST                      | Database host                                                                                 | localhost                                                                                                                                           |
| DB_PORT                      | Port of database                                                                              | 5432                                                                                                                                                |
| DB_NAME                      | Database name                                                                                 | miw                                                                                                                                                 |
| USE_SSL                      | Whether SSL is enabled in database server                                                     | false                                                                                                                                               |
| DB_USER_NAME                 | Database username                                                                             |                                                                                                                                                     |
| DB_PASSWORD                  | Database password                                                                             |                                                                                                                                                     |
| DB_POOL_SIZE                 | Max number of database connection acquired by application                                     | 10                                                                                                                                                  |
| KEYCLOAK_MIW_PUBLIC_CLIENT   | Only needed if we want enable login with keyalock in swagger                                  | miw_public                                                                                                                                          |
| MANAGEMENT_PORT              | Spring actuator port                                                                          | 8090                                                                                                                                                |
| MIW_HOST_NAME                | Application host name, this will be used in creation of did ie. did:web:MIW_HOST_NAME:BPN     | localhost                                                                                                                                           |
| ENCRYPTION_KEY               | encryption key used to encrypt and decrypt private and public key of wallet                   |                                                                                                                                                     |
| AUTHORITY_WALLET_BPN         | base wallet BPN number                                                                        | BPNL000000000000                                                                                                                                    |
| AUTHORITY_WALLET_NAME        | Base wallet name                                                                              | Catena-X                                                                                                                                            |
| AUTHORITY_WALLET_DID         | Base wallet web did                                                                           | web:did:host:BPNL000000000000                                                                                                                       |
| VC_SCHEMA_LINK               | Comma separated list of VC schema URL                                                         | https://www.w3.org/2018/credentials/v1, https://raw.githubusercontent.com/catenax-ng/product-core-schemas/main/businessPartnerData                  |
| VC_EXPIRY_DATE               | Expiry date of VC (dd-MM-yyyy ie.  01-01-2025 expiry date will be 2024-12-31T18:30:00Z in VC) | 01-01-2025                                                                                                                                          |
| KEYCLOAK_REALM               | Realm name of keycloak                                                                        | miw_test                                                                                                                                            |
| KEYCLOAK_CLIENT_ID           | Keycloak private client id                                                                    |                                                                                                                                                     |
| AUTH_SERVER_URL              | Keycloak server url                                                                           |                                                                                                                                                     |
| SUPPORTED_FRAMEWORK_VC_TYPES | Supported framework VC, provide values ie type1=value1,type2=value2                           | cx-behavior-twin=Behavior Twin,cx-pcf=PCF,cx-quality=Quality,cx-resiliency=Resiliency,cx-sustainability=Sustainability,cx-traceability=ID_3.0_Trace |
|                              |                                                                                               |                                                                                                                                                     |

## Reference

1. https://www.testcontainers.org/modules/databases/postgres/
2. https://github.com/dasniko/testcontainers-keycloak
3. https://github.com/smartSenseSolutions/smartsense-java-commons
