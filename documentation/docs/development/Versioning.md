---
title: Versioning
sidebar_position: 6
tags: [ ]
---

# Versioning

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

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet
