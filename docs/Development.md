# Development Process

## Summary

[TBD]

## Branching

The **Managed Identity Wallets** project adheres to
the [Gitflow Workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).

Gitflow is a branching model and workflow for managing version control in software development projects using Git. It
provides a structured approach to organizing branches, releases, and collaboration among team members.

The Gitflow workflow consists of two main branches: "master" and "develop." The "master" branch represents the stable
and production-ready state of the project, while the "develop" branch serves as the main integration branch for ongoing
development.

In addition to these two main branches, Gitflow introduces several supporting branches. Feature branches are created off
the "develop" branch and used for implementing new features or changes. Once a feature is complete, it is merged back
into the "develop" branch. Release branches are created from the "develop" branch to prepare for a new release. Bug
fixes and hotfixes are typically made in separate branches derived from the "master" branch and merged back into both "
master" and "develop" branches.

The Gitflow model promotes a structured and controlled release process. When a stable and tested state is reached in
the "develop" branch, a release branch is created. This branch allows for final testing, bug fixes, and the preparation
of release-related documentation. Once the release is ready, it is merged into both the "master" and "develop" branches,
with the "master" branch receiving a version tag.

## Commits

The **Managed Identity Wallets** project adheres to
the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

Conventional commits are a standardized way of formatting commit messages in software development projects. The
Conventional Commits specification provides guidelines for writing commit messages that are human-readable, informative,
and can be easily parsed by automated tools.

The format of a conventional commit message typically consists of a type, an optional scope, and a subject. The type
indicates the purpose or nature of the commit, such as "feat" for a new feature, "fix" for a bug fix, "docs" for
documentation changes, and so on. The scope is optional and represents the module or component of the project being
modified. The subject is a brief and descriptive summary of the changes made in the commit.

The conventional commit structure is as follows:
> `<type>([optional scope]): <description>`
>
> `[optional body]`
>
> `[optional footer(s)]`

Commonly used types include:

- `feat`
- `fix`
- `ci`
- `chore`
- `docs`
- `refactor`
- `test`

For BREAKING CHANGES use the following _footer_:

- `BREAKING CHANGE: <description>`

---

Example of a commit that introduces breaking changes. To draw additional attention to the breaking changes, the commit
scope is prefixed with an exclamation mark:
> chore(ci)!: drop support for Java 11
>
> BREAKING CHANGE: Java 11 features not available in the new version.

_Please note_: Putting a `!` next to the scope, without the breaking change footer, will not trigger a major release!

---

## Versioning

The **Managed Identity Wallets** project adheres to [semantic versioning](https://semver.org/).

Semantic versioning is a versioning scheme commonly used in software development to convey information about changes and
compatibility between different versions of a software package. It consists of three numbers separated by periods,
following the format MAJOR.MINOR.PATCH.

The MAJOR version indicates significant changes that could potentially break backward compatibility. This means that
when the MAJOR version is incremented, it implies that there are incompatible changes, and developers need to make
updates to their code to ensure compatibility.

The MINOR version represents added functionality or features in a backwards-compatible manner. It indicates that the
software has been enhanced with new features, but existing functionality remains intact, allowing developers to update
their code without any major modifications.

The PATCH version signifies backward-compatible bug fixes or small updates, such as addressing security vulnerabilities
or resolving minor issues. It indicates that changes have been made to improve the software's stability or security
without introducing new features or breaking existing functionality.

By adhering to semantic versioning, developers can communicate the nature of changes in their software releases
effectively. This scheme helps users and developers understand the impact of an update on compatibility and
functionality, making it easier to manage dependencies and ensure smooth integration within software ecosystems.

This project uses the [Semantic Release GitHub Action](https://semantic-release.gitbook.io/semantic-release/) to
automate the release process. This action analyzes commit messages to determine the type of changes and automatically
sets the version number accordingly. It also generates a changelog based on commit messages and publishes the release to
a repository.

These are some commits with their corresponding semantic release types:

| Commit Message                                                                                     | Release Type |
|:---------------------------------------------------------------------------------------------------|:-------------|
| fix(typo): correct minor typos in code                                                             | Patch        |
| feat: add new feature                                                                              | Minor        |
| feat: add new feature that breaks backward compatibility<br/><br/>BREAKING CHANGE: \<description\> | Major        |

# Helm

## Unit Test

This repository uses [Helm Unit Test](https://github.com/helm-unittest/helm-unittest) to test the Helm charts.

### Installation

```bash
$ helm plugin install https://github.com/helm-unittest/helm-unittest.git
```

### Run Tests

```bash
$ helm unittest <chart-name>
```


## Documentation

For helm chart documentation we use
the [Helm-Docs by Norwoodj](https://github.com/norwoodj/helm-docs).

### Installation

Homebrew
```bash
brew install norwoodj/tap/helm-docs
```

Scoop
```bash
scoop install helm-docs
```

### Generate Documentation

```
helm-docs
# OR
helm-docs --dry-run # prints generated documentation to stdout rather than modifying READMEs
```

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet
