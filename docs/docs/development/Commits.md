---
title: Commits
sidebar_position: 4
tags: [ ]
---


# Commits

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

# NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2021,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/managed-identity-wallet
