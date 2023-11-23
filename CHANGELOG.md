# [0.2.0-develop.7](https://github.com/eclipse-tractusx/managed-identity-wallet/compare/v0.2.0-develop.6...v0.2.0-develop.7) (2023-11-23)


### Bug Fixes

* CGD-468: Application starts with corrupted data in case of invalid AES key ([c734946](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/c734946abfc1c34f1710f74e1329505dafa2fa00))
* **ci:** helm chart release ([c2bd166](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/c2bd166f25c4488e1bc0d7bb5215d92602ad9f96))
* missing env variables in release workflow ([#111](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/111)) ([0f99498](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/0f9949899eb218e0b3b1b3742c0ea5ee82d19701))
* rate limit during json-ld context loading ([#100](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/100)) ([09d1f1a](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/09d1f1a4f5a8a2bbaf7e328efea4ed60d232d778))
* typo in Bearer ([754b90a](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/754b90a9fa90a308584949a6bf8085b6b27d8a19))
* Typo in Bearer ([#102](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/102)) ([3765c68](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/3765c689fcadc29f663c0521c3b9cc072ee8e779))


### Features

* add Eclipse Copyright header to CHANGELOG.md.jinja ([dd53533](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/dd53533bc6ccb9a160e4c51c46438321ed2b2be7))
* add GH Action workflow to test the app (incl. coverage) ([1e16b04](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/1e16b0418825ea402dd4a690d760b367a5c0ce8b))
* BPN validation added in issue dismantler VC ([5a04a2e](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/5a04a2ef41e48feb42c2db5baa54ea0e54833a3f))
* **ci:** semantic releases from develop branch ([#87](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/87)) ([fda8ee6](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/fda8ee6ae864aa86823a7ebfdce9702f372a9ded))
* error msg updated ([2b305d2](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/2b305d2d488a7ed1c63022ff4241ad5fa588822d))

# [0.2.0-develop.2](https://github.com/eclipse-tractusx/managed-identity-wallet/compare/v0.2.0-develop.1...v0.2.0-develop.2) (2023-10-20)


### Bug Fixes

* **ci:** helm chart release ([c2bd166](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/c2bd166f25c4488e1bc0d7bb5215d92602ad9f96))

# [0.2.0-develop.1](https://github.com/eclipse-tractusx/managed-identity-wallet/compare/v0.1.1...v0.2.0-develop.1) (2023-10-18)


### Bug Fixes

* CGD-468: Application starts with corrupted data in case of invalid AES key ([c734946](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/c734946abfc1c34f1710f74e1329505dafa2fa00))
* missing env variables in release workflow ([#111](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/111)) ([0f99498](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/0f9949899eb218e0b3b1b3742c0ea5ee82d19701))
* rate limit during json-ld context loading ([#100](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/100)) ([09d1f1a](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/09d1f1a4f5a8a2bbaf7e328efea4ed60d232d778))
* typo in Bearer ([754b90a](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/754b90a9fa90a308584949a6bf8085b6b27d8a19))
* Typo in Bearer ([#102](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/102)) ([3765c68](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/3765c689fcadc29f663c0521c3b9cc072ee8e779))


### Features

* add Eclipse Copyright header to CHANGELOG.md.jinja ([dd53533](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/dd53533bc6ccb9a160e4c51c46438321ed2b2be7))
* add GH Action workflow to test the app (incl. coverage) ([1e16b04](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/1e16b0418825ea402dd4a690d760b367a5c0ce8b))
* BPN validation added in issue dismantler VC ([5a04a2e](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/5a04a2ef41e48feb42c2db5baa54ea0e54833a3f))
* **ci:** semantic releases from develop branch ([#87](https://github.com/eclipse-tractusx/managed-identity-wallet/issues/87)) ([fda8ee6](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/fda8ee6ae864aa86823a7ebfdce9702f372a9ded))
* error msg updated ([2b305d2](https://github.com/eclipse-tractusx/managed-identity-wallet/commit/2b305d2d488a7ed1c63022ff4241ad5fa588822d))

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

<!-- insertion marker -->
## [V0.1.1] - 2023-09-06

### Known Knowns

- By default the role "view_wallets" is exclusively foreseen for the issuer/authority. In case of an unintended
  assignment of this role to any technical user, those are able to see the list of existing wallets with the current
  code version. A suitable fix with a second validation step will be provided with the next update of MIW.

### Security

- update spring-boot to version 3.1.2, updating transitive dependency spring-security-core to 6.1.2 (by Boris Rizov)

## [managed-identity-wallet-0.1.0-rc.3](https://github.com/pmoscode/managed-identity-wallet/releases/tag/managed-identity-wallet-0.1.0-rc.3) - 2023-08-28

<small>[Compare with managed-identity-wallet-0.1.0-rc.2](https://github.com/pmoscode/managed-identity-wallet/compare/managed-identity-wallet-0.1.0-rc.2...managed-identity-wallet-0.1.0-rc.3)</small>

### Features

- adds gradle task to equip 'jar/META-INF/' with 'DEPENDENCIES', 'SECURITY.md', 'NOTICE.md', 'LICENSE' (by Mathias Knoop).

### Bug Fixes

- tests fail randomly because of KeyGenerator (by Boris Rizov).

## [managed-identity-wallet-0.1.0-rc.2](https://github.com/pmoscode/managed-identity-wallet/releases/tag/managed-identity-wallet-0.1.0-rc.2) - 2023-08-25

<small>[Compare with managed-identity-wallet-1.1.0](https://github.com/pmoscode/managed-identity-wallet/compare/managed-identity-wallet-1.1.0...managed-identity-wallet-0.1.0-rc.2)</small>

### Bug Fixes

- remove out-of-date deployment workflow (by Sebastian Bezold).

## [managed-identity-wallet-1.1.0](https://github.com/pmoscode/managed-identity-wallet/releases/tag/managed-identity-wallet-1.1.0) - 2023-08-23

<small>[Compare with first commit](https://github.com/pmoscode/managed-identity-wallet/compare/060340e0f43f6bd2616afc1d3589c12bb1a5ffe6...managed-identity-wallet-1.1.0)</small>

### Features

- did document resolver component changes reverted (by Nitin Vavdiya).
- add docker hub release workflow (by Peter Motzko).
- add trigger on push (by Peter Motzko).
- adds license file 'CC-BY-4.0.tyt' in the corresponding directory 'LICENSES'. (by Mathias Knoop).
- ssi-lib version set to 15 (by Nitin Vavdiya).
- removed endpoint from config file (by Ronak Thacker).
- CGD-444: sql migration added (by Nitin Vavdiya).
- CGD-444-add-random-key-reference-to-did (by Nitin Vavdiya).
- validate issuer bpn while creating new wallet (by Ronak Thacker).
- add --wait to helm install (by Peter Motzko).
- add external chart repos (by Peter Motzko).
- update Chart.lock (by Peter Motzko).
- separate linting and testing and build temporary image to deploy on cluster (by Peter Motzko).
- update deprecated code (by Peter Motzko).
- enable manual trigger (by Peter Motzko).
- encryption key is now always stored in secret (by Dominik Pinsel).
- remove anchors from values.yaml (by Dominik Pinsel).
- generate authority wallet DID out of BPN and host (by Dominik Pinsel).
- set default log level (by Dominik Pinsel).
- set default database encryption key to random value (by Dominik Pinsel).
- set vc expiry date default to end of year (by Dominik Pinsel).
- make chart standalone runnable (by Dominik Pinsel).
- CGD-204: changes related to did document resolver in ssi-lib (by Nitin Vavdiya).
- add healthcheck for use with docker, compose or swarm (by Peter Motzko).
- add documentation for env / secret variables (by Peter Motzko).
- adds overview of roles associated with available endpoints (by Mathias Knoop).
- add documentation for new content and adjust the old one (by Peter Motzko).
- add additional unittests (by Peter Motzko).
- add internal / external postgresql switch (by Peter Motzko).
- adds overview of the required roles per endpoint (by Mathias Knoop).
- add task for generating CHANGELOG.md file (by Peter Motzko).
- add GH Action for DAST scanning (by Peter Motzko).
- update Helm chart Readme and corresponding Readme-template (by Peter Motzko).
- update Helm chart description (by Peter Motzko).
- add helm-docs documentation (by Peter Motzko).
- remove predefined annotation for ingress (by Peter Motzko).
- add one more folder depth to helm unittest in Taskfile (by Peter Motzko).
- add helm values for local deployment (by Peter Motzko).
- add simple backup of database to a PV (by Peter Motzko).
- updated swagger doc (by Ronak Thacker).
- updated swagger doc response sample and env file (by Ronak Thacker).
- add tests for env and secrets in values (by Peter Motzko).
- swagger doc request example updated (by Ronak Thacker).
- test case added for VC expiry check while VP validate as JWT (by Ronak Thacker).
- added end user documentation (by Ronak Thacker).
- added administrator documentation (by Ronak Thacker).
- adjust trivy GH Action (by Peter Motzko).
- remove manual trigger from veracode.yaml (by Peter Motzko).
- update KICS.yaml to fit current application (by Peter Motzko).
- update README.md by using current helm-docs template (by Peter Motzko).
- add Helm chart Readme template for helm-docs (by Peter Motzko).
- update chart description and add homepage link and keywords (by Peter Motzko).
- check expiry of VC while VP validate support added and test cases updated (by Ronak Thacker).
- updated context url of did document and test case (by Ronak Thacker).
- Extend Mac user information (by Peter Motzko).
- CGD-347: manage log level at runtime (by Nitin Vavdiya).
- Uncomment KC_HOSTNAME in env.docker.dist and env.local.dist (is required, not optional) (by Peter Motzko).
- Add DEV_ENVIRONMENT variable to env.docker.dist and env.local.dist (by Peter Motzko).
- CGD-368: Check expiry date of VC during validation (by Nitin Vavdiya).
- replace check for "docker compose" plugin (by Peter Motzko).
- remove docker compose selection -> fixed to new V2 version (by Peter Motzko).
- mark important content in README.md (by Peter Motzko).
- document COMPOSE_COMMAND env (by Peter Motzko).
- use COMPOSE_COMMAND env from env.* to execute the available docker compose binary (by Peter Motzko).
- add COMPOSE_COMMAND to env.*.dist to configure the available docker compose binary (by Peter Motzko).
- run Helm unittests (by Peter Motzko).
- suppress task header in output (by Peter Motzko).
- add missing tasks and rename working dir (by Peter Motzko).
- show install link provided as parameter (by Peter Motzko).
- add default labels to secret (by Peter Motzko).
- add tests for dev stage and put tests in subfolder for each stage (by Peter Motzko).
- put Helm chart unittests in subfolder for each stage (by Peter Motzko).
- update readme (by Peter Motzko).
- add tasks for test-report and coverage (by Peter Motzko).
- add task to check, if helm-docs is installed (by Peter Motzko).
- add task to rebuild Helm chart readme (by Peter Motzko).
- Update readme (by Peter Motzko).
- swagger doc updated as per new role in api and Retrieve wallet by identifier test cases updated (by Ronak Thacker).
- make Taskfile app namespace OS specific (by Peter Motzko).
- add environment type switch (by Peter Motzko).
- update .gitignore (by Peter Motzko).
- moved to dev-assets folder (by Peter Motzko).
- add realm for local dev config (by Peter Motzko).
- env-files move to dev folder (by Peter Motzko).
- script to obtain an authority token from keycloak (by Peter Motzko).
- rename tasks and point to new dev-env (by Peter Motzko).
- add new dev environment (by Peter Motzko).
- split tasks to own namespaces (by Peter Motzko).
- increment appVersion to 3.3.3.b420443 as an attempt (by Mathias Knoop).
- increment app version to '1.0.0-rc1' and chart version to '4.0.1-rc1' (by Mathias Knoop).
- json web signature and key generation from lib (by Nitin Vavdiya).
- fallthrough in check-prerequisites task (by Peter Motzko).
- authenticate using client_id and claint_secret added in swagger UI (by Nitin Vavdiya).
- add Helm unittests for default values (by Peter Motzko).
- remove "dash" from template (by Peter Motzko).
- add eclipse copyright header everywhere (by Peter Motzko).
- adds direct '.java-version' from program 'jenv' to '.gitignore' to avoid that is version-controled. (by Mathias Knoop).
- add more cleanup commands (by Peter Motzko).
- include checks and new "local.env" + add check-prerequisites task (by Peter Motzko).
- rename tasks in Taskfile (by Peter Motzko).
- add "local.dev" to .gitignore (by Peter Motzko).
- adds (initially) a Postman-collection for testing MIW on stage 'dev'. (by Mathias Knoop).
- add securityContext (primary same user/group id as docker image) (by Peter Motzko).
- use user/group id > 10000 and rename "user" to "miw" (by Peter Motzko).
- back to default values formatting (with accurate values) (by Peter Motzko).
- summary VC context URL updated (by Nitin Vavdiya).
- json web signature 2020 support added for did document, vc and vp (by Ronak Thacker).
- add and adjust authority user's BPN and DID:WEB (by Mathias Knoop).
- json web signature 2020 support WIP (by Ronak Thacker).
- set health check actuator resp. liveliness probe endpoint to '/actuator/health/liveness' and readiness probe endpoint to '/actuator/health/readiness' both on port '8090' (by Mathias Knoop).
- updated vc type (by Ronak Thacker).
- adjust task 'stopDockerApp' to stop the actual container 'local_miw_app' (by Mathias Knoop).
- Holder identifier added in framework VC subject (by Nitin Vavdiya).
- framework VC name changes and test case changes (by Nitin Vavdiya).
- adding generated 'README.md' to accompany the charts for 'Managed-Identity-Wallet' as required. (by Mathias Knoop).
- add app setup for the stages 'dev' and 'int' in order to deploy the (new) 'Managed-Identity-Wallet' on these stages. (by Mathias Knoop).
- validation added in issue VC api for summary VC (by Nitin Vavdiya).
- change in create VP API, type and name removed from summary VC (by Nitin Vavdiya).
- revert renaming and postpone that for later. (by Mathias Knoop).
- add '/dev.env' to '.gitignore' to prevent it form being comitted/ pushed unwanted. (by Mathias Knoop).
- rename tasks from beginning with 'run' to beginning with 'start' to achieve a uniform 'start...' and 'stop...' appearance of task names. (by Mathias Knoop).
- updated create wallet api (by Ronak Thacker).
- updated store credential api (by Ronak Thacker).
- summery VC flow after holder delete summary VC (by Nitin Vavdiya).
- Paggination support added in list VC API (by Nitin Vavdiya).
- summary credential test cases added (by Ronak Thacker).
- Summary VC flow, code changes as per input given in code review, test case changes as per summary VC flow (by Nitin Vavdiya).
- updated issuer credential api and test cases as per new api spec (by Ronak Thacker).
- Issuer get credential API testcases, test case modification for self_issued and is_stored (by Nitin Vavdiya).
- Testcase modification as per new API specification (by Nitin Vavdiya).
- delete credential api and test cases added (by Ronak Thacker).
- API changes as per new API specification(separate apis for holder and issuer wallet) (by Nitin Vavdiya).
- validate VP and validate VC test cases updated with mock (by Nitin Vavdiya).
- validate VP as jwt API, enforce https while did resolve in config, changes according to new lib (by Nitin Vavdiya).
- update lib method to resolved vc/vp validate (by Ronak Thacker).
- updated credential get api (by Ronak Thacker).
- token support added in swagger (by Nitin Vavdiya).
- test case of validate vc wip (by Ronak Thacker).
- credential get api type filter support added (by Ronak Thacker).
- shorten enc_key to 32 bytes (by Peter Motzko).
- extend application name (by Peter Motzko).
- add authority_wallet_did env (by Peter Motzko).
- WIP: add more stuff to Taskfile (by Peter Motzko).
- set ingress host type to "ImplementationSpecific" (by Peter Motzko).
- add classname annotation to ingress (by Peter Motzko).
- add init script to setup initial database (by Peter Motzko).
- read me file chamges, sample reponse added (by Nitin Vavdiya).
- set default values for dev environment in ArgoCD (by Peter Motzko).
- set default values for image tag and ingress className (by Peter Motzko).
- comment out default values (by Peter Motzko).
- comment out unused config (by Peter Motzko).
- add full release name to ingress secret name (by Peter Motzko).
- readm changes and gradle fix (by Nitin Vavdiya).
- update ditignore and dev.env added (by Nitin Vavdiya).
- readme added, validate VP wip (by Nitin Vavdiya).
- issue credential api test case added (by Ronak Thacker).
- add values for dev stage (WIP) (by Peter Motzko).
- remove unused secrets (by Peter Motzko).
- add container env (plain and from secret) (by Peter Motzko).
- add container env and DB secrets (by Peter Motzko).
- adjust to current setup (by Peter Motzko).
- rename miw service GH repo (by Peter Motzko).
- adjust Dockerfile to current setup (by Peter Motzko).
- change default name of miw-app artifact (by Peter Motzko).
- add local miw-app image to docker-compose (by Peter Motzko).
- credentials validation api wip (by Ronak Thacker).
- reamdme added with env verables, vp as jwt, credentials id added in table, search with credential id (by Nitin Vavdiya).
- caller BPN security added and test case modification for the same (by Nitin Vavdiya).
- remove acapy vars from .env.example (by Peter Motzko).
- change active Helm chart (by Peter Motzko).
- remove old acapy actions (by Peter Motzko).
- switch to new Helm chart "charts/managed-identity-wallet" (by Peter Motzko).
- add new helm chart "managed-identity-wallet" (WIP) (by Peter Motzko).
- added test cases for getCredentials api (by Ronak Thacker).
- code changes to create DidDocument (by Nitin Vavdiya).
- authority wallet did in env (by Nitin Vavdiya).
- presenation API WIP, code refactor (by Nitin Vavdiya).
- Create BPN VC while creating wallet, test case modification, filter support added in get all wallet and get all credential API (by Nitin Vavdiya).
- bpnCredential added in wallet creation (by Ronak Thacker).
- Framwork VC API, Dismantler VC API, Testcase modificatoin, VC context URL in config, VC expiry in config (by Nitin Vavdiya).
- test cases added for issueMembershipCredential api (by Ronak Thacker).
- membership credentials api added (by Ronak Thacker).
- ssi lib v4 added, autority wallet config added (by Nitin Vavdiya).
- Store credential API with test case, Validate test case (by Nitin Vavdiya).
- credential list api added (by Ronak Thacker).
- Store credntial API, testcase mofitication based on DidDocument Java POJO (by Nitin Vavdiya).
- ssi:lib version updated (by Ronak Thacker).
- Spring security added with keycloak, Swagger added, Testcases modification based on spring security (by Nitin Vavdiya).
- Resolve the DID document for a given DID or BPN api added (by Ronak Thacker).
- disable authorization check, because of potential bug (to be investigated) (by Peter Motzko).
- add console logger to db init transaction (commented out, but useful for debugging) (by Peter Motzko).
- adjust naming to snake_case and add suffixes (by Peter Motzko).
- Bump Keycloak version of docker-compose to 21.1 and update env file (by Peter Motzko).
- Add first helm unittest draft for Deployment resource (by Peter Motzko).
- Introduce Taskfile (by Peter Motzko).
- Replace old custom Dockerfile with official postgres image (by Peter Motzko).
- Remove obsolete stuff (by Peter Motzko).

### Bug Fixes

- update README.md and remove duplicate variable (by Peter Motzko).
- adjust test (by Peter Motzko).
- use user/group 11111 instead of 1001/0 (by Peter Motzko).
- typo (by Peter Motzko).
- skip app tests (by Peter Motzko).
- add GITHUB_TOKEN variable before app build (by Peter Motzko).
- install java 17 (by Peter Motzko).
- build miw app (by Peter Motzko).
- install Taskfile (by Peter Motzko).
- quote python version (by Peter Motzko).
- add missing "quote" function and adjust tests (by Peter Motzko).
- fix suggested issues from PR (by Peter Motzko).
- update image repo (by Peter Motzko).
- remove trailing space (by Peter Motzko).
- add space (typo) (by Peter Motzko).
- remove duplicate env var (by Peter Motzko).
- removed recret from code (by Ronak Thacker).
- add empty object (by Peter Motzko).
- validation erorr message not shown in reponse, docs: CGD-391: sample repomse added in wallet APIs (by Nitin Vavdiya).
- Veracode finding for CVE-2023-24998 (by Nitin Vavdiya).
- test cases (by Nitin Vavdiya).
- veracode log issue (by Nitin Vavdiya).
- veracode log realted issue fix (by Nitin Vavdiya).
- veracode issues: Spring boot and other lib version update (by Nitin Vavdiya).
- fix test values for helm test in GH Actions (by Peter Motzko).
- adjust gradle build command and remove unused code (by Peter Motzko).
- missing add repo step from chart-releaser workflow (by Gábor Almádi).
- conflict resolved (by Nitin Vavdiya).
- replace docker_compose env var with real command (by Peter Motzko).
- split into two lines (by Peter Motzko).
- add missing "sudo" (by Peter Motzko).
- move information about docker for macos upwards (by Peter Motzko).
- add empty line at the bottom for Helm lint testing (by Peter Motzko).
- use jdk 17 instead of jdk 18 and remove the EXPOSE statement (by Peter Motzko).
- add install links to check-tools.yaml (by Peter Motzko).
- move APPLICATION_PORT env var outside if condition (by Peter Motzko).
- CGD-288 and code refactor (by Nitin Vavdiya).
- use "localhost" instead docker container name (by Peter Motzko).
- attempt to deploy recent code (by Mathias Knoop).
- adjust git history for Peter Motzko, peter.motzko@volkswagen.de (by Mathias Knoop).
- remove quotes (by Peter Motzko).
- quote numbers (by Peter Motzko).
- adjust resources and limits for 'managed-identity-wallets-new' in order to attain and retain app-health-status. (by Mathias Knoop).
- adjust values to enable sync again. (by Mathias Knoop).
- adjust values for timeouts and resources for 'liveness' and 'readiness' actuators to mitigate restarts to to cpu-throttling and respective memory peaks which would possibly yield a restart, too. (by Mathias Knoop).
- CGD-238: BPN from access_token ignore case (by Nitin Vavdiya).
- changes task name to 'build' from 'buildJar' and adjusts all occurrences in file 'Taskfile'. (by Mathias Knoop).
- remove token (already revoked) (by Peter Motzko).
- remove double className (by Peter Motzko).
- remove last slash (by Peter Motzko).
- set ingress host path  to "/" (by Peter Motzko).
- bind APPLICATION_PORT to 8080 (by Peter Motzko).
- add random generated encryption_key (by Peter Motzko).
- typo in db_host (by Peter Motzko).
- wrong data type in secret (by Peter Motzko).
- add correct service for postgresql db (by Peter Motzko).
- use lowercase letters for repository name (by Peter Motzko).
- default port for postgresDB (by Peter Motzko).

### Code Refactoring

- adjust values according to latest changes (by Peter Motzko).
- reorder postgres config and envs in deployment (by Peter Motzko).
- CGD-401 and CGD-399: logs added, xss protection added, removed secret from config file (by Nitin Vavdiya).
- rename tests suites (by Peter Motzko).
- move test template definition out to root (by Peter Motzko).
- remove manual trigger (by Peter Motzko).
- remove old MIW Helm chart (by Peter Motzko).
- use custom values.yaml (instead of values-dev.yaml) for testing and add more tests (by Peter Motzko).
- remove env.environment due to set env order of Taskfile (by Peter Motzko).
- reorder variables (by Peter Motzko).
- add missing task to linux tasks (by Peter Motzko).
- spilt tasks after os in folders (by Peter Motzko).
- move get_token.sh to scripts folder (by Peter Motzko).
- rename paths (by Peter Motzko).
- auth method removed, company name removed from sample data (by Nitin Vavdiya).
- allowedVehicleBrands optional while issue Dismantler VC (by Nitin Vavdiya).
- refactor Taskfile (by Peter Motzko).
- variable name change and minor refactor (by Nitin Vavdiya).
