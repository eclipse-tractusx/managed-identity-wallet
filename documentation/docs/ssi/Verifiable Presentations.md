---
title: Verifiable Presentations
sidebar_position: 3
tags: [ ssi, presentations, verifiable presentations, verifiable credentials, issuer, holder, verifier ]
---

# Verifiable Presentations

SSI Verifiable Presentations are a pivotal aspect of Self-Sovereign Identity (SSI), offering a dynamic way for
individuals to share and prove their identity attributes. Built on the principles of decentralized identity, these
presentations allow users to selectively disclose verifiable credentials, securely attesting to their identity without
revealing unnecessary details.

<details>
    <summary>Example</summary>
    <pre>
    \{
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://www.w3.org/2018/credentials/examples/v1"
        ],
        "type": "VerifiablePresentation",
        "verifiableCredential": [
            \{
                "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://www.w3.org/2018/credentials/examples/v1"
                ],
                "id": "http://example.edu/credentials/1872",
                "type": [
                    "VerifiableCredential",
                    "AlumniCredential"
                ],
                "issuer": "https://example.edu/issuers/565049",
                "issuanceDate": "2010-01-01T19:23:24Z",
                "credentialSubject": \{
                    "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                    "alumniOf": \{
                        "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                        "name": [
                            \{
                                "value": "Example University",
                                "lang": "en"
                            },
                            \{
                                "value": "Exemple d'Université",
                                "lang": "fr"
                            }
                        ]
                    }
                },
                "proof": \{
                    "type": "RsaSignature2018",
                    "created": "2017-06-18T21:19:10Z",
                    "proofPurpose": "assertionMethod",
                    "verificationMethod": "https://example.edu/issuers/565049#key-1",
                    "jws": "..."
                }
            }
        ],
        "proof": \{
            "type": "RsaSignature2018",
            "created": "2018-09-14T21:19:10Z",
            "proofPurpose": "authentication",
            "verificationMethod": "did:example:ebfeb1f712ebc6f1c276e12ec21#keys-1",
            "challenge": "1f44d55f-f161-4938-a659-f8026467f126",
            "domain": "4jt78h47fh47",
            "jws": "..."
        }
    }
    </pre>
</details>
