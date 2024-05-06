---
title: Verifiable Credentials
sidebar_position: 3
tags: [ ssi, credentials, presentations, verifiable presentations, verifiable credentials, issuer, holder, verifier ]
---

# Verifiable Credentials

SSI Verifiable Credentials are a cornerstone of Self-Sovereign Identity (SSI), offering a transformative solution to
traditional identity verification. These credentials, often stored on decentralized ledgers like blockchain, enable
individuals to own and control their digital identity attributes. By leveraging cryptographic proofs, verifiable
credentials allow for secure and tamper-proof verification without the need for centralized authorities. This
breakthrough in identity management fosters privacy, interoperability, and user autonomy, revolutionizing how
individuals share and authenticate their personal information in the digital realm.

<details>
    <summary>Example</summary>
    <pre>
    \{
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://www.w3.org/2018/credentials/examples/v1"
        ],
        "id": "http://example.edu/credentials/58473",
        "type": ["VerifiableCredential", "AlumniCredential"],
        "credentialSubject": \{
            "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
            "image": "https://example.edu/images/58473",
            "alumniOf": \{
                "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                "name": [\{
                    "value": "Example University",
                    "lang": "en"
                    }, \{
                    "value": "Exemple d'Université",
                    "lang": "fr"
                }]
            }
        },
        "proof": \{
        }
    }
    </pre>
</details>

## Verifiable Credentials for Data Spaces

The MIW is not only about managing self sovereign identities, it is also about data spaces. A data space typically
refers to a virtual or conceptual environment where data is organized, stored, and managed. It is a framework that
allows for the structured representation, storage, and retrieval of information. The concept of a data space is often
associated with the idea of creating a unified, accessible, and coherent space for handling diverse types of data.

Tracxtus-X Managed Identity Wallets (MIW) are designed to support the use of Verifiable Credentials (VC) in the context
of data spaces. So this repository introduces a set of Verifiable Credentials that may be used to enforce access control
within a data space.

Access control through Verifiable Credentials could be implemented as follows:

- All members within a data space place trust in one or more Verifiable Credential Issuers. This trust relationship can
  vary, accommodating scenarios where a single issuer is responsible for all Verifiable Credentials or where different
  issuers handle specific types of Verifiable Credentials, depending on the use case.
- The Issuers verteilen distribute these Verifiable Credentials to the participants (Holders) within the data space as
  required.
- A participant in the data space securely stores these Verifiable Credentials in their digital wallet.
- When two participants within the data space intend to share data, they initiate the process by exchanging Verifiable
  Credentials. This exchange serves the purpose of verifying whether both participants belong to the same data space and
  possess the necessary access rights before proceeding with any data sharing activities.

### Membership Verifiable Credential

A Membership Verifiable Credential in the context of data spaces refers to a type of verifiable credential that attests
to an individual or entity's membership status within a specific data space or community. This credential provides
cryptographic proof of the entity's association with the data space.

<details>
    <summary>Example</summary>
    <pre>
    \{
        "issuanceDate": "2024-01-19T08:00:17Z",
        "credentialSubject": [
            \{
                "holderIdentifier": "BPN12345",
                "startTime": "2024-01-19T08:00:17.748160281Z",
                "memberOf": "Tractus-X",
                "id": "did:web:managed-identity-wallets.foo:BPN12345",
                "type": "MembershipCredential",
                "status": "Active"
            }
        ],
        "id": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER#1b6813e3-14f3-462c-afce-9a5c3d75e83f",
        "proof": \{
            "proofPurpose": "assertionMethod",
            "verificationMethod": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER#049f920c-e702-4e36-9b01-540423788a90",
            "type": "JsonWebSignature2020",
            "created": "2024-01-19T08:00:17Z",
            "jws": "..."
        },
        "type": [
            "VerifiableCredential",
            "MembershipCredential"
        ],
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://localhost/your-context.json",
            "https://w3id.org/security/suites/jws-2020/v1"
        ],
        "issuer": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER",
        "expirationDate": "2024-06-30T00:00:00Z"
    }
    </pre>
</details>

### Business Partner Number Verifiable Credential

A Business Partner Number (BPN) Verifiable Credential serves the purpose of linking a participant to a specific Business
Partner Number within a given data space, forming an integral part of the Verifiable Credential Subject. Each Business
Partner Number is distinctly unique within the confines of the data space.

<details>
    <summary>Example</summary>
    <pre>
    \{
        "credentialSubject": [
            \{
                "contractTemplate": "https://public.catena-x.org/contracts/",
                "holderIdentifier": "BPN12345",
                "id": "did:web:managed-identity-wallets.foo:BPN12345",
                "items": [
                    "BpnCredential"
                ],
                "type": "SummaryCredential"
            }
        ],
        "issuanceDate": "2023-07-18T09:33:11Z",
        "id": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER#340fc333-18b3-436b-abdb-461e8d0d4084",
        "proof": \{
            "created": "2023-07-18T09:33:11Z",
            "jws": "...",
            "proofPurpose": "proofPurpose",
            "type": "JsonWebSignature2020",
            "verificationMethod": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER#"
        },
        "type": [
            "VerifiableCredential",
            "SummaryCredential"
        ],
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://catenax-ng.github.io/product-core-schemas/SummaryVC.json",
            "https://w3id.org/security/suites/jws-2020/v1"
        ],
        "issuer": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER",
        "expirationDate": "2023-10-01T00:00:00Z"
    }
    </pre>
</details>

### Dismantler Verifiable Credential

A Verifiable Credential can extend its utility by associating the participant with a particular role within the data
space. In this instance, the Verifiable Credential Subject provides evidence that the participant holds the role of a
dismantler. Furthermore, the Verifiable Credential Subject elaborates on the specific capabilities and qualifications of
the participant in their capacity as a dismantler.

<details>
    <summary>Example</summary>
    <pre>
    \{
        "credentialSubject": [
            \{
                "bpn": "BPN12345",
                "id": "did:web:managed-identity-wallets.foo:BPN12345",
                "type": "DismantlerCredential",
                "activityType": "vehicleDismantle",
                "allowedVehicleBrands": "Alfa Romeo, Mercedes-Benz"
            }
        ],
        "issuanceDate": "2023-07-13T12:35:00Z",
        "id": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER#845ee4fd-4743-48d4-9b84-c09f29c49b80",
        "proof": \{
            "created": "2023-07-13T12:35:00Z",
            "jws": "...",
            "proofPurpose": "proofPurpose",
            "type": "JsonWebSignature2020",
            "verificationMethod": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER#"
        },
        "type": [
            "VerifiableCredential",
            "DismantlerCredent"proof":ial"
        ],
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://localhost/your-context.json",
            "https://w3id.org/security/suites/jws-2020/v1"
        ],
        "issuer": "did:web:managed-identity-wallets.foo:BPNL0000000ISSUER",
        "expirationDate": "2023-09-30T22:00:00Z"
    }
    </pre>
</details>

> Proposed Verifiable Credentials schemas for data spaces a further discussed in
> the [schemas documentation](../schemas/README.md).
