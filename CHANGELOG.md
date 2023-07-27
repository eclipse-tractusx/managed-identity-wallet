# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

<!-- insertion marker -->
## Unreleased

<small>[Compare with latest](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.7.5...HEAD)</small>

### Features

- add task for generating CHANGELOG.md file ([0ba1142](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0ba1142069bf2abd85f90aaa12a7f2f9d371a89d) by Peter Motzko).
- add GH Action for DAST scanning ([8ff4e1a](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/8ff4e1abf37334ead3e4fd14a3df7aead1f43ec4) by Peter Motzko).
- update Helm chart Readme and corresponding Readme-template ([ada59c2](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ada59c2ee2fa6744afe1a2b759644589b1c700bd) by Peter Motzko).
- update Helm chart description ([73235a8](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/73235a88b1ba48a1880dc74554b3cfe71b614650) by Peter Motzko).
- add helm-docs documentation ([24c0fcd](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/24c0fcd63cc33b6255c51abcab3fc157f814bb1f) by Peter Motzko).
- remove predefined annotation for ingress ([2d8d81b](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/2d8d81bc025d3f4921e333cf229502046945ea94) by Peter Motzko).
- add one more folder depth to helm unittest in Taskfile ([967afc1](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/967afc1c0216951a9635185dca1f8c38c4e66100) by Peter Motzko).
- add helm values for local deployment ([895c506](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/895c506f941a4d658c6fcb092fa04494f042d0a5) by Peter Motzko).
- add simple backup of database to a PV ([680e09f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/680e09fd18bf95518244f0a92c181d0264837aa6) by Peter Motzko).
- add tests for env and secrets in values ([e7a17b4](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e7a17b4b4014cd0e518b78dc88358b62b0caabbc) by Peter Motzko).
- added end user documentation ([83ae886](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/83ae88672a1dd28c31ca987e73ef84fd85fa366f) by Ronak Thacker).
- added administrator documentation ([1931fa0](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1931fa0a8f1afd01249dbcbeecfa09b1cb739f83) by Ronak Thacker).
- adjust trivy GH Action ([ee2f548](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ee2f54884289ad308c87b7e93b5be0c739ed55bd) by Peter Motzko).
- remove manual trigger from veracode.yaml ([fab2f10](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/fab2f1075b339933152c78ac09ec43bc2a284c71) by Peter Motzko).
- update KICS.yaml to fit current application ([af214cd](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/af214cd80cc30f9cd0e0d0b40748ca0fdaf5b1df) by Peter Motzko).
- update README.md by using current helm-docs template ([74e8bf9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/74e8bf9cfa60d035f2baf4fafe3e3b972e2e485f) by Peter Motzko).
- add Helm chart Readme template for helm-docs ([a96ca4c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a96ca4c6ace3c47f051007b8ae7b04c2b670adf1) by Peter Motzko).
- update chart description and add homepage link and keywords ([5b7b6b4](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/5b7b6b484f83462469b71dc931ff9291492b7f7b) by Peter Motzko).
- check expiry of VC while VP validate support added and test cases updated ([3c1d965](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3c1d965f913b1da7f0f5d58c99e89ea36c58fffc) by Ronak Thacker).
- updated context url of did document and test case ([0b05e15](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0b05e15bfd9e366d80c9004f0b3e9b5aea255438) by Ronak Thacker).
- Extend Mac user information ([403362d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/403362d2e7cb4f4e87b3b940415fb3d88dfab2f0) by Peter Motzko).
- CGD-347: manage log level at runtime ([1676bc3](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1676bc311f005f5207241d3138d690f05fb19131) by Nitin Vavdiya).
- Uncomment KC_HOSTNAME in env.docker.dist and env.local.dist (is required, not optional) ([aaeefc6](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/aaeefc61546d4ed3fa6fe0c91527e73d17068f45) by Peter Motzko).
- Add DEV_ENVIRONMENT variable to env.docker.dist and env.local.dist ([a0ed2a4](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a0ed2a4d7a51b14e3c22d1b6740c9dbebe648a49) by Peter Motzko).
- CGD-368: Check expiry date of VC during validation ([37bee28](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/37bee283777f081b82120cf6437da19260f901b3) by Nitin Vavdiya).
- replace check for "docker compose" plugin ([d75c719](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/d75c71903f69f9f62dad4ae2aa97e7be2fc525d4) by Peter Motzko).
- remove docker compose selection -> fixed to new V2 version ([aab42bc](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/aab42bc1590e62f0410c789da435614c127921a2) by Peter Motzko).
- mark important content in README.md ([43dec2d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/43dec2d1918296a17117070bb4e4c6d279bcc71f) by Peter Motzko).
- document COMPOSE_COMMAND env ([a6fc918](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a6fc91812ad9065c1f5011597deb1bc6fd9b545a) by Peter Motzko).
- use COMPOSE_COMMAND env from env.* to execute the available docker compose binary ([931e69f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/931e69fd02a0ada7c5b0827eaaf5aa140b5f07af) by Peter Motzko).
- add COMPOSE_COMMAND to env.*.dist to configure the available docker compose binary ([1893a64](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1893a6484bd0150d7d2ac3e5cb2e45e221403b2f) by Peter Motzko).
- run Helm unittests ([f11f824](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f11f8246da65fa17b0a4f4fac59ae67e42b709de) by Peter Motzko).
- suppress task header in output ([87aff68](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/87aff685c7b6db40cd701d3b7f51101e4b02e0c4) by Peter Motzko).
- add missing tasks and rename working dir ([27c2b03](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/27c2b03325004932c317c96989733c0a88ef4785) by Peter Motzko).
- show install link provided as parameter ([cb7ae41](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/cb7ae41b21e6a3acbafcf98dcc09000514b298d1) by Peter Motzko).
- add default labels to secret ([dfe107d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/dfe107dcd54fd44ad87e56ab799ee78a7f39a0ec) by Peter Motzko).
- add tests for dev stage and put tests in subfolder for each stage ([ecbe7e5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ecbe7e5d9765c7336914801001444905d8f84529) by Peter Motzko).
- put Helm chart unittests in subfolder for each stage ([3e17880](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3e17880beb79fc50f094a79cd49f47c06dbdc4b2) by Peter Motzko).
- update readme ([74fdb21](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/74fdb21dc549ed80bc1cbf8f71b40e0a9410a834) by Peter Motzko).
- add tasks for test-report and coverage ([b4ceeb5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b4ceeb5b8325a1b573ec7d6f9fd671e1e9cc41f1) by Peter Motzko).
- add task to check, if helm-docs is installed ([d559cdc](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/d559cdcb87edc427bbb46a80769dfed3137216d4) by Peter Motzko).
- add task to rebuild Helm chart readme ([881e131](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/881e131008cd358a376df62a72107711620c2845) by Peter Motzko).
- Update readme ([81e58df](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/81e58dfcbcdee82b1554c770d98f107875d3cf04) by Peter Motzko).
- swagger doc updated as per new role in api and Retrieve wallet by identifier test cases updated ([330d207](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/330d207e239004340465ecfb34c606a658616c26) by Ronak Thacker).
- make Taskfile app namespace OS specific ([6245576](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/62455766d2ea7ab9da95af6948a62cb455fbc623) by Peter Motzko).
- add environment type switch ([4a5c128](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/4a5c1287c3396ac9ee42e985db67579ab75bccc8) by Peter Motzko).
- update .gitignore ([33b7262](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/33b726263658687608e75379e8cfe1236fda05ca) by Peter Motzko).
- moved to dev-assets folder ([a70115c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a70115c1d1243908ccc16d2fe2c3bbefd0070d8a) by Peter Motzko).
- add realm for local dev config ([56a67b7](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/56a67b742fbb1dc12806459ba72778f525fad966) by Peter Motzko).
- env-files move to dev folder ([660cac9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/660cac957dd683ed6211653ca552f1320ab7f3f7) by Peter Motzko).
- script to obtain an authority token from keycloak ([65b8b20](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/65b8b20a24ac6cbce7572f173844d21d4bff0bfc) by Peter Motzko).
- rename tasks and point to new dev-env ([62e0f2b](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/62e0f2b06c75a71e65e2e1dfeb16666cbb826b97) by Peter Motzko).
- add new dev environment ([0858bf9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0858bf9611162f1055b7b29d20494685878aab39) by Peter Motzko).
- split tasks to own namespaces ([db0151c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/db0151c68fb208dd3981d271277490f48ccdca7a) by Peter Motzko).
- increment appVersion to 3.3.3.b420443 as an attempt ([b75ebaf](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b75ebafd5395492116d99eca4de6f496cd06ff9f) by Mathias Knoop).
- increment app version to '1.0.0-rc1' and chart version to '4.0.1-rc1' ([07e21cf](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/07e21cfa048fa445fbb4895a0c932fbbd399ac51) by Mathias Knoop).
- json web signature and key generation from lib ([8fda456](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/8fda456a59e011f250677f2e929e6ffb14d1f5f9) by Nitin Vavdiya).
- fallthrough in check-prerequisites task ([28801cc](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/28801cc3996f9dc9f699326285bed538f9d9ba6f) by Peter Motzko).
- authenticate using client_id and claint_secret added in swagger UI ([aaa0a8f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/aaa0a8f5efd5c7282579d42c51677e5816a9c6a6) by Nitin Vavdiya).
- add Helm unittests for default values ([cc6d2bf](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/cc6d2bf32f711dd0b745e9e24c7bc73cb02cb5b3) by Peter Motzko).
- remove "dash" from template ([7b5bca9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7b5bca9e3261ddc49605799c68fac32dc1e353ce) by Peter Motzko).
- add eclipse copyright header everywhere ([ad5c631](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ad5c631fa2bc3ce22415d215f2cb7207feee014a) by Peter Motzko).
- adds direct '.java-version' from program 'jenv' to '.gitignore' to avoid that is version-controled. ([3531dd5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3531dd527b1d0f4306a466a4b59d25fa49872470) by Mathias Knoop).
- add more cleanup commands ([c18b1b0](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/c18b1b079929a82d22e302ec641bb1107f7cf718) by Peter Motzko).
- include checks and new "local.env" + add check-prerequisites task ([eb277ff](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/eb277ffdb1605a1650d08ac340140beaa1246b0d) by Peter Motzko).
- rename tasks in Taskfile ([cdde677](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/cdde677a6f5a594d65633ebd0b472dddf21dfea4) by Peter Motzko).
- add "local.dev" to .gitignore ([0c4166d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0c4166d1ce7cd4521e0fc7980c1a73c70e57245e) by Peter Motzko).
- adds (initially) a Postman-collection for testing MIW on stage 'dev'. ([25debc3](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/25debc365e18ca6f3f0eb0e864539ce0352e3807) by Mathias Knoop).
- add securityContext (primary same user/group id as docker image) ([25e93fa](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/25e93fa50652ced6b68b2e6e8fc18e4e5fdbcfe0) by Peter Motzko).
- use user/group id > 10000 and rename "user" to "miw" ([4b08914](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/4b08914d48ed598178db1cbaff6bfac8e008485b) by Peter Motzko).
- back to default values formatting (with accurate values) ([b0e3c61](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b0e3c6192f358dd3f88a72b05365dc76601b4a4d) by Peter Motzko).
- summary VC context URL updated ([0cc6bfb](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0cc6bfbd61d845689d6b0fe56214fd03b98bf0e6) by Nitin Vavdiya).
- json web signature 2020 support added for did document, vc and vp ([a9838ce](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a9838ce167a7f2d6a92849de66c8242e093a93b4) by Ronak Thacker).
- add and adjust authority user's BPN and DID:WEB ([7e2ab46](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7e2ab464c81f9b90d234ce963f204c9d5e46f100) by Mathias Knoop).
- json web signature 2020 support WIP ([62ee442](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/62ee442e799e59df0ed70925b71e419413a0bc9a) by Ronak Thacker).
- set health check actuator resp. liveliness probe endpoint to '/actuator/health/liveness' and readiness probe endpoint to '/actuator/health/readiness' both on port '8090' ([af44016](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/af44016dc3e04d5e7e1448007cb73a429c529de9) by Mathias Knoop).
- updated vc type ([8b6ebfa](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/8b6ebfadbddabe9342b227e8043be110f7ae1ae1) by Ronak Thacker).
- adjust task 'stopDockerApp' to stop the actual container 'local_miw_app' ([2fb0000](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/2fb0000162383dd9e8947e5f7d2ef985c66aa319) by Mathias Knoop).
- Holder identifier added in framework VC subject ([e65af19](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e65af19f777154e7f2bc69f90e40e32feef52dbf) by Nitin Vavdiya).
- framework VC name changes and test case changes ([7952f4b](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7952f4b1ef94ec144308683607bf5083226da523) by Nitin Vavdiya).
- adding generated 'README.md' to accompany the charts for 'Managed-Identity-Wallet' as required. ([d33f6aa](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/d33f6aa50f5f7cb0276c1e6391d85a4eae793c87) by Mathias Knoop).
- add app setup for the stages 'dev' and 'int' in order to deploy the (new) 'Managed-Identity-Wallet' on these stages. ([3f8be32](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3f8be327f44c44e58fe3fd08a6e2853ba8e254d7) by Mathias Knoop).
- validation added in issue VC api for summary VC ([97ac189](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/97ac189d1e2ef13da12432c2e340d5b9140fa8cf) by Nitin Vavdiya).
- change in create VP API, type and name removed from summary VC ([d4909b0](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/d4909b05f05d4141616abb22acbb31e1d1c4a5ec) by Nitin Vavdiya).
- revert renaming and postpone that for later. ([3a63399](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3a63399e80220bca2de0ed3bdcaf3b453b68c8fa) by Mathias Knoop).
- add '/dev.env' to '.gitignore' to prevent it form being comitted/ pushed unwanted. ([ac09710](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ac09710204a9c123c66ae2bcd5a5d668a4a84de9) by Mathias Knoop).
- rename tasks from beginning with 'run' to beginning with 'start' to achieve a uniform 'start...' and 'stop...' appearance of task names. ([a3f3f1c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a3f3f1c4f9450cc436537672dc439a5e598ea0b5) by Mathias Knoop).
- updated create wallet api ([ba320ab](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ba320ab31fbed0c833ffb8462b0b39c4a5ab3648) by Ronak Thacker).
- updated store credential api ([74df138](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/74df138aa01417887e2bd439178f1e80b7be840a) by Ronak Thacker).
- summery VC flow after holder delete summary VC ([1b61f38](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1b61f38c721ef961e4f6d5c97555a3dc49c1668d) by Nitin Vavdiya).
- Paggination support added in list VC API ([f4338fd](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f4338fd1711e66b64db03f36f0a61c9d189ed70d) by Nitin Vavdiya).
- summary credential test cases added ([b322515](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b322515b8b5ed1f9fa10f7cd0138820ba15a7e6e) by Ronak Thacker).
- Summary VC flow, code changes as per input given in code review, test case changes as per summary VC flow ([07c4706](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/07c4706a8beb85313f325317e5feb9dca6bfff17) by Nitin Vavdiya).
- updated issuer credential api and test cases as per new api spec ([f33b056](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f33b056c41584e65492b028cebb7f4a9f361d01d) by Ronak Thacker).
- Issuer get credential API testcases, test case modification for self_issued and is_stored ([bafaab9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/bafaab91d67ef1d85229f786500243dc22ef4d33) by Nitin Vavdiya).
- Testcase modification as per new API specification ([f29edb3](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f29edb30ff9c651be10dc082ef6454fd293e7177) by Nitin Vavdiya).
- delete credential api and test cases added ([e56a52e](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e56a52e5218c9d07d738f590b2aeea01b9f33202) by Ronak Thacker).
- API changes as per new API specification(separate apis for holder and issuer wallet) ([6286538](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/628653865b0a4db9193fde1d07e5e946df05160c) by Nitin Vavdiya).
- validate VP and validate VC test cases updated with mock ([7531191](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7531191afc62fba81d16aeef26b2ff0a84f20fd8) by Nitin Vavdiya).
- validate VP as jwt API, enforce https while did resolve in config, changes according to new lib ([235420f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/235420fcd3eed36e575c10951bb067b0bb530e9a) by Nitin Vavdiya).
- update lib method to resolved vc/vp validate ([6f70b09](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6f70b09523bed428472ed61c0c88964473a243b5) by Ronak Thacker).
- updated credential get api ([1698996](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/16989967d67160ffbff6a0b9eae4d616634fd456) by Ronak Thacker).
- token support added in swagger ([cd74001](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/cd74001c323a54c4dcc9094daca42614c5826ff6) by Nitin Vavdiya).
- test case of validate vc wip ([2123468](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/21234689065a7a39fc45a6d87ce40e7dc79e3ff2) by Ronak Thacker).
- credential get api type filter support added ([9d6a49d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/9d6a49daab04bda7fe06484ec1e827a534addbd9) by Ronak Thacker).
- shorten enc_key to 32 bytes ([a52e2ce](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a52e2cec45befdb699fe4c90a29be8eb464eb6e1) by Peter Motzko).
- extend application name ([5aca6f2](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/5aca6f2a1063830373faa52fd5ee5673cf3860ab) by Peter Motzko).
- add authority_wallet_did env ([ce2e531](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ce2e531e7fac46b23d656823e1ea11cb1396359d) by Peter Motzko).
- WIP: add more stuff to Taskfile ([280f51c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/280f51cb4d7050a87f4faca547c45d05eacc6089) by Peter Motzko).
- set ingress host type to "ImplementationSpecific" ([8e40f3a](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/8e40f3a0b32473bd5501fde76c3f3a837354c9ce) by Peter Motzko).
- add classname annotation to ingress ([d250497](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/d25049790c2d10c8be784a953426a872adf88998) by Peter Motzko).
- add init script to setup initial database ([9950690](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/99506905c8e9a53bccc28cd7f7ecc752179c1112) by Peter Motzko).
- read me file chamges, sample reponse added ([88651fa](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/88651fa518fdcabf5ab1902d775d748a701df46e) by Nitin Vavdiya).
- set default values for dev environment in ArgoCD ([2d10bcf](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/2d10bcfe7e5159573d275f42556bd852eda7df07) by Peter Motzko).
- set default values for image tag and ingress className ([f8310e6](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f8310e6b65682ffff9aa5d82641aced98ee933bb) by Peter Motzko).
- comment out default values ([d424086](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/d4240868609392558bc3cb682b9e3747ec7cc59d) by Peter Motzko).
- comment out unused config ([248ab4c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/248ab4cede85449e1028bc8aa00f4b8b5e3d9636) by Peter Motzko).
- add full release name to ingress secret name ([58a7299](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/58a72999f8229ddc39dd76442f4e8754b47e7720) by Peter Motzko).
- readm changes and gradle fix ([79d5bf0](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/79d5bf04488477296c07c151b455623d7b023e58) by Nitin Vavdiya).
- update ditignore and dev.env added ([b90fe94](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b90fe944a6f447e54349ff1ef99298d7fa9f2468) by Nitin Vavdiya).
- readme added, validate VP wip ([1c4d6af](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1c4d6af2642b81e94430614ceb9ec4c2c3497da9) by Nitin Vavdiya).
- issue credential api test case added ([6edf1e6](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6edf1e635c4bd82536a9a76bf88153a73bcb6a95) by Ronak Thacker).
- add values for dev stage (WIP) ([bb2c6a5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/bb2c6a5be738d042510f00a62947df303ea58bb6) by Peter Motzko).
- remove unused secrets ([fa21e4d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/fa21e4d536ab6c7bf62a97ee3188b3873e24592a) by Peter Motzko).
- add container env (plain and from secret) ([7242b4b](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7242b4b78834a0965d756f416ef0ac0068ca22a3) by Peter Motzko).
- add container env and DB secrets ([f175786](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f1757861ff2d204e0f3e98035f4fdc10c247c2cb) by Peter Motzko).
- adjust to current setup ([ee92bf5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ee92bf5f5519d5e7759351d7a04962c08fceae79) by Peter Motzko).
- rename miw service GH repo ([ab9c7ff](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ab9c7ffb2632ad7621e010917306997589100599) by Peter Motzko).
- adjust Dockerfile to current setup ([8556169](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/85561698505ef5d72a76314dbb8d69223e5e57cb) by Peter Motzko).
- change default name of miw-app artifact ([29e65af](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/29e65afbc1b0e9ec941e8e516a0e8a8b292eb12a) by Peter Motzko).
- add local miw-app image to docker-compose ([21ac5e8](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/21ac5e880bd5fff9ad6c478986aecc02e36e4069) by Peter Motzko).
- credentials validation api wip ([0515ef6](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0515ef6244d52bea506363540593489bee48d165) by Ronak Thacker).
- reamdme added with env verables, vp as jwt, credentials id added in table, search with credential id ([524c537](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/524c5370aabd95e95b9c23eb902543394a0dbbb2) by Nitin Vavdiya).
- caller BPN security added and test case modification for the same ([b375317](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b3753173eb2eb90474c6a9e92d7465203ddc17c3) by Nitin Vavdiya).
- remove acapy vars from .env.example ([3ce0c6d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3ce0c6d21c735209503e17025089e4b024050a32) by Peter Motzko).
- change active Helm chart ([a5a88b5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a5a88b5e0482bd802e964661b41660bf0908141b) by Peter Motzko).
- remove old acapy actions ([e0f0ae7](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e0f0ae7b8a1560b458511d922bae1624c18294fb) by Peter Motzko).
- switch to new Helm chart "charts/managed-identity-wallet" ([c302664](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/c302664b96431373ef111361c1d994c27640fe98) by Peter Motzko).
- add new helm chart "managed-identity-wallet" (WIP) ([eac9f3a](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/eac9f3addc4edb60ffe93d58bed4db5816ab0252) by Peter Motzko).
- added test cases for getCredentials api ([56072f6](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/56072f68d092b3ebf8cbef9647074e4b7d6f456a) by Ronak Thacker).
- code changes to create DidDocument ([1675ef4](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1675ef4d1f6b5cb32b80b856b39955383902f1f9) by Nitin Vavdiya).
- authority wallet did in env ([139f9d9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/139f9d95f116e02fc1ba91404d3b010fe0587386) by Nitin Vavdiya).
- presenation API WIP, code refactor ([2c1b5d9](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/2c1b5d9c3bc667d8f91d1f35145bd3fb9c0c38c0) by Nitin Vavdiya).
- Create BPN VC while creating wallet, test case modification, filter support added in get all wallet and get all credential API ([aa04faa](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/aa04faa2adf828bfbb712993af90926a397e4ff0) by Nitin Vavdiya).
- bpnCredential added in wallet creation ([67c749f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/67c749f98ff768deaea2f68a219916c46e0276e5) by Ronak Thacker).
- Framwork VC API, Dismantler VC API, Testcase modificatoin, VC context URL in config, VC expiry in config ([6ea7580](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6ea7580023ee6fef9d561558ab22eec79c27e242) by Nitin Vavdiya).
- test cases added for issueMembershipCredential api ([a3d19c1](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a3d19c15ad59c57cd06f18fa5630e9555d639d10) by Ronak Thacker).
- membership credentials api added ([45602c8](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/45602c8fe8db3a982ea9c644c180902ad512b75e) by Ronak Thacker).
- ssi lib v4 added, autority wallet config added ([1c5ee0e](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1c5ee0e116bbc0ad69525da8cc7a3fbdfa67da4a) by Nitin Vavdiya).
- Store credential API with test case, Validate test case ([4911817](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/4911817dfdf40038cc2298d62676d4e2fa25c2b2) by Nitin Vavdiya).
- credential list api added ([e34cf00](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e34cf0067d77401be28aa1e43ad9d118465e39a8) by Ronak Thacker).
- Store credntial API, testcase mofitication based on DidDocument Java POJO ([569097b](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/569097b392d0fb7242ed9df47fcaf5fda40904b0) by Nitin Vavdiya).
- ssi:lib version updated ([4939ddb](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/4939ddb2d9d4f4b982ffeb75279414b63d16a005) by Ronak Thacker).
- Spring security added with keycloak, Swagger added, Testcases modification based on spring security ([aed48ee](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/aed48ee4de89ab0df1d649260609db69264b6530) by Nitin Vavdiya).
- Resolve the DID document for a given DID or BPN api added ([1368edf](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1368edfb1cef1cac0640b599bd8ba76b7e2aa627) by Ronak Thacker).
- disable authorization check, because of potential bug (to be investigated) ([718af89](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/718af892e70eaf478e3001b14772f1ade4fc5747) by Peter Motzko).
- add console logger to db init transaction (commented out, but useful for debugging) ([6706f18](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6706f186ebb1343bf28dc8b59ff63a8a4d18fd7f) by Peter Motzko).
- adjust naming to snake_case and add suffixes ([720217f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/720217fdfa650e2785a92708df688d8738bf04bc) by Peter Motzko).
- Bump Keycloak version of docker-compose to 21.1 and update env file ([9b81087](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/9b81087ac2d811c67217adee06df57a7522770bf) by Peter Motzko).
- Add first helm unittest draft for Deployment resource ([b533b52](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b533b52460578872117c101c5a07c5d9cbb19f3e) by Peter Motzko).
- Introduce Taskfile ([5d4a889](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/5d4a88989ce8fc1dc93ea69a57ab4b3a8da3bdf6) by Peter Motzko).
- Replace old custom Dockerfile with official postgres image ([ce2c907](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ce2c907bb9e4c10a81da4652cdeb3c21a363381e) by Peter Motzko).
- Remove obsolete stuff ([c2ff738](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/c2ff738ab5f13de40734bfccc17fa10d59a57b39) by Peter Motzko).

