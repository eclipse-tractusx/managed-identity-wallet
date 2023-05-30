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

import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.junit.jupiter.api.Assertions;

public class TestUtils {

    public static Wallet createWallet(String bpn, String did, WalletRepository walletRepository) {
        String didDocument = """
                {
                  "id": "did:web:localhost:bpn123124",
                  "verificationMethod": [
                    {
                      "publicKeyMultibase": "z9mo3TUPvEntiBQtHYVXXy5DfxLGgaHa84ZT6Er2qWs4y",
                      "controller": "did:web:localhost%3Abpn123124",
                      "id": "did:web:localhost%3Abpn123124#key-1",
                      "type": "Ed25519VerificationKey2020"
                    }
                  ],
                  "@context": "https://www.w3.org/ns/did/v1"
                }
                """;

        Wallet wallet = Wallet.builder()
                .bpn(bpn)
                .did(did)
                .didDocument(DidDocument.fromJson(didDocument))
                .algorithm("ED25519")
                .name(bpn)
                .build();
        return walletRepository.save(wallet);
    }

    public static void checkVC(VerifiableCredential verifiableCredential, MIWSettings miwSettings) {
        //text context URL
        Assertions.assertEquals(verifiableCredential.getContext().size(), miwSettings.vcContexts().size());
        for (String link : verifiableCredential.getContext()) {
            Assertions.assertTrue(miwSettings.vcContexts().contains(link));
        }

        //check expiry date
        Assertions.assertEquals(verifiableCredential.getExpirationDate().compareTo(miwSettings.vcExpiryDate().toInstant()), 0);
    }
}
