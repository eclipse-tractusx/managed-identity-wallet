# Managed Identity Wallets <a id="introduction"></a>

This repository is part of the overarching Catena-X project, and more specifically
developed within the Catena-X Core Agile Release Train.

The Managed Identity Wallets (MIW) service implements the Self-Sovereign-Identity (SSI)
readiness by providing a wallet hosting platform including a DID resolver,
service endpoints and the company wallets itself.

Technically this project is developed using the [ktor](https://ktor.io) Microservices
framework and thus the Kotlin language. It is using [gradle](https://gradle.org/) as
build system. To store the wallets and communicate with an external ledger MIW is using
[Aries Cloud Agent Python](https://github.com/hyperledger/aries-cloudagent-python) with
it's [multi-tenant feature](https://github.com/hyperledger/aries-cloudagent-python/blob/main/Multitenancy.md)
and [JSON-LD credential](https://github.com/hyperledger/aries-cloudagent-python/blob/main/JsonLdCredentials.md)
To support credential revocation MIW is using the revocation service within the
[GXFS Notarization API/Service](https://gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service/-/tree/main/services/revocation)

> **Warning**
> This is not yet ready for production usage, as
> [Aries Cloud Agent Python](https://github.com/hyperledger/aries-cloudagent-python)
> does not support `did:indy` resolution yet. This disclaimer will be removed,
> once it is available.

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

| Key                       | Type   | Description |
|---------------------------|--------|-------------|
| `CX_DB_JDBC_URL`          | URL    | database connection string, most commonly postgreSQL is used |
| `CX_DB_JDBC_DRIVER`       | URL    | database driver to use, most commonly postgreSQL is used |
| `CX_AUTH_JWKS_URL`        | URL    | IAM certs url |
| `CX_AUTH_ISSUER_URL`      | URL    | IAM token issuer url |
| `CX_AUTH_REALM`           | String | IAM realm |
| `CX_AUTH_ROLE_MAPPINGS`   | String | IAM role mapping |
| `CX_AUTH_RESOURCE_ID`     | String | IAM resource id |
| `CX_AUTH_CLIENT_ID`       | String | IAM client id |
| `CX_AUTH_CLIENT_SECRET`   | String | It can be extracted from keycloak under *realms* &gt;*catenax* &gt; *clients* &gt; *ManagedIdentityWallets* &gt; *credentials* |
| `APP_VERSION`             | String | application version, this should be in-line with the version in the deployment |
| `ACAPY_API_ADMIN_URL`     | String | admin url of ACA-Py |
| `ACAPY_ADMIN_API_KEY`     | String | admin api key of ACA-Py endpoints |
| `ACAPY_BASE_WALLET_API_ADMIN_URL`     | String | admin url of the catena-x endorser ACA-Py |
| `ACAPY_BASE_WALLET_ADMIN_API_KEY`     | String | admin api key of the catena-x endorser ACA-Py endpoints |
| `ACAPY_NETWORK_IDENTIFIER`| String | Hyperledger Indy name space |
| `CX_BPN`                  | String | BPN of the catena-x wallet |
| `CX_DID`                  | String | DID of the catena-x wallet, this wallet must be registered on ledger with the endorser role |
| `CX_VERKEY`               | String | Verification key of the catena-x wallet, this wallet must be registered on ledger with the endorser role |
| `CX_NAME`                 | String | Name of the catena-x base wallet |
| `BPDM_DATAPOOL_URL`       | String | BPDM data pool API endpoint |
| `BPDM_AUTH_CLIENT_ID`     | String | client id for accessing the BPDM data pool endpoint |
| `BPDM_AUTH_CLIENT_SECRET` | String | client secret for accessing the BPDM data pool endpoint |
| `BPDM_AUTH_GRANT_TYPE`    | String | grant type for accessing the BPDM data pool endpoint |
| `BPDM_AUTH_SCOPE`         | String | openid scope for accessing the BPDM data pool endpoint |
| `BPDM_AUTH_URL`           | String | IAM url to get the access token for BPDM data pool endpoint |
| `BPDM_PULL_DATA_AT_HOUR`  | String | At which hour (24-hour clock) the cron job should pull the data from the BPDM data pool |
| `REVOCATION_URL`          | String | URL of the revocation service |
| `REVOCATION_CREATE_STATUS_LIST_CREDENTIAL_AT_HOUR` | String | At which hour (24-hour clock) the cron job should issue/update status-list credentials |

## Local Development Setup

To get a full development environment up (first with a in-memory database)
run following these steps:

1. Clone the GitHub repository

    ```bash
    git clone https://github.com/eclipse-tractusx/managed-identity-wallets.git
    cd managed-identity-wallets
    ```

1. Copy over the `.env.example` to `dev.env`


    ```bash
    cp .env.example dev.env
    ```

1. Start the supporting containers for postgreSQL (database), keycloak (identity
management), ACA-Py (ledger communication) and revocation service (credential
revocation handling)

    ```bash
    cd dev-assets/dev-containers
    docker compose up -d
    ```

    You can stop the containers via `docker compose down -v`

1. Run the MIW service from the project rootfolder via (on MacOS)

    ```bash
    cd ../../
    set -a; source dev.env; set +a
    ./gradlew run
    ```

    or respectively run `Application.kt` within in your IDE (using `dev.env` as configuration).

1. :tada: **First milestone reached the MIW service is up and running!**

    Suggested next step is to use the postgreSQL database to have persistent storage
    across starts, this can be done via changing following variables in `dev.env`
    (assuming the standard port for postgreSQL 5432 is available).

    | Key               | Value           |
    |-------------------|-----------------|
    | CX_DB_JDBC_URL    | `jdbc:postgresql://localhost:5432/miwdev?user=miwdevuser&password=cx_password` |
    | CX_DB_JDBC_DRIVER | `org.postgresql.Driver` |

    Then restart the service via `./gradlew run`

## Advanced Development Setup

With the following steps you can explore the API 

1. Start Postman and add the environment `Managed_Identity_Wallet_Local.postman_environment` and the collection `Managed_Identity_Wallet.postman_collection` from ./dev-assets/
    1. In the added environment make sure that the client_id and client_secret are the same as in your `dev.env` configuration.

    1. Issue Status-List Credential by sending a POST request to `/api/credentials/revocations/statusListCredentialRefresh`. This step is temporary until the update to Ktor 2.x

1. The two Postman collections `Cx_Base_Wallet_Acapy.postman_collection` and `Managed_Wallets_Acapy.postman_collection` are additional for debugging purposes. these collections include direct calls to the admin API of the Catena-X AcaPy instance and the Multi-tenancy AcaPy instance.

1. The Postman collection `Test-Acapy-SelfManagedWallet-Or-ExternalWallet.postman_collection` sends requests to the external AcaPy instance that simulate an external wallet or self managed wallet.

1. :tada: **Second milestone reached: Your own wallet!**

Now you have achieved the following:

* set up the development environment to run it from source
* initialized the catena-x wallet and its revocation list
* you can retrieve the list of wallets in Postman via the *Get wallets from Managed Identity Wallets*

## Setup Summary

| Service               | URL                     | Description |
|-----------------------|-------------------------|-------------|
| postgreSQL database   | port 5432 on `localhost`| within the Docker Compose setup |
| Keycloak              | http://localhost:8081/  | within the Docker Compose setup, username: `admin` and password: `catena`, client id: `ManagedIdentityWallets` and client secret can be found under the Clients &gt; ManagedIdentityWallets &gt; Credentials |
| revocation service    | http://localhost:8086   | within the Docker Compose setup |
| ACA-Py for Catena-X Endorser Wallet | http://localhost:10000  | within the Docker Compose setup |
| ACA-Py Multi-tenancy for Managed Wallets | http://localhost:10003  | within the Docker Compose setup |
| MIW service           | http://localhost:8080/  | |
| ACA-Py (External Wallet) | http://localhost:10001  | within the Docker Compose setup |

# Administrator Documentation

## Manual Keycloak Configuration

Within the development setup the Keycloak is initially prepared with the
values in `./dev-assets/dev-containers/keycloak`. The realm could also be
manually added and configured at http://localhost:8081 via the "Add realm"
button. It can be for example named `catenax`. Also add an additional client,
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

## Manual Database Configuration

Within the development setup databases are initially prepared as needed, in the
cloud deployment that is done via init containers. The MIW and the ACA-Py
service are setting up the required tables on the first start. For MIW this is
done within the `src/main/.../managedidentitywallets/plugins/Persistence.kt` database
setup:

```
SchemaUtils.createMissingTablesAndColumns(Wallets, VerifiableCredentials, SchedulerTasks)
```

The tables of the **Revocation Service** are added manually to the database using the
SQL script at `./dev-asset/dev-containers/revocation/V1.0.0__Create_DB.sql`

## Local docker deployment

First step is to create the distribution of the application:

```bash
./gradlew installDist
```

Next step is to build and tag the Docker image, replacing the
`<VERSION>` with the app version:

```
docker build -t catena-x/managed-identity-wallets:<VERSION> .
```

Finally, start the image (please make sure that there are no quotes around the
values in the env file):

```
docker run --env-file .env.docker -p 8080:8080 catena-x/managed-identity-wallets:<VERSION>
```

## Deployment on Kubernetes

*Work in progress*

1. Create a namespace

    Using as example `managed-identity-wallets`:

    ```
    kubectl create namespace managed-identity-wallets
    ```

1. Create relevant secrets

    Altogether four secrets are needed
    * catenax-managed-identity-wallets-secrets
    * catenax-managed-identity-wallets-acapy-secrets    
    * postgres-acapy-secret-config
    * postgres-managed-identity-wallets-secret-config

    Create these with following commands, after replacing the placeholders:

    ```
    kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-secrets \
      --from-literal=cx-db-jdbc-url='jdbc:postgresql://<placeholder>:5432/miwdev?user=miwdevuser&password=<placeholder>' \
      --from-literal=cx-auth-client-id='ManagedIdentityWallets' \
      --from-literal=cx-auth-client-secret='<placeholder>'

    kubectl -n managed-identity-wallets create secret generic catenax-managed-identity-wallets-acapy-secrets \
      --from-literal=acapy-endorser-wallet-key='<placeholder>' \
      --from-literal=acapy-endorser-agent-wallet-seed='<placeholder>' \
      --from-literal=acapy-endorser-jwt-secret='<placeholder>' \
      --from-literal=acapy-endorser-db-account='postgres' \
      --from-literal=acapy-endorser-db-password='<placeholder>' \
      --from-literal=acapy-endorser-db-admin='postgres' \
      --from-literal=acapy-endorser-db-admin-password='<placeholder>' \
      --from-literal=acapy-endorser-admin-api-key='<placeholder>' \
      --from-literal=acapy-mt-wallet-key='<placeholder>' \
      --from-literal=acapy-mt-agent-wallet-seed='<placeholder>' \
      --from-literal=acapy-mt-jwt-secret='<placeholder>' \
      --from-literal=acapy-mt-db-account='postgres' \
      --from-literal=acapy-mt-db-password='<placeholder>' \
      --from-literal=acapy-mt-db-admin='postgres' \
      --from-literal=acapy-mt-db-admin-password='<placeholder>' \
      --from-literal=acapy-mt-admin-api-key='<placeholder>'

    kubectl -n managed-identity-wallets create secret generic postgres-acapy-secret-config \
    --from-literal=password='<placeholder>' \
    --from-literal=postgres-password='<placeholder>' \
    --from-literal=user='postgres'

    kubectl -n managed-identity-wallets create secret generic postgres-managed-identity-wallets-secret-config \
    --from-literal=password='<placeholder>' \
    --from-literal=postgres-password='<placeholder>' \
    --from-literal=user='postgres'
    ```

1.  If the Indy ledger is write-restricted, the DID of the used seed
    must be registered manually before starting ACA-Py.

1. Install a new deployment via helm

    Run following command to use the base values as well as the predefined values for local deployment:

    ```
    helm install managed-identity-wallets ./helm/managed-identity-wallets/ -n managed-identity-wallets -f ./helm/managed-identity-wallets/values.yaml -f ./helm/managed-identity-wallets/values-local.yaml
    ```

4. Expose via loadbalancer

    ```
    kubectl -n managed-identity-wallets apply -f dev-assets/kube-local-lb.yaml
    ```

5. To check the current deployment and version run `helm list -n <namespace-placeholder>`. Example output:

    ```
    NAME         	NAMESPACE        	REVISION	UPDATED                                	STATUS  	CHART                  	                APP VERSION
    cx-miw       	ingress-miw     	1       	2022-02-24 08:51:39.864930557 +0000 UTC	deployed	catenax-managed-identity-wallets-0.1.0	0.0.5      
    ```

# End Users

See OpenAPI documentation, which is automatically created from
the source and available on each deployment at the `/docs` endpoint
(e.g. locally at http://localhost:8080/docs). An export of the JSON
document can be also found in [docs/openapi_v200.json](docs/openapi_v200.json).

# Further Guides

In this section there are advanced cases (e.g. building your own ACA-Py image)
described.

## Preparation of ACA-Py Docker Image <a id= "acapyDockerImage"></a>

ACA-Py can be used via the official image at `bcgovimages/aries-cloudagent:py36-1.16-1_0.7.5`
or build your own image following the steps:
* clone the repository `git clone https://github.com/hyperledger/aries-cloudagent-python.git`
* navigate to the repository `cd aries-cloudagent-python`
* currently tested with version `0.7.5`
* run `git checkout 0.7.5`
* run `docker build -t acapy:0.7.5 -f ./docker/Dockerfile.run .`
* change the used image for `cx_acapy` in `dev-assets/dev-containers/docker-compose.yml`

## Integrate with an write-restricted Indy Ledger

If the used Indy ledger (see parameter `--genesis-url https://indy-test.idu.network/genesis`)
is write-restricted to endorsers or higher roles, the DID and its VerKey must be registered
manually before starting ACA-Py.

The [Indy CLI](https://hyperledger-indy.readthedocs.io/projects/sdk/en/latest/docs/design/001-cli/README.html)
in Docker using the [docker-file](https://github.com/hyperledger/indy-sdk/blob/main/cli/cli.dockerfile)
can be used to generate a new DID from a given seed. However, it does not show the
complete `VerKey`, check this [Issue](https://github.com/hyperledger/indy-sdk/issues/2553). 
Therefore, the easiest way to generate a DID is currently to start ACA-Py with a given seed.

  * Navigate to `./dev-assets/generate-did-from-seed`
  * Generate a random seed that has 32 characters. If the use of an offline secure secret/password
    generator is not possible, then these guidelines must be followed:
    * No repeat of characters or strings
    * No patterns or use of dictionary words
    * The use of uppercase and lowercase letters - as well as numbers and allowed symbols
    * No personal preferences like names or phone numbers
  * Set the seed as an enviroment variable e.g. `export SEED=312931k4h15989pqwpou129412i214dk`
  * Run the script generateDidFromSeed script with `./generateDidFromSeed.sh` which starts the
    ACA-Py container and shows the printout of the `DID` and `VerKey` from its logs in the console
    like the following
    ```
    2022-08-12 08:08:13,888 indy.did DEBUG get_my_did_with_meta: <<< res: '{"did":"HW2eFhr3KcZw5JcRW45KNc","verkey":"aEErMofs7DcJT636pocN2RiEHgTLoF4Mpj6heFXwtb3q","tempVerkey":null,"metadata":null}'
    ```
  * If the script did not stop the container, the command `docker compose down -v` can stop and delete it manually

## Testing GitHub actions locally <a id= "testingGitHubActionsLocally"></a>

Using [act](https://github.com/nektos/act) it is possible to test GitHub actions
locally. To run it needs a secrets file, which could be derived on `.env.example`,
see the section above.

```
act --secret-file .env
```

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


## Helm Documentation
The `./charts/README.md` is autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)

To regenerate the README.md after updating `values.yaml` or `Chart.yaml` run

```
helm-docs --sort-values-order file
```

## Dashboard <a id="dashboard"></a>

Within `ui-src` a simple Vue based dashboard application is available
which currently only shows the existing companies as well as is able
to retrieve the full BPN information from the CX data pool API on a
click on the BPN.

It can be developed with

```
cd ui-src
yarn serve
```

In each release the files in `/static` are updated but within the deployment
pipeline the application is built and copied over to the `/static` directory.

The steps to build the static files are like following:

```
cd ui-src
yarn build
rm -rf ../static/*
cp -r dist/* ../static
```
