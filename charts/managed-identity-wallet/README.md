# managed-identity-wallet

![Version: 1.1.0](https://img.shields.io/badge/Version-1.1.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.0.1](https://img.shields.io/badge/AppVersion-0.0.1-informational?style=flat-square)

Managed Identity Wallet is supposed to supply a secure data source and data sink for Digital Identity Documents (DID), in order to enable Self-Sovereign Identity founding on those DIDs.
And at the same it shall support an uninterrupted tracking and tracing and documenting the usage of those DIDs, e.g., within logistical supply chains.

**Homepage:** <https://github.com/eclipse-tractusx/managed-identity-wallet>

## Get Repo Info

    helm repo add tractusx-dev https://eclipse-tractusx.github.io/charts/dev
    helm repo update

## Install chart

    helm install [RELEASE_NAME] tractusx-dev/managed-identity-wallet

The command deploys miw on the Kubernetes cluster in the default configuration.

See configuration below.

See [helm install](https://helm.sh/docs/helm/helm_install/) for command documentation.

## Uninstall Chart

    helm uninstall [RELEASE_NAME]

This removes all the Kubernetes components associated with the chart and deletes the release.

See [helm uninstall](https://helm.sh/docs/helm/helm_uninstall/) for command documentation.

## Upgrading Chart

    helm upgrade [RELEASE_NAME] [CHART]

See [helm upgrade](https://helm.sh/docs/helm/helm_upgrade/) for command documentation.

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://charts.bitnami.com/bitnami | postgresql | 11.9.13 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity | object | `{}` | Affinity configuration |
| backup | object | `{"database":{"cron":"* */6 * * *","enabled":false,"storage":{"diskSize":"10G","keepStorage":true,"storageClassName":"-"}}}` | Simple Postgresql backup solution (Dump data to second PV) |
| backup.database | object | `{"cron":"* */6 * * *","enabled":false,"storage":{"diskSize":"10G","keepStorage":true,"storageClassName":"-"}}` | Backup database |
| backup.database.cron | string | `"* */6 * * *"` | Backup schedule (help: https://crontab.guru) |
| backup.database.enabled | bool | `false` | Enable / Disable the backup |
| backup.database.storage | object | `{"diskSize":"10G","keepStorage":true,"storageClassName":"-"}` | Storage configuration |
| backup.database.storage.diskSize | string | `"10G"` | Disk size for backup content |
| backup.database.storage.keepStorage | bool | `true` | Set to true, if the PV should stay even when the chart release is uninstalled |
| backup.database.storage.storageClassName | string | `"-"` | storageClassName |
| envs | object | `{"APPLICATION_ENVIRONMENT":"dev","AUTHORITY_WALLET_BPN":"","AUTHORITY_WALLET_DID":"","AUTHORITY_WALLET_NAME":"","AUTH_SERVER_URL":"","DB_POOL_SIZE":"","KEYCLOAK_MIW_PUBLIC_CLIENT":"","KEYCLOAK_REALM":"","MANAGEMENT_PORT":null,"MIW_HOST_NAME":"localhost","USE_SSL":false,"VC_EXPIRY_DATE":"","VC_SCHEMA_LINK":""}` | Parameters for the application (will be provided as plain environment variables) |
| envs.APPLICATION_ENVIRONMENT | string | `"dev"` | Application environments like dev, int, prod |
| envs.AUTHORITY_WALLET_BPN | string | `""` | Authority/base wallet/root wallet BPN |
| envs.AUTHORITY_WALLET_DID | string | `""` | Authority/base wallet/root wallet web did |
| envs.AUTHORITY_WALLET_NAME | string | `""` | Authority/base wallet/root wallet name |
| envs.AUTH_SERVER_URL | string | `""` | Keycloak server url |
| envs.DB_POOL_SIZE | string | `""` | Initial database connection pool size |
| envs.KEYCLOAK_MIW_PUBLIC_CLIENT | string | `""` | Keycloak public client id, used only if we want to enable login in swagger using keycloak |
| envs.KEYCLOAK_REALM | string | `""` | Keycloak realm name |
| envs.MANAGEMENT_PORT | string | `nil` | Spring actuator port |
| envs.MIW_HOST_NAME | string | `"localhost"` | Hostname of miw application |
| envs.USE_SSL | bool | `false` | Whether database connection with SSL, true if the database connection is done using SSL |
| envs.VC_EXPIRY_DATE | string | `""` | Default expiry date of issued VC |
| envs.VC_SCHEMA_LINK | string | `""` | Verifiable credential schema URL, which will be part of @context in VC |
| fullnameOverride | string | `""` |  |
| image | object | `{"pullPolicy":"Always","repository":"ghcr.io/catenax-ng/tx-managed-identity-wallets_miw_service","tag":""}` | Image of the main container |
| image.pullPolicy | string | `"Always"` | PullPolicy |
| image.repository | string | `"ghcr.io/catenax-ng/tx-managed-identity-wallets_miw_service"` | Image repository |
| image.tag | string | `""` | Image tag (empty one will use "appVersion" value from chart definition) |
| imagePullSecrets | list | `[]` | Credentials name for private repos |
| ingress | object | `{"annotations":{},"enabled":false,"hosts":[{"host":"chart-example.local","paths":[{"path":"/","pathType":"ImplementationSpecific"}]}],"tls":[]}` | Ingress configuration |
| nameOverride | string | `""` |  |
| nodeSelector | object | `{"kubernetes.io/os":"linux"}` | NodeSelector configuration |
| podAnnotations | object | `{}` | PodAnnotation configuration |
| podSecurityContext | object | `{}` | PodSecurityContext |
| postgresql | object | `{"auth":{"database":"miw","username":"miw"},"external":{"auth":{"existingSecret":"","existingSecretKey":"password","password":"","username":""},"config":{"database":"","host":"","port":5432}},"internal":{"enabled":true}}` | Configuration of the Postgresql database (internal and external) |
| postgresql.auth | object | `{"database":"miw","username":"miw"}` | Default settings for the primary database and user |
| postgresql.auth.database | string | `"miw"` | MIW database name |
| postgresql.auth.username | string | `"miw"` | username for MIW database |
| postgresql.external | object | `{"auth":{"existingSecret":"","existingSecretKey":"password","password":"","username":""},"config":{"database":"","host":"","port":5432}}` | Configure own postgresql database |
| postgresql.external.auth.existingSecret | string | `""` | Existing secret with provided password |
| postgresql.external.auth.existingSecretKey | string | `"password"` | Key name of password in secret |
| postgresql.external.auth.password | string | `""` | DB password |
| postgresql.external.auth.username | string | `""` | DB username |
| postgresql.external.config | object | `{"database":"","host":"","port":5432}` | General config |
| postgresql.external.config.database | string | `""` | Existing database to use |
| postgresql.external.config.host | string | `""` | Instance host or IP |
| postgresql.external.config.port | int | `5432` | Instance port |
| postgresql.internal | object | `{"enabled":true}` | Configure bundled postgresql |
| postgresql.internal.enabled | bool | `true` | Enable bundled database |
| replicaCount | int | `1` | The amount of replicas to run |
| resources | object | `{"limits":{"cpu":4,"memory":"1Gi"},"requests":{"cpu":"250m","memory":"500Mi"}}` | Resource boundaries |
| secrets | object | `{"ENCRYPTION_KEY":"","KEYCLOAK_CLIENT_ID":""}` | Parameters for the application (will be stored as secrets - so, for important values, ...) |
| secrets.ENCRYPTION_KEY | string | `""` | AES encryption key used to encrypt/decrypt private keys (random 32 chars) |
| secrets.KEYCLOAK_CLIENT_ID | string | `""` | Keycloak confidential client id for MIW application |
| securityContext | object | `{"allowPrivilegeEscalation":false,"privileged":false,"runAsGroup":11111,"runAsNonRoot":true,"runAsUser":11111}` | Preconfigured SecurityContext |
| service | object | `{"port":8080,"type":"ClusterIP"}` | Service configuration |
| serviceAccount | object | `{"annotations":{},"create":true,"name":""}` | ServiceAccount configuration |
| tolerations | list | `[]` | Tolerations configuration |

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Peter Motzko | <peter.motzko@volkswagen.de> | <https://github.com/pmoscode> |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
