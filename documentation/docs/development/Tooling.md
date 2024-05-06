---
title: Tools
sidebar_position: 5
tags: [ ]
---

# Tools

Following tools the MIW development team used successfully:

| Area       | Tool     | Download Link                                   | Comment                                                                                          |
|------------|----------|-------------------------------------------------|--------------------------------------------------------------------------------------------------|
| IDE        | IntelliJ | https://www.jetbrains.com/idea/download/        | Use[envfile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) to use the **local** flow |
| Build      | Gradle   | https://gradle.org/install/                     |                                                                                                  |
| Runtime    | Docker   | https://www.docker.com/products/docker-desktop/ |                                                                                                  |
| Database   | DBeaver  | https://dbeaver.io/                             |                                                                                                  |
| IAM        | Keycloak | https://www.keycloak.org/                       |                                                                                                  |
| Kubernetes | Helm     | https://helm.sh/docs/intro/install/             | Used for deployment in Kubernetes                                                                |

## Development Setup

### Prerequisites

To simplify the dev environment, [Taskfile](https://taskfile.dev) is used as a task executor. You have to install it
first.

> **IMPORTANT**: Before executing any of th tasks, you have to choose your flow (_local_ or _docker_). _local_ is
> default.
> To change that, you need to edit the variable **ENV** in the _Taskfile.yaml_. (see below)

After that, run `task check-prereqs` to see, if any other required tool is installed or missing. If something is
missing, a link to the install docs is provided.

Now, you have to adjust the _env_ files (located in _dev-assets/env-files_). To do that, copy every file to the same
directory, but without ".dist" at the end.

Description of the env files:

- **env.local**: Setup everything to get ready for flow "local". You need to fill in the passwords. Everything else can
  remain as it is.
- **env.docker**: Setup everything to get ready for flow "docker". You need to fill in the passwords. Everything else
  can remain as it is.

> **IMPORTANT**: When you are using MacOS and the MIW docker container won't start up (stuck somewhere or doesn't start
> at all), you can enable the docker-desktop feature "Use Rosetta for x86/amd64 emulation on Apple Silicon" in your
> Docker
> settings (under "features in development"). This should fix the issue.

In both env files (env.local and env.docker) you need to set _GITHUB_USERNAME_ and _GITHUB_TOKEN_ in order to be able to
build the app, because the SSI lib is stored in a private repo (you also need the proper rights to access the repo).
The access token need to have `read:packages` access. (ref: https://github.com/settings/tokens/new)

Note: _SKIP_GRADLE_TASKS_PARAM_ is used to pass parameters to the build process of the MIW jar. Currently, it skips the
tests and code coverage, but speeds up the build time.
If you want to activate it, just comment it out
like `SKIP_GRADLE_TASKS_PARAM="" #"-x jacocoTestCoverageVerification -x test"`

After every execution (either _local_ or _docker_ flow), run the matching "stop" task (
e.g.: `task docker:start-app` -> `task docker:stop-app`)

When you just run `task` without parameters, you will see all tasks available.

### local

1. Run `task docker:start-middleware` and wait until it shows "(main) Running the server in development mode. DO NOT use
   this configuration in production." in the terminal
2. Run `task app:build` to build the MIW application
3.
Run [ManagedIdentityWalletsApplication.java](src/main/java/org/eclipse/tractusx/managedidentitywallets/ManagedIdentityWalletsApplication.java)
via IDE and use the local.env file to populate environment vars (e.g. EnvFile plugin for IntelliJ)
4. Run `task app:get-token` and copy the token (including "BEARER" prefix) (Mac users have the token already in their
   clipboard :) )
5. Open API doc on http://localhost:8000 (or what port you configured in the _env.local_ file)
6. Click on Authorize on swagger UI and on the dialog paste the token into the "value" input
7. Click on "Authorize" and "close"
8. MIW is up and running

### docker

1. Run `task docker:start-app` and wait until it shows "Started ManagedIdentityWalletsApplication in ... seconds"
2. Run `task app:get-token` and copy the token (including "BEARER" prefix) (Mac users have the token already in their
   clipboard :) )
3. Open API doc on http://localhost:8000 (or what port you configured in the _env.local_ file)
4. Click on Authorize on swagger UI and on the dialog paste the token into the "value" input
5. Click on "Authorize" and "close"
6. MIW is up and running

# End Users

See OpenAPI documentation, which is automatically created from
the source and available on each deployment at the `/docs/api-docs/docs` endpoint
(e.g. locally at http://localhost:8087/docs/api-docs/docs). An export of the JSON
document can be also found in [docs/openapi_v001.json](docs/openapi_v001.json).

# Test Coverage

Jacoco is used to generate the coverage report. The report generation
and the coverage verification are automatically executed after tests.

The generated HTML report can be found under `jacoco-report/html/`

To generate the report run the command

```
task app:test-report
```

To check the coverage run the command

```
task app:coverage
```

Currently, the minimum is 80% coverage.

# Common issues and solutions during local setup

#### 1. Can not build with test cases

Test cases are written using the Spring Boot integration test frameworks. These test frameworks start the Spring Boot
test context, which allows us to perform integration testing. In our tests, we utilize the Testcontainers
library (https://java.testcontainers.org/) for managing Docker containers. Specifically, we use Testcontainers to start
PostgreSQL and Keycloak Docker containers locally.

Before running the tests, please ensure that you have Docker runtime installed and that you have the necessary
permissions to run containers.

Alternative, you can skip test during the build with `` ./gradlew clean build -x test``

#### 2. Database migration related issue

We have implemented database migration using Liquibase (https://www.liquibase.org/). Liquibase allows us to manage
database schema changes effectively.

In case you encounter any database-related issues, you can resolve them by following these steps:

1. Delete all tables from the database.
2. Restart the application.
3. Upon restart, the application will recreate the database schema from scratch.

This process ensures that any issues with the database schema are resolved by recreating it in a fresh state.

# Environment Variables `<a id= "environmentVariables"></a>`

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

## Helm

### Unit Test

This repository uses [Helm Unit Test](https://github.com/helm-unittest/helm-unittest) to test the Helm charts.

#### Installation

```bash
$ helm plugin install https://github.com/helm-unittest/helm-unittest.git
```

#### Run Tests

```bash
$ helm unittest <chart-name>
```

### Documentation

For helm chart documentation we use
the [Helm-Docs by Norwoodj](https://github.com/norwoodj/helm-docs).

#### Installation

Homebrew

```bash
brew install norwoodj/tap/helm-docs
```

Scoop

```bash
scoop install helm-docs
```

#### Generate Documentation

```
helm-docs
# OR
helm-docs --dry-run # prints generated documentation to stdout rather than modifying READMEs
```

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet
