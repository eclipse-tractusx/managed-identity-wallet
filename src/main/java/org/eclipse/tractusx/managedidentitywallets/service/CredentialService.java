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

package org.eclipse.tractusx.managedidentitywallets.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateCredentialProblem;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.model.Ed25519Signature2020;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The type Credential service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final MIWSettings miwSettings;
    private final WalletKeyRepository walletKeyRepository;
    private final EncryptionUtils encryptionUtils;

    private final WalletService walletService;

    /**
     * Gets credentials.
     *
     * @param holderIdentifier the holder identifier
     * @param id               the id
     * @param issuerIdentifier the issuer identifier
     * @param type             the type
     * @return the credentials
     */
    public List<Credential> getCredentials(String holderIdentifier, String id, String issuerIdentifier, List<String> type) {
        return credentialRepository.findAll();//TODO with params
    }

    /**
     * Issue membership credential verifiable credential.
     *
     * @param issueMembershipCredentialRequest the issue membership credential request
     * @return the verifiable credential
     */
    @SneakyThrows
    public VerifiableCredential issueMembershipCredential(IssueMembershipCredentialRequest issueMembershipCredentialRequest) {

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(issueMembershipCredentialRequest.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getId(), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = getPrivateKeyById(baseWallet.getId());

        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(Map.of("type", VerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                        "holderIdentifier", holderWallet.getBpn(),
                        "memberOf", baseWallet.getName(),
                        "status", "Active",
                        "startTime", Instant.now().toString()));

        // VC Type
        List<String> verifiableCredentialType = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, VerifiableCredentialType.MEMBERSHIP_CREDENTIAL);

        // Create VC
        VerifiableCredential verifiableCredential = createVerifiableCredential(baseWallet.getDid(), verifiableCredentialType, verifiableCredentialSubject, privateKeyBytes);

        // Create Credential
        Credential credential = Credential.builder()
                .holder(holderWallet.getId())
                .issuer(baseWallet.getId())
                .type(VerifiableCredentialType.MEMBERSHIP_CREDENTIAL)
                .data(verifiableCredential)
                .build();

        //Store Credential
        credentialRepository.save(credential);

        // Return VC
        return credential.getData();
    }

    private VerifiableCredential createVerifiableCredential(String issuerDid, List<String> verifiableCredentialType, VerifiableCredentialSubject verifiableCredentialSubject, byte[] privateKey) {
        List<String> context = List.of("https://www.w3.org/2018/credentials/v1", "https://raw.githubusercontent.com/catenax-ng/product-core-schemas/main/businessPartnerData");
        //VC Builder
        VerifiableCredentialBuilder builder =
                new VerifiableCredentialBuilder()
                        .context(context)
                        .id(URI.create(UUID.randomUUID().toString()))
                        .type(verifiableCredentialType)
                        .issuer(URI.create(issuerDid))
                        .expirationDate(Instant.now().plusSeconds(365 * 86400)) //TODO need to verify expiry time
                        .issuanceDate(Instant.now())
                        .credentialSubject(verifiableCredentialSubject);

        //Ed25519 Proof Builder
        LinkedDataProofGenerator generator = LinkedDataProofGenerator.create();
        Ed25519Signature2020 proof = generator.createEd25519Signature2020(builder.build(), URI.create(issuerDid + "#key-1"), privateKey);

        //Adding Proof to VC
        builder.proof(proof);

        //Create Credential
        return builder.build();
    }

    @SneakyThrows
    private byte[] getPrivateKeyById(Long id) {
        WalletKey baseWalletKey = walletKeyRepository.getByWalletId(id);
        String privateKey = encryptionUtils.decrypt(baseWalletKey.getPrivateKey());
        return new PemReader(new StringReader(privateKey)).readPemObject().getContent();
    }

    private void isCredentialExit(Long holderId, String credentialType) {
        Validate.isTrue(credentialRepository.existsByHolderAndType(holderId, credentialType)).launch(new DuplicateCredentialProblem("Credential of type " + credentialType + " is already exists "));
    }
}