### Bug Fixes

- add empty object ([00a9b08](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/00a9b08c1a9bd319e0ebe05734d26fae2209b8fe) by Peter Motzko).
- Veracode finding for CVE-2023-24998 ([5ac9f2d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/5ac9f2dfeb1b80afce33014eae9d592774ae3bf3) by Nitin Vavdiya).
- test cases ([1a9b51e](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/1a9b51ea8ef256b2b8275e7361fd54a0c2d09d90) by Nitin Vavdiya).
- veracode log issue ([69cd4d8](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/69cd4d83470faa2615b6128f416ea4f3a601f21e) by Nitin Vavdiya).
- veracode log realted issue fix ([7908741](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7908741e3a765bc68e9c955dfccc58e4b3f72da2) by Nitin Vavdiya).
- veracode issues: Spring boot and other lib version update ([f30c4d8](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f30c4d80ec70db4387dbb41e06d867579a4bdde6) by Nitin Vavdiya).
- fix test values for helm test in GH Actions ([4532aa7](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/4532aa7611c54270e20e2adec8be24cad21eed20) by Peter Motzko).
- adjust gradle build command and remove unused code ([590d56a](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/590d56a7d990799fa985f81447f3b4be6b35d653) by Peter Motzko).
- conflict resolved ([a2ce51f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/a2ce51fce616644364abc6348c34b1050ca33400) by Nitin Vavdiya).
- replace docker_compose env var with real command ([187435b](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/187435be54f66d88dda11c152c38205223791f38) by Peter Motzko).
- split into two lines ([0b2e11f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/0b2e11fb4a3d6f7b862f688da5c7f85b1ea9fe1e) by Peter Motzko).
- add missing "sudo" ([e912482](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e912482c8c87fa9d1f904ef26ed52d65810bf271) by Peter Motzko).
- move information about docker for macos upwards ([ff414f8](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ff414f8e3cb764ad1602f2b2367e2756d123cdd8) by Peter Motzko).
- add empty line at the bottom for Helm lint testing ([57a37f4](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/57a37f467d60cef5eb595911f101bc36050d924b) by Peter Motzko).
- use jdk 17 instead of jdk 18 and remove the EXPOSE statement ([e2ef3fe](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e2ef3fe536b8168a3806508c8c2da8f15cc5d247) by Peter Motzko).
- add install links to check-tools.yaml ([7c94f89](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7c94f8954fd2fb2020b444ea9466d8eef8325353) by Peter Motzko).
- move APPLICATION_PORT env var outside if condition ([299b669](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/299b669b4b71315cd9c7c19672d510ddb5727c65) by Peter Motzko).
- CGD-288 and code refactor ([f037c16](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f037c168a5a16aa3ff2009e7c086d82a8030bf44) by Nitin Vavdiya).
- use "localhost" instead docker container name ([c6caf36](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/c6caf36297582b0d1a6697f181b51f967d550cb1) by Peter Motzko).
- attempt to deploy recent code ([3b5e5f7](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3b5e5f7919674213b0f18374c4bd9715fbb418c8) by Mathias Knoop).
- adjust git history for Peter Motzko, peter.motzko@volkswagen.de ([06e4d83](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/06e4d838b9ccb5175ceac48547a70e9c18d62a7f) by Mathias Knoop).
- remove quotes ([8889455](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/8889455b67a6e88168b5bdc576393b758a638ffa) by Peter Motzko).
- quote numbers ([9a48104](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/9a4810452cf7a4187a8f4d9c33a7c7fbc346514e) by Peter Motzko).
- adjust resources and limits for 'managed-identity-wallets-new' in order to attain and retain app-health-status. ([b4e5ba5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b4e5ba51db62f2bc1b995b2a85111c6ab4b01aa4) by Mathias Knoop).
- adjust values to enable sync again. ([b856059](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b856059008fc62f565eb1b8b7c52354224384447) by Mathias Knoop).
- adjust values for timeouts and resources for 'liveness' and 'readiness' actuators to mitigate restarts to to cpu-throttling and respective memory peaks which would possibly yield a restart, too. ([f7673a0](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f7673a005f83b26418a9c82c45f8628713dfd98c) by Mathias Knoop).
- CGD-238: BPN from access_token ignore case ([f6bf0d5](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f6bf0d5bf08ed839f37b0b6a2cb09258c02b8cfd) by Nitin Vavdiya).
- changes task name to 'build' from 'buildJar' and adjusts all occurrences in file 'Taskfile'. ([458834c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/458834c9a0875990551dc553dd948e61f62e30f6) by Mathias Knoop).
- remove token (already revoked) ([835654d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/835654d9be8504a896f916ed928c5f2a6481793e) by Peter Motzko).
- remove double className ([e8e2a43](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e8e2a43301a62a8596a1415e62d0debab59bd502) by Peter Motzko).
- remove last slash ([dab469f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/dab469fe8f6d643284cd1f9d4691391026e5c482) by Peter Motzko).
- set ingress host path  to "/" ([3dcfd46](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3dcfd460d630f9893ebebc9c760c2a96cf1dabd6) by Peter Motzko).
- bind APPLICATION_PORT to 8080 ([9a9097d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/9a9097deb094ce868d071e40f3bbdac70897d376) by Peter Motzko).
- add random generated encryption_key ([6e89285](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6e89285a797fc90a9b2e9d2b48687c4188dd1fa2) by Peter Motzko).
- typo in db_host ([66daf19](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/66daf19f10b5654b9d7c60c5611a1fd03d023c21) by Peter Motzko).
- wrong data type in secret ([c600d16](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/c600d1648065130d3ab21b56148b3348debf82bf) by Peter Motzko).
- add correct service for postgresql db ([e00a84f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/e00a84f5aaa231465d733e41e911aa4ee8eef3c8) by Peter Motzko).
- use lowercase letters for repository name ([9e10c9f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/9e10c9f9dd8859b2eb2b8613369a15787b0cccd4) by Peter Motzko).
- default port for postgresDB ([6b603c0](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6b603c038c38b567e7f882b0eaefd03b093bf292) by Peter Motzko).

