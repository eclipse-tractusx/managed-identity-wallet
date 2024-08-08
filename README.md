The Managed Identity Wallets (MIW) service implements the Self-Sovereign-Identity (SSI) using `did:web`.

This is a gradle multi-module project containing two applications:

1. **miw**: This is a wallet application. Please refer [README.md](miw%2FREADME.md) for more information
2. **revocation-service**: This is verifiable credential revocation service. Please
   refer [README.md](revocation-service%2FREADME.md) for more information

# Committer Documentation

*(This section is also intentionally included in the CONTRIBUTING.md file)*

The project uses the tool `semantic-release` to automatically create releases
and manage CHANGELOG.md entries. These files **SHOULD** never be manually edited
nor present in any PR. If you see this file in a PR, it means the incoming branch
is not at the tip of the project history - it will most likely mangle your project
when merged.

You'll find all important steps in the files `.github/release.yaml` and `.releaserc`.

The development work is always done on branch `develop`, all pull requests are made
against `develop`. When it is time to create an official release a PR from `develop`
to `main` must be created. **IMPORTANT**: after merging, you **MUST** wait for the
pipeline to complete, as it will create two new commits on `main`. After that you
**MUST** create a PR, merging main back into develop, to obtain these two new commits,
and to kick-off the new tag on `develop`. Failing to do so will result in a huge
headache, spaghetti code, faulty commits and other "life-improving" moments. **DO NOT
MESS THIS STEP UP**.

It is possible to test how a release will work on your own fork, **BUT** you'll have
to do some extra work to make it happen. `semantic-release` uses git notes to track
the tags. You'll have to sync them manually (as most git configs do not include the settings
to do so automatically):

```shell
git fetch upstream refs/notes/*:refs/notes/*
git push origin --tags
git push origin refs/notes/*:refs/notes/*
```

At this point your repository will behave exactly like upstream when doing a release.

# Developer Documentation

For end-to-end testing both the application should be running.

### Common gradle task

1. Build both the application

``./gradlew clean build``

2. Run tests

``./gradlew clean test``
