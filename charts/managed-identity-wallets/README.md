
# DEPRECATED

# managed-identity-wallets

![Version: 0.7.5](https://img.shields.io/badge/Version-0.7.5-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 3.3.2](https://img.shields.io/badge/AppVersion-3.3.2-informational?style=flat-square)

Managed Identity Wallets Service

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | acapypostgresql(postgresql) | 11.x.x |
| https://charts.bitnami.com/bitnami | postgresql(postgresql) | 11.x.x |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| auth.realm | string | `"CX-Central"` | The realm name in Keycloak |
| auth.role | string | `"add_wallets"` | The main role in MIW |
| auth.roleMappings | string | `"create_wallets:add_wallets,view_wallets:view_wallets,update_wallets:update_wallets,delete_wallets:delete_wallets,view_wallet:view_wallet,update_wallet:update_wallet"` | The role mapping in MIW |
| auth.resourceId | string | `"Cl5-CX-Custodian"` | The resource Id in Keycloak |
| auth.jwksUrl | string | `"https://localhost:8081"` | IAM certs url |
| auth.issuerUrl | string | `"https://localhost:8081"` | IAM token issuer url |
| auth.redirectUrl | string | `"https:/localhost:8080/callback"` | IAM redirect url to the MIW |
| image.name | string | `"catenax-ng/tx-managed-identity-wallets_service"` |  |
| image.registry | string | `"ghcr.io"` |  |
| image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| image.secret | string | `"acr-credentials"` |  |
| allowEmptyPassword | string | `"\"yes\""` |  |
| db.jdbcDriver | string | `"org.postgresql.Driver"` | Database driver to use |
| namespace | string | `"managed-identity-wallets"` |  |
| logging | object | `{"exposed":"INFO","jetty":"INFO","ktorRoot":"INFO","netty":"INFO","servicesCalls":"INFO"}` | The logging configurations |
| logging.servicesCalls | string | `"INFO"` | Options: ALL, HEADERS, BODY, INFO, NONE |
| httpClientTimeout | object | `{"bpdServiceConnect":"25000","bpdServiceRequest":"25000","bpdServiceSocket":"25000","revocationServiceConnect":"25000","revocationServiceRequest":"25000","revocationServiceSocket":"25000","walletServiceConnect":"25000","walletServiceRequest":"25000","walletServiceSocket":"25000","webhookServiceConnect":"25000","webhookServiceRequest":"25000","webhookServiceSocket":"25000"}` | The timeout (milliseconds) configuration of http client in each service. |
| openapi.title | string | `"Managed Identity Wallets API"` |  |
| openapi.description | string | `"Managed Identity Wallets API"` |  |
| openapi.termsOfServiceUrl | string | `"https://www.eclipse.org/legal/termsofuse.php"` |  |
| openapi.contactName | string | `"Eclipse Tractus-X"` |  |
| openapi.contactEmail | string | `"tractusx-dev@eclipse.org"` |  |
| openapi.contactUrl | string | `"https://projects.eclipse.org/projects/automotive.tractusx"` |  |
| openapi.licenseName | string | `"Apache 2.0"` |  |
| openapi.licenseUrl | string | `"https://github.com/eclipse-tractusx/managed-identity-wallets/blob/main/LICENSE"` |  |
| wallet.baseWalletBpn | string | `""` | The BPN of the base wallet |
| wallet.baseWalletShortDid | string | `""` | The short DID of the base wallet. It can be created with its verkey as described in https://github.com/eclipse-tractusx/managed-identity-wallets#Integrate-with-Indy-Ledger. It should be registered on the Indy ledger with role endorser. |
| wallet.baseWalletVerkey | string | `""` | The verkey (public key) of the base wallet |
| wallet.baseWalletName | string | `""` | The name of the base wallet |
| wallet.allowlistDids | string | `""` | A list of full DIDs seperated by comma ",". Those DIDs are allowed to send a connection request to managed wallets. Empty for public invitation allowance |
| wallet.membershipOrganisation | string | `"Platform-A"` | The name used in the Membership credential |
| revocation.refreshHour | string | `"3"` | At which hour (24-hour clock) the cron job should issue/update status-list credentials |
| revocation.revocationServiceUrl | string | `"http://localhost:8086"` | The url of the revocation service |
| revocationService.imageName | string | `"registry.gitlab.com/gaia-x/data-infrastructure-federation-services/not/notarization-service/revocation"` |  |
| revocationService.tag | string | `"1.0.0-SNAPSHOT-quarkus-2.10.2.Final-java17"` |  |
| revocationService.port | string | `"8086"` |  |
| revocationService.httpAccessLog | bool | `true` |  |
| revocationService.minIssueInterval | string | `"2"` | Issuance cache interval |
| revocationService.baseUrlForCredentialList | string | `"https//localhost:8080/api/credentials/"` | The the endpoint in MIW at which status credentials can be issued |
| revocationService.clientIssuanceApiUrl | string | `"http://localhost:8080"` | The url at which the MIW is reachable |
| acapy.imageName | string | `"bcgovimages/aries-cloudagent"` |  |
| acapy.tag | string | `"py36-1.16-1_0.7.5"` |  |
| acapy.endorser.genesisUrl | string | `"http://dev.greenlight.bcovrin.vonx.io/genesis"` | The url of the used Indy ledger |
| acapy.endorser.label | string | `"BaseWalletIssuer"` | The label of the instance |
| acapy.endorser.logLevel | string | `"INFO"` |  |
| acapy.endorser.networkIdentifier | string | `"greenlight:dev"` | The network identifier of the used Indy ledger |
| acapy.endorser.endpointUrl | string | `"https://localhost:8000"` | The endpoint url in the DID document |
| acapy.endorser.databaseHost | string | `"acapypostgresql"` | The host of the used database |
| acapy.endorser.endpointPort | string | `"8000"` | The port at which the wallet is reachable |
| acapy.endorser.adminPort | string | `"11000"` | The port at which the admin API is reachable |
| acapy.endorser.adminUrl | string | `"http://localhost:11000"` | The url of the admin API |
| acapy.endorser.secret.apikey | string | `"0"` | The API key of the admin endpoints. It must be a random and secure string |
| acapy.endorser.secret.walletseed | string | `"RandomSeedPlaceHolder0x135791113"` | The seed of the wallet. It must be random and secure (no patterns or use of dictionary words, the use of uppercase and lowercase letters - as well as numbers and allowed symbols, no personal preferences like names or phone numbers). It must be registired on the used ledger as endorser. |
| acapy.endorser.secret.dbaccount | string | `"postgres"` |  |
| acapy.endorser.secret.dbadminuser | string | `"postgres"` |  |
| acapy.endorser.secret.dbadminpassword | string | `"postgres"` |  |
| acapy.endorser.secret.dbpassword | string | `"postgres"` |  |
| acapy.endorser.secret.jwtsecret | string | `"0"` |  |
| acapy.endorser.secret.walletkey | string | `"0"` |  |
| acapy.mt.genesisUrl | string | `"http://dev.greenlight.bcovrin.vonx.io/genesis"` | The url of the used Indy ledger |
| acapy.mt.label | string | `"ManagedWallet"` | The label of the instance |
| acapy.mt.logLevel | string | `"INFO"` |  |
| acapy.mt.networkIdentifier | string | `"greenlight:dev"` | The network identifier of the used Indy ledger |
| acapy.mt.endpointUrl | string | `"https://localhost:8003"` | The endpoint url in the DID document |
| acapy.mt.databaseHost | string | `"acapypostgresql"` | The host of the used database |
| acapy.mt.endpointPort | string | `"8003"` | The port at which the sub-wallets are reachable |
| acapy.mt.adminPort | string | `"11003"` | The port at which the admin API is reachable |
| acapy.mt.adminUrl | string | `"http://localhost:11003"` | The url of the admin API |
| acapy.mt.endorserPublicDid | string | `"ShortDIDPlaceholderX"` | The short DID of the base wallet |
| acapy.mt.webhookUrl | string | `"http://localhost:8080/webhook"` | The url at which events are sent. It should be the webhook endpoint in MIW |
| acapy.mt.secret.apikey | string | `"0"` | The API-Key of the admin endpoints. It must be a random and secure string |
| acapy.mt.secret.walletseed | string | `"RandomSeedPlaceHolder0x246810120"` | The seed of the wallet. It must be random and secure (no patterns or use of dictionary words, the use of uppercase and lowercase letters - as well as numbers and allowed symbols, no personal preferences like names or phone numbers). It must be registired on the used ledger as endorser. |
| acapy.mt.secret.dbaccount | string | `"postgres"` |  |
| acapy.mt.secret.dbadminuser | string | `"postgres"` |  |
| acapy.mt.secret.dbadminpassword | string | `"postgres"` |  |
| acapy.mt.secret.dbpassword | string | `"postgres"` |  |
| acapy.mt.secret.jwtsecret | string | `"0"` |  |
| acapy.mt.secret.walletkey | string | `"0"` |  |
| ingress.enabled | bool | `false` |  |
| acapypostgresql.enabled | bool | `true` |  |
| acapypostgresql.auth.existingSecret | string | `"product-managed-identity-wallets-acapypostgresql"` |  |
| acapypostgresql.secret.password | string | `"postgres"` |  |
| acapypostgresql.secret.postgrespassword | string | `"postgres"` |  |
| acapypostgresql.secret.user | string | `"postgres"` |  |
| postgresql.useDefaultJdbcUrl | bool | `true` | use the default jdbc url `jdbc:postgresql://%s-postgresql:5432/postgres?user=%s&password=%s` instead of the value in managedIdentityWallets.secrets.jdbcUrl |
| postgresql.enabled | bool | `true` |  |
| postgresql.auth.existingSecret | string | `"product-managed-identity-wallets-postgresql"` |  |
| postgresql.primary.extraVolumeMounts[0].name | string | `"initdb"` |  |
| postgresql.primary.extraVolumeMounts[0].mountPath | string | `"/docker-entrypoint-initdb.d"` |  |
| postgresql.primary.extraVolumes[0].name | string | `"initdb"` |  |
| postgresql.primary.extraVolumes[0].emptyDir | object | `{}` |  |
| postgresql.primary.initContainers[0].name | string | `"initdb"` |  |
| postgresql.primary.initContainers[0].image | string | `"ghcr.io/catenax-ng/tx-managed-identity-wallets_initdb:3.3.2"` | The image is built and used to initialize the database of MIW. The tag must equal the appVersion in Chart.yaml |
| postgresql.primary.initContainers[0].imagePullPolicy | string | `"Always"` |  |
| postgresql.primary.initContainers[0].command[0] | string | `"sh"` |  |
| postgresql.primary.initContainers[0].args[0] | string | `"-c"` |  |
| postgresql.primary.initContainers[0].args[1] | string | `"echo \"Copying initdb sqls...\"\ncp -R /initdb/* /docker-entrypoint-initdb.d\n"` |  |
| postgresql.primary.initContainers[0].volumeMounts[0].name | string | `"initdb"` |  |
| postgresql.primary.initContainers[0].volumeMounts[0].mountPath | string | `"/docker-entrypoint-initdb.d"` |  |
| postgresql.secret.password | string | `"postgres"` |  |
| postgresql.secret.postgrespassword | string | `"postgres"` |  |
| postgresql.secret.user | string | `"postgres"` |  |
| datapool.grantType | string | `"client_credentials"` |  |
| datapool.scope | string | `"openid"` |  |
| datapool.refreshHour | string | `"23"` | At which hour (24-hour clock) the cron job should pull the data from the BPDM data pool |
| datapool.url | string | `""` | Url at which the API of BPDM is reachable |
| datapool.authUrl | string | `""` | IAM url to get the access token for BPDM data pool endpoint |
| managedIdentityWallets.secret.jdbcurl | string | `"jdbc:postgresql://<host>:<port>/<db-name>?user=<user>&password=<password>"` | Database connection string to the Postgres database of MIW. Used when postgresql.useDefaultJdbcUrl is false |
| managedIdentityWallets.secret.authclientid | string | `"clientid"` | It can be extracted from Keycloak |
| managedIdentityWallets.secret.authclientsecret | string | `"client"` | It can be extracted from Keycloak |
| managedIdentityWallets.secret.bpdmauthclientid | string | `"clientid"` | client id for accessing the BPDM data pool endpoint |
| managedIdentityWallets.secret.bpdmauthclientsecret | string | `"client"` | client secret for accessing the BPDM data pool endpoint |
| certificate.host | string | `"localhost"` |  |
| isLocal | bool | `false` | Deployment on Kubernetes on local device |
| resources | object | `{"endorserAcapy":{"limits":{"cpu":"250m","memory":"256Mi"},"requests":{"cpu":"100m","memory":"128Mi"}},"managedIdentityWallets":{"limits":{"cpu":"250m","memory":"256Mi"},"requests":{"cpu":"100m","memory":"128Mi"}},"mtAcapy":{"limits":{"cpu":"250m","memory":"256Mi"},"requests":{"cpu":"100m","memory":"128Mi"}},"revocationService":{"limits":{"cpu":"250m","memory":"256Mi"},"requests":{"cpu":"100m","memory":"128Mi"}}}` | used resources for each componenet |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
