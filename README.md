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
docker build -t catena-x/custodian:0.0.1 .
```

Finally, start the image:

```
docker run -p 8080:8080 catena-x/custodian:0.0.1
```

## Environment variable setup

Please see the file `.env.example` for the environment examples that are used
below. Here a few hints on how to set it up:

1. `CX_RG`: please enter your resource group name
2. `CX_AKS`: please enter your kubernetes service name
3. `CX_ACR_SERVER`: get the login server of your registry e.g. via `az acr list -o table`
4. `CX_ACR_USER`: get the username of your registry e.g.
   via `az acr credential show --name $CX_ACR_NAME --query "username" -o table `
5. `CX_ACR_PASSWORD`: get the password of the registry
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