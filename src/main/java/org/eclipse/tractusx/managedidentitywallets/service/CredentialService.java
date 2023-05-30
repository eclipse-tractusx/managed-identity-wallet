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
import org.eclipse.tractusx.managedidentitywallets.constant.MIWVerifiableCredentialType;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueDismantlerCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueFrameworkCredentialRequest;
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
     * Issue framework credential verifiable credential.
     *
     * @param request the request
     * @return the verifiable credential
     */
    public VerifiableCredential issueFrameworkCredential(IssueFrameworkCredentialRequest request) {
        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(request.getBpn());
        
        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = getPrivateKeyById(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", request.getType(),
                "id", holderWallet.getDid(),
                "value", request.getValue(),
                "contract-template", request.getContractTemplate(),
                "contract-version", request.getContractVersion());
        Credential credential = getCredential(subject, MIWVerifiableCredentialType.USE_CASE_FRAMEWORK_CONDITION_CX, baseWallet, privateKeyBytes, holderWallet);

        //Store Credential
        credentialRepository.save(credential);

        // Return VC
        return credential.getData();
    }

    /**
     * Issue dismantler credential verifiable credential.
     *
     * @param request the request
     * @return the verifiable credential
     */
    public VerifiableCredential issueDismantlerCredential(IssueDismantlerCredentialRequest request) {

        //Fetch Holder Wallet
        Wallet holderWallet = walletService.getWalletByIdentifier(request.getBpn());

        //check duplicate
        isCredentialExit(holderWallet.getId(), MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX);

        // Fetch Issuer Wallet
        Wallet baseWallet = walletService.getWalletByIdentifier(miwSettings.authorityWalletBpn());
        byte[] privateKeyBytes = getPrivateKeyById(baseWallet.getId());

        Map<String, Object> subject = Map.of("type", MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "activityType", request.getActivityType(),
                "allowedVehicleBrands", request.getAllowedVehicleBrands());
        Credential credential = getCredential(subject, MIWVerifiableCredentialType.DISMANTLER_CREDENTIAL_CX, baseWallet, privateKeyBytes, holderWallet);

        //Store Credential
        credentialRepository.save(credential);

        // Return VC
        return credential.getData();
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
        Credential credential = getCredential(Map.of("type", VerifiableCredentialType.MEMBERSHIP_CREDENTIAL,
                "id", holderWallet.getDid(),
                "holderIdentifier", holderWallet.getBpn(),
                "memberOf", baseWallet.getName(),
                "status", "Active",
                "startTime", Instant.now().toString()), VerifiableCredentialType.MEMBERSHIP_CREDENTIAL, baseWallet, privateKeyBytes, holderWallet);

        //Store Credential
        credentialRepository.save(credential);

        // Return VC
        return credential.getData();
    }

    private VerifiableCredential createVerifiableCredential(String issuerDid, List<String> verifiableCredentialType, VerifiableCredentialSubject verifiableCredentialSubject, byte[] privateKey) {
        //VC Builder
        VerifiableCredentialBuilder builder =
                new VerifiableCredentialBuilder()
                        .context(miwSettings.vcContexts())
                        .id(URI.create(UUID.randomUUID().toString()))
                        .type(verifiableCredentialType)
                        .issuer(URI.create(issuerDid))
                        .expirationDate(miwSettings.vcExpiryDate().toInstant())
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


    private Credential getCredential(Map<String, Object> subject, String type, Wallet baseWallet, byte[] privateKeyBytes, Wallet holderWallet) {
        //VC Subject
        VerifiableCredentialSubject verifiableCredentialSubject =
                new VerifiableCredentialSubject(subject);

        // VC Type
        List<String> verifiableCredentialType = List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL, type);

        // Create VC
        VerifiableCredential verifiableCredential = createVerifiableCredential(baseWallet.getDid(), verifiableCredentialType, verifiableCredentialSubject, privateKeyBytes);

        // Create Credential
        return Credential.builder()
                .holder(holderWallet.getId())
                .issuer(baseWallet.getId())
                .type(type)
                .data(verifiableCredential)
                .build();
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
