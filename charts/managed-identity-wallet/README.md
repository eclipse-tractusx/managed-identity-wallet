# managed-identity-wallet

![Version: 1.0.1](https://img.shields.io/badge/Version-1.0.1-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 0.0.1](https://img.shields.io/badge/AppVersion-0.0.1-informational?style=flat-square)

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

| Repository                         | Name       | Version |
|------------------------------------|------------|---------|
| https://charts.bitnami.com/bitnami | common     | 2.x.x   |
| https://charts.bitnami.com/bitnami | keycloak   | 15.1.6  |
| https://charts.bitnami.com/bitnami | postgresql | 11.9.13 |

## Parameters

### MIW Common parameters

| Name                         | Description                                                                                  | Value                              |
| ---------------------------- | -------------------------------------------------------------------------------------------- | ---------------------------------- |
| `replicaCount`               | The amount of replicas to run                                                                | `1`                                |
| `nameOverride`               | String to partially override common.names.fullname template (will maintain the release name) | `""`                               |
| `fullnameOverride`           | String to fully override common.names.fullname template                                      | `""`                               |
| `image.repository`           | MIW image repository                                                                         | `tractusx/managed-identity-wallet` |
| `image.pullPolicy`           | MIW image pull policy                                                                        | `Always`                           |
| `image.tag`                  | MIW image tag (empty one will use "appVersion" value from chart definition)                  | `""`                               |
| `secrets`                    | Parameters for the application (will be stored as secrets - so, for passwords, ...)          | `{}`                               |
| `envs`                       | Parameters for the application (will be provided as environment variables)                   | `{}`                               |
| `serviceAccount.create`      | Enable creation of ServiceAccount                                                            | `true`                             |
| `serviceAccount.annotations` | Annotations to add to the ServiceAccount                                                     | `{}`                               |
| `serviceAccount.name`        | The name of the ServiceAccount to use.                                                       | `""`                               |

### Managed Identity Wallet Common Parameters

| Name                                       | Description                                                                                                                       | Value       |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `service.type`                             | Kubernetes Service type                                                                                                           | `ClusterIP` |
| `service.port`                             | Kubernetes Service port                                                                                                           | `8080`      |
| `ingress.enabled`                          | Enable ingress controller resource                                                                                                | `false`     |
| `ingress.annotations`                      | Ingress annotations                                                                                                               | `{}`        |
| `ingress.hosts`                            | Ingress accepted hostnames                                                                                                        | `[]`        |
| `ingress.tls`                              | Ingress TLS configuration                                                                                                         | `[]`        |
| `podSecurityContext`                       | Pod Security Context                                                                                                              | `{}`        |
| `jobSecurityContext.runAsUser`             | User ID used to run the job                                                                                                       | `1001`      |
| `jobSecurityContext.runAsGroup`            | Group ID used to run the job                                                                                                      | `0`         |
| `jobSecurityContext.runAsNonRoot`          | Run the job as a non-root user                                                                                                    | `true`      |
| `securityContext.privileged`               | Enable privileged container                                                                                                       | `false`     |
| `securityContext.allowPrivilegeEscalation` | Allow privilege escalation                                                                                                        | `false`     |
| `securityContext.runAsUser`                | User ID used to run the container                                                                                                 | `1001`      |
| `securityContext.runAsGroup`               | Group ID used to run the container                                                                                                | `0`         |
| `securityContext.runAsNonRoot`             | Run the container as a non-root user                                                                                              | `true`      |
| `resources.requests.cpu`                   | CPU resource requests                                                                                                             | `250m`      |
| `resources.requests.memory`                | Memory resource requests                                                                                                          | `500Mi`     |
| `resources.limits.cpu`                     | CPU resource limits                                                                                                               | `2`         |
| `resources.limits.memory`                  | Memory resource limits                                                                                                            | `1Gi`       |
| `nodeSelector`                             | [node selector](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector) to constrain pods to nodes | `{}`        |
| `tolerations`                              | Tolerations for pod assignment                                                                                                    | `[]`        |
| `affinity`                                 | Affinity for pod assignment                                                                                                       | `{}`        |
| `podAnnotations`                           | Pod annotations                                                                                                                   | `{}`        |

### Managed Identity Wallets Primary Parameters

| Name                                     | Description                                                                                                                                   | Value                                              |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| `miw.host`                               | Host name. Default: <release name>-managed-identity-wallet:<port>                                                                             | `{{ .Release.Name }}-managed-identity-wallet:8080` |
| `miw.environment`                        | Runtime environment. Should be ether local, dev, int or prod                                                                                  | `dev`                                              |
| `miw.jobs.createDatabaseIfNotExists`     | Enable to create the database if it does not exist                                                                                            | `true`                                             |
| `miw.ssi.enforceHttpsInDidWebResolution` | Enable to use HTTPS in DID Web Resolution                                                                                                     | `false`                                            |
| `miw.ssi.vcExpiryDate`                   | Verifiable Credential expiry date. Format 'dd-MM-yyyy'. If empty it is set to 31-12-<current year>                                            | `""`                                               |
| `miw.authorityWallet.bpn`                | Authority Wallet BPN                                                                                                                          | `BPNL000000000000`                                 |
| `miw.logging.level`                      | Log level. Should be ether ERROR, WARN, INFO, DEBUG, or TRACE.                                                                                | `INFO`                                             |
| `miw.database.useSSL`                    | Set to true to enable SSL connection to the database                                                                                          | `false`                                            |
| `miw.database.port`                      | Database port                                                                                                                                 | `5432`                                             |
| `miw.database.host`                      | Database host. Default: <release name>-postgresql                                                                                             | `{{ .Release.Name }}-postgresql`                   |
| `miw.database.user`                      | Database user                                                                                                                                 | `miw`                                              |
| `miw.database.name`                      | Database name                                                                                                                                 | `miw_app`                                          |
| `miw.database.secret`                    | Existing secret name for the database password. Default: <release name>-postgresql                                                            | `{{ .Release.Name }}-postgresql`                   |
| `miw.database.secretPasswordKey`         | Existing secret key for the database password                                                                                                 | `password`                                         |
| `miw.database.encryptionKey.value`       | Database encryption key for confidential data.  Ignored if `secret` is set. If empty a secret with 32 random alphanumeric chars is generated. | `""`                                               |
| `miw.database.encryptionKey.secret`      | Existing secret for database encryption key                                                                                                   | `""`                                               |
| `miw.database.encryptionKey.secretKey`   | Existing secret key for database encryption key                                                                                               | `""`                                               |
| `miw.keycloak.realm`                     | Keycloak realm                                                                                                                                | `miw_test`                                         |
| `miw.keycloak.clientId`                  | Keycloak client id                                                                                                                            | `miw_private_client`                               |
| `miw.keycloak.url`                       | Keycloak URL. Default: http://<release name>-keycloak                                                                                         | `http://{{ .Release.Name }}-keycloak`              |

### Keycloak Parameters (for more parameters see https://github.com/bitnami/charts/tree/main/bitnami/keycloak)

| Name                                                  | Description                                                                        | Value                            |
| ----------------------------------------------------- | ---------------------------------------------------------------------------------- | -------------------------------- |
| `keycloak.enabled`                                    | Enable to deploy Keycloak                                                          | `true`                           |
| `keycloak.jobs.createDatabaseIfNotExists`             | Enable to create keycloak database if not exists                                   | `true`                           |
| `keycloak.extraEnvVars[0].name`                       | KEYCLOAK_HOSTNAME                                                                  | `KEYCLOAK_HOSTNAME`              |
| `keycloak.extraEnvVars[0].value`                      | {{ .Release.Name }}-keycloak                                                       | `{{ .Release.Name }}-keycloak`   |
| `keycloak.postgresql.enabled`                         | Enable to deploy PostgreSQL                                                        | `false`                          |
| `keycloak.externalDatabase.host`                      | Database host. Default: <release name>-postgresql                                  | `{{ .Release.Name }}-postgresql` |
| `keycloak.externalDatabase.port`                      | Database port                                                                      | `5432`                           |
| `keycloak.externalDatabase.user`                      | Database user                                                                      | `miw`                            |
| `keycloak.externalDatabase.database`                  | Database name                                                                      | `miw_keycloak`                   |
| `keycloak.externalDatabase.existingSecret`            | Existing secret name for the database password. Default: <release name>-postgresql | `{{ .Release.Name }}-postgresql` |
| `keycloak.externalDatabase.existingSecretPasswordKey` | Existing secret key for the database password                                      | `password`                       |
| `keycloak.auth.adminUser`                             | Keycloak admin user                                                                | `admin`                          |
| `keycloak.auth.adminPassword`                         | Keycloak admin password                                                            | `""`                             |
| `keycloak.keycloakConfigCli.enabled`                  | Enable to create the miw playground realm                                          | `true`                           |
| `keycloak.keycloakConfigCli.existingConfigmap`        | Existing configmap name for the realm configuration                                | `keycloak-realm-config`          |
| `keycloak.keycloakConfigCli.backoffLimit`             | Number of retries before considering a Job as failed                               | `2`                              |

### Postgresql Parameters (for more parameters see https://github.com/bitnami/charts/tree/main/bitnami/postgresql)

| Name                                              | Description                                                                         | Value         |
| ------------------------------------------------- | ----------------------------------------------------------------------------------- | ------------- |
| `postgresql.enabled`                              | Enable to deploy Postgresql                                                         | `true`        |
| `postgresql.auth.enablePostgresUser`              | Enable to create the postgresql admin user                                          | `false`       |
| `postgresql.auth.username`                        | Postgresql user to create                                                           | `miw`         |
| `postgresql.auth.password`                        | Postgresql password to set (if empty one is generated)                              | `""`          |
| `postgresql.backup.enabled`                       | Enable to create a backup cronjob                                                   | `false`       |
| `postgresql.backup.conjob.schedule`               | Backup schedule                                                                     | `* */6 * * *` |
| `postgresql.backup.conjob.storage.existingClaim`  | Name of an existing PVC to use                                                      | `""`          |
| `postgresql.backup.conjob.storage.resourcePolicy` | Set resource policy to "keep" to avoid removing PVCs during a helm delete operation | `keep`        |
| `postgresql.backup.conjob.storage.size`           | PVC Storage Request for the backup data volume                                      | `8Gi`         |


## Bar