---
title: Branching
sidebar_position: 3
tags: [ ]
---

# Branching

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

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet
