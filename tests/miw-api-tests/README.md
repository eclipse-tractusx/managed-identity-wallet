# managed Identity Wallet(MIW) API testing

MIW API testing with cucumber, testNG and Xray.

### Environment wearables to be set for application

| Name                      | Description                                                                               |
|---------------------------|-------------------------------------------------------------------------------------------|
| MIW_HOST                  | Host of MIW application(ie. https://managed-identity-wallets-new.dev.demo.catena-x.net)   |
| AUTH_SERVER_URL           | Keycloak server URL(ie. https://centralidp.dev.cofinity-x.com/auth/)                      |
| BASE_WALLET_CLIENT_ID     | Keycloak client_id in which base wallet BPN is mapped                                     |
| BASE_WALLET_CLIENT_SECRET | Keycloak client_secret in which base wallet BPN is mapped                                 |
| BASE_WALLET_BPN           | Base wallet BPN                                                                           |
| USER_WALLET_CLIENT_ID     | Keycloak client_id in which user wallet BPN is mapped to test API as business partner     |
| USER_WALLET_CLIENT_SECRET | Keycloak client_secret in which user wallet BPN is mapped to test API as business partner |
| USER_WALLET_BPN           | User wallet BPN                                                                           |
| REALM                     | Keycloak realm name (ie. CX-Central)                                                      |

### Environment wearables to be for gradle(need to set in gradle.properties file)

| Name            | Description                                              |
|-----------------|----------------------------------------------------------|
| githubUserName  | Github username used to get ssi-lib from github packages |
| githubToken     | Github token used to get ssi-lib from github packages    |
| testPlanKey     | Xray test plan key                                       |
| testExecKey     | Xray test execution id                                   |
| clientId        | Xray API clientId                                        |
| clientSecret    | Xray API clientSecret                                    |
| testEnvironment | Xray test env                                            |

## Run test cases only

``./gradlew clean test``

## Run test cases and upload reports in xray

``./gradlew clean importJunitResultsToXrayCloud``