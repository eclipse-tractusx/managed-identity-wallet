/*
 * *******************************************************************************
 *  Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets.service.revocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.CredentialStatus;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.RevocationPurpose;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialVerificationRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.StatusListRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.RevocationException;
import org.eclipse.tractusx.managedidentitywallets.revocation.RevocationClient;
import org.eclipse.tractusx.managedidentitywallets.service.CommonService;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatusList2021Entry;
import org.eclipse.tractusx.ssi.lib.serialization.SerializeUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * The `RevocationService` class is a Java service that handles the revocation of credentials and
 * the creation of status lists.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RevocationService {

    private final RevocationClient revocationClient;

    private final CommonService commonService;

    private final ObjectMapper objectMapper;


    @SneakyThrows
    public void revokeCredential(CredentialVerificationRequest verificationRequest, String callerBpn, String token) {
        VerifiableCredential verifiableCredential;
        if (verificationRequest.containsKey(StringPool.VC_JWT_KEY)) {
            SignedJWT signedJWT = SignedJWT.parse((String) verificationRequest.get(StringPool.VC_JWT_KEY));
            Map<String, Object> claims = objectMapper.readValue(signedJWT.getPayload().toBytes(), Map.class);
            String vcClaim = objectMapper.writeValueAsString(claims.get("vc"));
            Map<String, Object> map = SerializeUtil.fromJson(vcClaim);
            verifiableCredential = new VerifiableCredential(map);
        } else {
            verifiableCredential = new VerifiableCredential(verificationRequest);
        }
        //check if credential status is not null
        Validate.isTrue(verifiableCredential.getVerifiableCredentialStatus().isEmpty()).launch(new BadDataException("Provided credential is not revocable"));

        //Fetch issuer waller
        Wallet issuerWallet = commonService.getWalletByIdentifier(verifiableCredential.getIssuer().toString());

        //check caller must be issuer of VC
        Validate.isFalse(issuerWallet.getBpn().equals(callerBpn)).launch(new ForbiddenException("Invalid credential access"));

        //check if VC is already Revoked
        CredentialStatus credentialStatus = checkRevocation(verifiableCredential, token);

        if (credentialStatus.getName().equals(CredentialStatus.REVOKED.getName())) {
            throw new RevocationException(HttpStatus.CONFLICT.value(), "Credential is already revoked", Map.of("message", "Credential is already revoked"));
        }
        revocationClient.revokeCredential(verifiableCredential.getVerifiableCredentialStatus(), token);
        log.info("Credential with id {} is revoked by caller bpn {}", verifiableCredential.getId(), callerBpn);
    }

    /**
     * Gets status list entry.
     *
     * @param issuerId the issuer id
     * @param token    the token
     * @return the status list entry
     */
    public VerifiableCredentialStatusList2021Entry getStatusListEntry(@NotNull String issuerId, String token) {
        StatusListRequest statusListRequest = StatusListRequest.builder()
                .issuerId(issuerId)
                .purpose(RevocationPurpose.REVOCATION.name().toLowerCase())
                .build();

        return new VerifiableCredentialStatusList2021Entry(revocationClient.getStatusListEntry(statusListRequest, token));
    }

    /**
     * Check revocation credential status.
     *
     * @param verifiableCredential the verifiable credential
     * @param token                the token
     * @return the credential status
     */
    public CredentialStatus checkRevocation(@NotNull VerifiableCredential verifiableCredential, String token) {
        Map<String, String> response = revocationClient.verifyCredentialStatus(verifiableCredential.getVerifiableCredentialStatus(), token);
        log.debug("Revocation status for VC id->{}  -> {}", verifiableCredential.getId(), response.get("status"));
        return CredentialStatus.valueOf(response.get("status").toUpperCase());
    }
}
