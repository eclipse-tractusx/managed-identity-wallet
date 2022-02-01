# Catena-X Core Custodian

This repository is part of the overarching Catena-X project, and more specifically
developed within the Catena-X Core Agile Release Train.

The Custodian implements the Self-Sovereign-Identity (SSI) readiness by providing
a wallet hosting platform including a DID resolver, service endpoints and the
company wallets itself.

Technically this project is developed using the [ktor](https://ktor.io) Microservices
framework and thus the Kotlin language. It is using [gradle](https://gradle.org/) as
build system.

## Building with gradle

To install gradle just follow [the official guide](https://gradle.org/install/), e.g. on MacOS homebrew can be used:

```
brew install gradle
```

Building then works with

```
gradle build
```

Or, as we also use gradle in the CI/CD pipeline, the gradle wrapper can be used

```
./gradlew build
```

In the following the `gradle` commands are using the gradle wrapper `gradlew`.

## Running locally with gradle

```
./gradlew run
```

## Building and running the Docker image

Based on the [official documentation](https://ktor.io/docs/docker.html#getting-the-application-ready)
below the steps to build and run this service via Docker.

First step is to create the distribution of the application (in this case using Gradle):

```
./gradlew installDist
```

Next step is to build and tag the Docker image:

```
docker build -t catena-x/custodian:0.0.4 .
```

Finally, start the image (please make sure that there are no quotes around the
values in the env file):

```
docker run --env-file .env.docker -p 8080:8080 catena-x/custodian:0.0.4
```

## Environment variable setup

Please see the file `.env.example` for the environment examples that are used
below. Here a few hints on how to set it up:

1. `CX_DB_JDBC_URL`: enter the database url, default is `jdbc:h2:mem:custodiandev;DB_CLOSE_DELAY=-1;`
2. `CX_DB_JDBC_DRIVER`: enter the driver, default is `org.h2.Driver`
3. `CX_AUTH_JWKS_URL`: enter the keycloak certs url, e.g. `http://localhost:8081/auth/realms/catenax/protocol/openid-connect/certs`
4. `CX_AUTH_ISSUER_URL`: enter the token issue, e.g. `http://localhost:8081/auth/realms/catenax`
5. `CX_AUTH_REALM`: specify the realm, e.g. `catenax`
6. `CX_AUTH_ROLE`: specify the expected role within the token, e.g. `access`
7. `CX_AUTH_CLIENT_ID`: specify the expected client id, e.g. `custodian`
8. `CX_DATAPOOL_URL`: specify the data pool API endpoint, e.g. `http://catenax-bpdm-dev.germanywestcentral.cloudapp.azure.com:8080`

To follow all steps in this readme you also need following variables:

1. `CX_SUBSCRIPTION_ID`: enter your Azure subscription id
2. `CX_CLIENT_ID`: enter your Azure client id
3. `CX_RG`: enter your resource group name
4. `CX_IP_NAME`: enter the static ip resource name
5. `CX_IP`: enter the actual IP of the static ip reresource
6. `CX_LB_SERVICE` enter the load balance service name in kubernetes
7. `CX_AKS`: please enter your kubernetes service name
8. `CX_ACR_SERVER`: get the login server of your registry e.g. via `az acr list -o table`
9. `CX_ACR_USER`: get the username of your registry e.g.
   via `az acr credential show --name $CX_ACR_NAME --query "username" -o table `
10. `CX_ACR_PASSWORD`: get the password of the registry
   via `az acr credential show --name $CX_ACR_NAME --query "passwords[0].value" -o table`

## Local development environment

To resemble the staging and production system as much as possible also on the
local machine, an external Postgresql database should be used instead of
the default included h2 in-memory database. This can be done using the default
Docker image for Postgresql:

```
docker run --name cx_postgres -e POSTGRES_PASSWORD=cx_password -p 5432:5432 -d postgres
```

Please see the section below setting up the database.

Additionally the authentication and authorization is done via
[keycloak](https://www.keycloak.org). A keycloak instance can be started via

```
docker run -d --name catenax_keycloak -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=catena -p 8081:8080 jboss/keycloak
```

To make it work a new realm needs to be configured at http://localhost:8081
and there via the "Add realm" button, it can be for example named `catenax`.
Also add an additional client, e.g. named `Custodian` with *valid redirect url*
set to `http://localhost:8080/*`. A role, e.g. named `custodian-api` and a user,
e.g. named `custodian-admin`, need to be created as well (including setting
a password, e.g. `catena-x`). The user also needs to have a specific client role
assigned, e.g. `access`, which is validated on access time.

The instructions were taken from [this medium blog post](https://medium.com/slickteam/ktor-and-keycloak-authentication-with-openid-ecd415d7a62e).
You can also import the configuration from `examples/

## Testing GitHub actions locally

Using [act](https://github.com/nektos/act) it is possible to test GitHub actions
locally. To run it needs a secrets file, which could be derived on `.env.example`,
see the section above.

```
act --secret-file .env
```

## Manually deploy the to Azure Kubernetes Service (AKS)

First install the Azure command line like in
[the official guide](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli),
e.g. on MacOS homebrew can be used:

```
brew update && brew install azure-cli
```

Make sure you have also kubectl installed

```
az aks install-cli
```

### Login to AKS

Initial the usage can be prepared via following commands:

```
set -a; source .env; set +a
docker login $CX_ACR_SERVER
az aks get-credentials --resource-group $CX_RG --name $CX_AKS_CLUSTER --admin
```

### Push the image to the ACR

Make sure that the latest image is pushed to the registry:

```
docker tag catena-x/custodian:0.0.1 $CX_ACR_SERVER/catena-x/custodian:0.0.1 
docker push $CX_ACR_SERVER/catena-x/custodian:0.0.1 
```

### Verify kubernetes is functional

Verify that nodes are available

```
kubectl get nodes
```

### Setup up static IP / DNS name

To use a static IP and dns name for the service, follow the step of the
official [documentation](https://docs.microsoft.com/en-us/azure/aks/static-ip)

```
az network public-ip create \
    --resource-group $CX_RG \
    --name $CX_IP \
    --sku Standard \
    --allocation-method static
```

You can view the generated IP with

```
az network public-ip show --resource-group $CX_RG --name $CX_IP --query ipAddress --output tsv
```

Make sure that the roles are assigned correctly

```
az role assignment create \
    --assignee $CX_CLIENT_ID \
    --role "Network Contributor" \
    --scope /subscriptions/$CX_SUBSCRIPTION_ID/resourceGroups/$CX_RG
````

And then create the service with

```
kubectl apply -f deployment/aks-custodian-lb-dev.yaml
```

Which makes it accessible at `<location>.cloudapp.azure.com`.

To troubleshoot look into the service via

```
kubectl describe service $CX_LB_SERVICE
```

## Setting up progresql database

Based on the [documentation](https://docs.microsoft.com/en-us/azure/postgresql/howto-create-users)
provided by Mirosoft following SQL needs to be executed to setup initiall the database:

```
CREATE DATABASE custodiandev;
CREATE ROLE custodiandevuser WITH LOGIN NOSUPERUSER INHERIT CREATEDB NOCREATEROLE NOREPLICATION PASSWORD '^cXnF61qM1kf';
GRANT CONNECT ON DATABASE custodiandev TO custodiandevuser;
```

Then following environment settings in your local environment file (potentially
named `.env`) can be used:

```
CX_DB_JDBC_URL="jdbc:postgresql://localhost:5432/custodiandev?user=custodiandevuser&password=^cXnF61qM1kf"
CX_DB_JDBC_DRIVER="org.postgresql.Driver"
```

Currently the ORM Exposed is creating the tables if they don't exist yet, done
within the `Persistence.kt` database setup:

```
SchemaUtils.createMissingTablesAndColumns(Companies, Wallets, VerifiableCredentials)
```

## Tools

To access databases [DBeaver](https://dbeaver.io/) is really useful as it has a
graphical interface but also excellent SQL support.

## Future

Potentially following libraries and frameworks could be added in future

* [HikariCP](https://github.com/brettwooldridge/HikariCP) for connection pooling
* [Koin](https://github.com/InsertKoinIO/koin) for dependency injection