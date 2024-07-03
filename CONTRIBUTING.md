# Contributing to Eclipse Tractus-X

Thanks for your interest in this project.

## Project description

The companies involved want to increase the automotive industry's
competitiveness, improve efficiency through industry-specific cooperation and
accelerate company processes through standardization and access to information
and data. A special focus is also on SMEs, whose active participation is of
central importance for the network's success. That is why Catena-X has been
conceived from the outset as an open network with solutions ready for SMEs,
where these companies will be able to participate quickly and with little IT
infrastructure investment. Tractus-X is meant to be the PoC project of the
Catena-X alliance focusing on parts traceability.

* https://projects.eclipse.org/projects/automotive.tractusx

## Project licenses

The Tractus-X project uses the following licenses:

* Apache-2.0 for code
* CC-BY-4.0 for non-code

## Terms of Use

This repository is subject to the Terms of Use of the Eclipse Foundation

* https://www.eclipse.org/legal/termsofuse.php

## Developer resources

Information regarding source code management, builds, coding standards, and
more.

* https://projects.eclipse.org/projects/automotive.tractusx/developer

Getting started:

* https://eclipse-tractusx.github.io/docs/developer

The project maintains the source code repositories in the following GitHub organization:

* https://github.com/eclipse-tractusx/

### How to submit pull requests

It is paramount to ensure that the git history of the project remains clean and
consistent. This means that the usage of concise and expressive commits **MUST**
be used. Other helpful tips are to always rebase your branch before submitting
the pull request.

First make sure you are working on your fork of the project, for example:

```shell
$ git remote show origin
* remote origin
Fetch URL: git@github.com:borisrizov-zf/managed-identity-wallet.git
Push  URL: git@github.com:borisrizov-zf/managed-identity-wallet.git
```

Make sure you setup a remote which points at the Tractus-X repository:

```shell
git remote add upstream git@github.com:eclipse-tractusx/managed-identity-wallet.git
```

Whenever you want to start working, pull all changes from your remotes:

```shell
git fetch --all
```

Then rebase your develop branch:

```shell
git checkout develop
git rebase upstream/develop
```

At this point your branches are synced and you can create a new branch:

```shell
git checkout -b feature/add-some-feature
```

### For Eclipse Committers and Maintainers

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

## Eclipse Development Process

This Eclipse Foundation open project is governed by the Eclipse Foundation
Development Process and operates under the terms of the Eclipse IP Policy.

* https://eclipse.org/projects/dev_process
* https://www.eclipse.org/org/documents/Eclipse_IP_Policy.pdf

## Eclipse Contributor Agreement

In order to be able to contribute to Eclipse Foundation projects you must
electronically sign the Eclipse Contributor Agreement (ECA).

* http://www.eclipse.org/legal/ECA.php

The ECA provides the Eclipse Foundation with a permanent record that you agree
that each of your contributions will comply with the commitments documented in
the Developer Certificate of Origin (DCO). Having an ECA on file associated with
the email address matching the "Author" field of your contribution's Git commits
fulfills the DCO's requirement that you sign-off on your contributions.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

## Contact

Contact the project developers via the project's "dev" list.

* https://accounts.eclipse.org/mailing-list/tractusx-dev
