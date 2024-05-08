---
title: Decentralized Identifiers
sidebar_position: 3
tags: [ ssi, identifiers, did, verifier ]
---

# Decentralized Identifiers (DID)

In the realm of Self-Sovereign Identity (SSI), Decentralized Identifiers (DIDs) play a pivotal role in reshaping digital
identity management. DIDs are unique, persistent identifiers created on decentralized networks, providing a secure
foundation for user-controlled identity interactions. Complementing DIDs are DID Documents, which contain essential
information such as public keys, authentication methods, and service endpoints associated with the DID. Importantly,
DIDs can be resolved to their corresponding DID Documents, allowing for dynamic retrieval of key identity information.
This dynamic duo, grounded in decentralization and cryptographic security, empowers individuals to independently own,
control, and selectively share their identities across diverse platforms.

<details>
    <summary>Example</summary>
    <table>
        <tr>
            <td>Decentralized Identifier (DID)</td>
            <td><strong>did:example:123456789abcdefghi</strong></td>
        </tr>
        <tr>
            <td>DID document</td>
            <td>
                <pre>
                \{
                    "@context": [
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/ed25519-2020/v1"
                    ],
                    "id": "did:example:123456789abcdefghi",
                    "verificationMethod": [
                        \{
                            "id": "did:example:123456789abcdefghi#key-1",
                            "type": "Ed25519VerificationKey2020",
                            "controller": "did:example:123456789abcdefghi",
                            "publicKeyMultibase": "zH3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV"
                        }
                    ],
                    "authentication": [
                        "#key-1"
                    ]
                }
                </pre>
            </td>
        </tr>
    </table>
</details>