### Code Refactoring

- rename tests suites ([3304d0d](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/3304d0d91a339f47d9ed8e38e3e7b7c7413daf89) by Peter Motzko).
- move test template definition out to root ([2163f28](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/2163f283dc0f868a2ecf99557a545097569c178a) by Peter Motzko).
- remove manual trigger ([50d6370](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/50d6370628c4d22f9f42af7545fbf1a98f205ea7) by Peter Motzko).
- remove old MIW Helm chart ([6cd5d6c](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6cd5d6c6fd9e088f0f9b81a09cf1559477b1143b) by Peter Motzko).
- use custom values.yaml (instead of values-dev.yaml) for testing and add more tests ([37c9289](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/37c9289a054cc2e70a9169119e951986abd0b074) by Peter Motzko).
- remove env.environment due to set env order of Taskfile ([11ad959](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/11ad959c37f1a9d23f0b3d8fe119a7d85a73ccd8) by Peter Motzko).
- reorder variables ([7149df4](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/7149df4791460d95c5638b946b4fd6db2e782221) by Peter Motzko).
- add missing task to linux tasks ([b1e9c0e](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b1e9c0e80cf9a480b2b6b4b4cdec55146e72e65e) by Peter Motzko).
- spilt tasks after os in folders ([5ac1bd3](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/5ac1bd345f8e155317b810273eefc26d22bc5711) by Peter Motzko).
- move get_token.sh to scripts folder ([8bf92ed](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/8bf92edd11e7d0d513a4179a21d14d28971e6e6b) by Peter Motzko).
- rename paths ([ea18e72](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ea18e72a2e1266ea2aa141dd8a0b2531e4409c02) by Peter Motzko).
- auth method removed, company name removed from sample data ([6b6b80f](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/6b6b80fc2fe621ab0848142ae78a7f75a8e64e32) by Nitin Vavdiya).
- allowedVehicleBrands optional while issue Dismantler VC ([ba45aee](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/ba45aeeea26c9cafcb56847a7864b47a89ab0e84) by Nitin Vavdiya).
- refactor Taskfile ([b4f9f54](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/b4f9f54b14f0b5c8919626cd952348094c79160e) by Peter Motzko).
- variable name change and minor refactor ([f1f6a56](https://github.com/catenax-ng/tx-managed-identity-wallets/commit/f1f6a56928fb8612247356ab45acc73a379404d3) by Nitin Vavdiya).

<!-- insertion marker -->
## [managed-identity-wallets-0.7.5](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.7.5) - 2023-02-28

<small>[Compare with managed-identity-wallets-0.7.2](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.7.2...managed-identity-wallets-0.7.5)</small>

## [managed-identity-wallets-0.7.2](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.7.2) - 2023-02-24

<small>[Compare with managed-identity-wallets-0.7.1](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.7.1...managed-identity-wallets-0.7.2)</small>

## [managed-identity-wallets-0.7.1](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.7.1) - 2023-02-23

<small>[Compare with managed-identity-wallets-0.7.0](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.7.0...managed-identity-wallets-0.7.1)</small>

## [managed-identity-wallets-0.7.0](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.7.0) - 2023-02-13

<small>[Compare with managed-identity-wallets-0.6.9](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.6.9...managed-identity-wallets-0.7.0)</small>

## [managed-identity-wallets-0.6.9](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.6.9) - 2023-02-09

<small>[Compare with managed-identity-wallets-0.6.8](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.6.8...managed-identity-wallets-0.6.9)</small>

## [managed-identity-wallets-0.6.8](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.6.8) - 2023-02-06

<small>[Compare with managed-identity-wallets-0.6.7](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.6.7...managed-identity-wallets-0.6.8)</small>

## [managed-identity-wallets-0.6.7](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.6.7) - 2023-02-02

<small>[Compare with managed-identity-wallets-0.6.6](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.6.6...managed-identity-wallets-0.6.7)</small>

## [managed-identity-wallets-0.6.6](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.6.6) - 2023-02-02

<small>[Compare with managed-identity-wallets-0.6.3](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.6.3...managed-identity-wallets-0.6.6)</small>

## [managed-identity-wallets-0.6.3](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.6.3) - 2023-01-26

<small>[Compare with managed-identity-wallets-0.6.0](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/managed-identity-wallets-0.6.0...managed-identity-wallets-0.6.3)</small>

## [managed-identity-wallets-0.6.0](https://github.com/catenax-ng/tx-managed-identity-wallets/releases/tag/managed-identity-wallets-0.6.0) - 2023-01-17

<small>[Compare with first commit](https://github.com/catenax-ng/tx-managed-identity-wallets/compare/060340e0f43f6bd2616afc1d3589c12bb1a5ffe6...managed-identity-wallets-0.6.0)</small>

