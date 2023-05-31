/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

package org.eclipse.tractusx.managedidentitywallets.utils;

import org.eclipse.tractusx.managedidentitywallets.constant.ApplicationConstant;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.ssi.lib.model.Ed25519Signature2020;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Common utils.
 */
public class CommonUtils {

    private CommonUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Gets identifier type.
     *
     * @param identifier the identifier
     * @return the identifier type
     */
    public static String getIdentifierType(String identifier) {
        if (identifier.startsWith("did:web")) {
            return ApplicationConstant.DID;
        } else {
            return ApplicationConstant.BPN;
        }
    }


    /**
     * Gets credential.
     *
     * @param subject         the subject
     * @param type            the type
     * @param issuerDid       the issuer did
     * @param privateKeyBytes the private key bytes
     * @param holderDid       the holder did
     * @return the credential
     */
    public static Credential getCredential(Map<String, Object> subject, String type, String issuerDid, byte[] privateKeyBytes, String holderDid, List<String> contexts, Date expiryDate) {
        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(subject);

        // VC Type
        List<String> verifiableCredentialType = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, type);

        // Create VC
        VerifiableCredential verifiableCredential = createVerifiableCredential(issuerDid, verifiableCredentialType, verifiableCredentialSubject, privateKeyBytes, contexts, expiryDate);

        // Create Credential
        return Credential.builder()
                .holderDid(holderDid)
                .issuerDid(issuerDid)
                .type(type)
                .data(verifiableCredential)
                .build();
    }


    private static VerifiableCredential createVerifiableCredential(String issuerDid, List<String> verifiableCredentialType, VerifiableCredentialSubject verifiableCredentialSubject, byte[] privateKey, List<String> contexts, Date expiryDate) {
        //VC Builder
        VerifiableCredentialBuilder builder =
                new VerifiableCredentialBuilder()
                        .context(contexts)
                        .id(URI.create(UUID.randomUUID().toString()))
                        .type(verifiableCredentialType)
                        .issuer(URI.create(issuerDid))
                        .expirationDate(expiryDate.toInstant())
                        .issuanceDate(Instant.now())
                        .credentialSubject(verifiableCredentialSubject);


        //Ed25519 Proof Builder
        LinkedDataProofGenerator generator = LinkedDataProofGenerator.create();
        Ed25519Signature2020 proof = generator.createEd25519Signature2020(builder.build(), URI.create(issuerDid), privateKey);

        //Adding Proof to VC
        builder.proof(proof);

        //Create Credential
        return builder.build();
    }
}
