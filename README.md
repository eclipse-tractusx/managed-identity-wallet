# Catena-X Core Custodian

This repository is part of the overarching Catena-X project, and more specifically
developed within the Catena-X Core Agile Release Train.

The Custodian implements the Self-Sovereign-Identity (SSI) readiness by providing
a wallet hosting platform including a DID resolver, service endpoints and the
company wallets itself.

Technically this project is built using the [ktor](https://ktor.io) Microservices
framework and thus the Kotlin language.

## Building with gradle

To install gradle just follow [the official guide](https://gradle.org/install/), e.g. on MacOS homebrew can be used:

```
brew install gradle
```

Building then works with

```
gradle build
```

## Running locally with gradle

```
gradle run
```

## Building and running the Docker image

Based on the [official documentation](https://ktor.io/docs/docker.html#getting-the-application-ready)
below the steps to build and run this service via Docker.

First step is to create the distribution of the application (in this case using Gradle):

```
gradle installDist
```

Next step is to build and tag the Docker image:

```
docker build -t catena-x/custodian:0.0.1 .
```

Finally, start the image:

```
docker run -p 8080:8080 catena-x/custodian:0.0.1
```
