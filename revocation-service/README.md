# Bitstring Statuslist Service

This service is responsible for managing the status of credentials using a Bitstring status list. It supports operations such as creating, revoking, and retrieving credential statuses.

## Prerequisites

Before you begin, ensure you have met the following requirements:

- Java JDK 17 is installed.
- Docker is running if you are using containers for services like Keycloak and Postgres.
- Keycloak service is operational and accessible.
- Postgres database service is running and accessible.
- Environment variables are configured according to the application's requirements.
- MIW is deployed and accessable
- Be sure the right ssi-lib version is installed

## Environment Configuration

The application can be configured using environment variables. Below are the available configuration properties with their default values (if any). Ensure these variables are set in your environment before starting the service.

### Application Configuration

- **APPLICATION_NAME**: The name of the Spring Boot application. Defaults to "Revocation".
- **APPLICATION_PORT**: The HTTP port for the application. Defaults to 8080.
- **APPLICATION_PROFILE**: The active Spring profile. Defaults to "local".

### Database Configuration

- **DATABASE_HOST**: The hostname or IP address of the Postgres database.
- **DATABASE_PORT**: The port number on which the Postgres server is running. Defaults to 5432.
- **DATABASE_NAME**: The name of the database to connect to.
- **DATABASE_USERNAME**: The username for accessing the database.
- **DATABASE_USE_SSL_COMMUNICATION**: Whether to use SSL for database communication. Defaults to false.
- **DATABASE_PASSWORD**: The password for accessing the database.
- **DATABASE_CONNECTION_POOL_SIZE**: The size of the database connection pool. Defaults to 10.

### Swagger Configuration

- **ENABLE_SWAGGER_UI**: Flag to enable or disable Swagger UI. Defaults to false.
- **ENABLE_API_DOC**: Flag to enable or disable API documentation. Defaults to false.

### Logging Configuration

- **APPLICATION_LOG_LEVEL**: The application-wide log level. Defaults to "DEBUG".

### Security Configuration

The application integrates with Keycloak for OAuth2 authentication and authorization:

- **SERVICE_SECURITY_ENABLED**: Flag to enable or disable Servive Security integration for Disabling Swagger and other Endpoints. Defaults to true, false only for test purposes recommended.

The application integrates with Keycloak for OAuth2 authentication and authorization:

- **KEYCLOAK_ENABLED**: Flag to enable or disable Keycloak integration. Defaults to true.
- **KEYCLOAK_REALM**: The Keycloak realm to connect to.
- **KEYCLOAK_CLIENT_ID**: The Keycloak client ID for the application.
- **KEYCLOAK_PUBLIC_CLIENT_ID**: The Keycloak public client ID, used for Swagger UI authentication.
- **AUTH_SERVER_URL**: The URL for the Keycloak authentication server.

Keycloak URLs for token and authentication management:

- **KEYCLOAK_AUTH_URL**: Constructed from `AUTH_SERVER_URL`, `KEYCLOAK_REALM`.
- **KEYCLOAK_TOKEN_URL**: Constructed from `AUTH_SERVER_URL`, `KEYCLOAK_REALM`.
- **KEYCLOAK_REFRESH_TOKEN_URL**: Same as `KEYCLOAK_TOKEN_URL`.
- **KEYCLOAK_USERNAME**: The username for accessing the Keycloak management APIs.
- **KEYCLOAK_PASSWORD**: The password for accessing the Keycloak management APIs.

### External Service URLs

- **MIW_URL**: The URL for the Middleware (MIW) used for signing status list credentials.
- **DOMAIN_URL**: The base URL for your domain, which may be used for service-to-service communication or callbacks.

## Spring Boot Configuration

The `server`, `spring`, `springdoc`, `management`, and `logging` sections of the YAML are Spring Boot-specific configurations. They configure the application's behavior, data source, OpenAPI documentation, and logging levels, among other things.

Be sure to replace placeholder values in the environment variables with actual data according to your application's specific requirements.

## Middleware (MIW) Setup

Ensure that the middleware (MIW) is running, as it is used to sign the status list credentials.

An Overview how to start the middleware can be found under the Readme.md in here:[README.md](..%2Fmiw%2FREADME.md)

## Starting Services

To start the Bitstring Statuslist Service, follow these steps:

1. **Start Keycloak and Postgres:**

   Ensure that both Keycloak and Postgres services are running. For development purposes the Keycloak and
   Postgres from the MIW Dev Setup can be used if not
   already running with the MIW Task deployment.

   [dev-assets](..%2Fdev-assets)

   For starting it, follow the Guidelines in the MIW-Repo Readme.md -> Development Setup -> local
   with `task docker:start-middleware` or manually
   call start the docker compose file.

2. **Start the Service:**

Execute the following command from the root of the project to start the service:
./gradlew bootRun

## Access Swagger UI

After starting the service, access the Swagger UI to test API endpoints by navigating to the following URL:
http://localhost:8080/ui/swagger-ui/

Replace `localhost` and `8080` with your service's host and port if they are different.

## Build and Run Using Gradle

- To build the project, run the following command:

```
cd revpcation-service
./../gradlew clean build
```

- To run the tests:
```
cd revpcation-service
./../gradlew clean test
```

## Additional Information

For more information on how to configure and use the service, refer to the provided documentation or contact the development team.
