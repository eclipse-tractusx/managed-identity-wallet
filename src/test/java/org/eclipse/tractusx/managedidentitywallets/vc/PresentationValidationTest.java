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

package org.eclipse.tractusx.managedidentitywallets.vc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.dto.IssueMembershipCredentialRequest;
import org.eclipse.tractusx.managedidentitywallets.service.IssuersCredentialService;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.eclipse.tractusx.managedidentitywallets.utils.AuthenticationUtils;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationType;
import org.eclipse.tractusx.ssi.lib.serialization.SerializeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = {TestContextInitializer.class})
class PresentationValidationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Autowired
    private WalletService walletService;
    @Autowired
    private IssuersCredentialService issuersCredentialService;
    @Autowired
    private PresentationService presentationService;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MIWSettings miwSettings;

    private final String bpnTenant_1 = TestUtils.getRandomBpmNumber();
    private final String bpnTenant_2 = TestUtils.getRandomBpmNumber();
    private String bpnOperator;
    private Did tenant_1;
    private Did tenant_2;
    private VerifiableCredential membershipCredential_1;
    private VerifiableCredential membershipCredential_2;

    @BeforeEach
    public void setup() {
        bpnOperator = miwSettings.authorityWalletBpn();

        CreateWalletRequest createWalletRequest = new CreateWalletRequest();
        createWalletRequest.setBusinessPartnerNumber(bpnTenant_1);
        createWalletRequest.setCompanyName("My Test Tenant Wallet");
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpnTenant_1;
        createWalletRequest.setDidUrl(defaultLocation);
        Wallet tenantWallet = walletService.createWallet(createWalletRequest, bpnOperator);
        tenant_1 = DidParser.parse(tenantWallet.getDid());

        CreateWalletRequest createWalletRequest2 = new CreateWalletRequest();
        createWalletRequest2.setBusinessPartnerNumber(bpnTenant_2);
        createWalletRequest2.setCompanyName("My Test Tenant Wallet");
        String defaultLocation2 = miwSettings.host() + COLON_SEPARATOR + bpnTenant_2;
        createWalletRequest2.setDidUrl(defaultLocation2);
        Wallet tenantWallet2 = walletService.createWallet(createWalletRequest2, bpnOperator);
        tenant_2 = DidParser.parse(tenantWallet2.getDid());

        IssueMembershipCredentialRequest issueMembershipCredentialRequest = new IssueMembershipCredentialRequest();
        issueMembershipCredentialRequest.setBpn(bpnTenant_1);
        membershipCredential_1 = issuersCredentialService.issueMembershipCredential(issueMembershipCredentialRequest, bpnOperator);

        IssueMembershipCredentialRequest issueMembershipCredentialRequest2 = new IssueMembershipCredentialRequest();
        issueMembershipCredentialRequest2.setBpn(bpnTenant_2);
        membershipCredential_2 = issuersCredentialService.issueMembershipCredential(issueMembershipCredentialRequest2, bpnOperator);
    }

    @Test
    void testSuccessfulValidation() {
        Map<String, Object> presentation = createPresentationJwt(membershipCredential_1, tenant_1);
        VerifiablePresentationValidationResponse response = validateJwtOfCredential(presentation);
        Assertions.assertTrue(response.valid);
    }

    @Test
    @SneakyThrows
    public void testSuccessfulValidationForMultipleVC() {
        Map<String, Object> creationResponse = createPresentationJwt(List.of(membershipCredential_1, membershipCredential_2), tenant_1);
        // get the payload of the json web token
        String encodedJwtPayload = ((String) creationResponse.get("vp")).split("\\.")[1];
        Map<String, Object> decodedJwtPayload = OBJECT_MAPPER.readValue(Base64.getUrlDecoder().decode(encodedJwtPayload), Map.class);
        VerifiablePresentation presentation = new VerifiablePresentation((Map) decodedJwtPayload.get("vp"));
        VerifiablePresentationValidationResponse response = validateJwtOfCredential(creationResponse);

        Assertions.assertTrue(response.valid);

        Assertions.assertEquals(2, presentation.getVerifiableCredentials().size());
    }

    @Test
    public void testValidationFailureOfCredentialWitInvalidExpirationDate() {
        // test is related to this old issue where the signature check still succeeded
        // https://github.com/eclipse-tractusx/SSI-agent-lib/issues/4
        VerifiableCredential copyCredential = new VerifiableCredential(membershipCredential_1);
        // e.g. an attacker tries to extend the validity of a verifiable credential
        copyCredential.put(VerifiableCredential.EXPIRATION_DATE, "2500-09-30T22:00:00Z");
        Map<String, Object> presentation = createPresentationJwt(copyCredential, tenant_1);
        VerifiablePresentationValidationResponse response = validateJwtOfCredential(presentation);
        Assertions.assertFalse(response.valid);
    }


    @Test
    public void testValidationFailureOfCredentialWitInvalidExpirationDateInSecondCredential() {
        // test is related to this old issue where the signature check still succeeded
        // https://github.com/eclipse-tractusx/SSI-agent-lib/issues/4
        VerifiableCredential copyCredential = new VerifiableCredential(membershipCredential_1);
        // e.g. an attacker tries to extend the validity of a verifiable credential
        copyCredential.put(VerifiableCredential.EXPIRATION_DATE, "2500-09-30T22:00:00Z");
        Map<String, Object> presentation = createPresentationJwt(List.of(membershipCredential_1, copyCredential), tenant_1);
        VerifiablePresentationValidationResponse response = validateJwtOfCredential(presentation);
        Assertions.assertFalse(response.valid);
    }

    @Test
    @SneakyThrows
    void testValidationFailureOfPresentationPayloadManipulation() {
        Map<String, Object> presentation = createPresentationJwt(membershipCredential_1, tenant_1);

        String jwt = (String) presentation.get(StringPool.VP);
        String payload = jwt.split("\\.")[1];
        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder();

        byte[] payloadDecoded = decoder.decode(payload);
        Map<String, Object> payloadMap = OBJECT_MAPPER.readValue(payloadDecoded, Map.class);

        // replace with credential of another tenant
        VerifiablePresentation newPresentation = new VerifiablePresentationBuilder()
                .context(List.of(VerifiablePresentation.DEFAULT_CONTEXT))
                .id(URI.create("did:test:" + UUID.randomUUID()))
                .type(List.of(VerifiablePresentationType.VERIFIABLE_PRESENTATION))
                .verifiableCredentials(List.of(membershipCredential_2))
                .build();
        payloadMap.put("vp", newPresentation);
        String newPayloadJson = OBJECT_MAPPER.writeValueAsString(payloadMap);
        String newPayloadEncoded = encoder.encodeToString(newPayloadJson.getBytes());

        String newJwt = jwt.split("\\.")[0] + "." + newPayloadEncoded + "." + jwt.split("\\.")[2];

        VerifiablePresentationValidationResponse response = validateJwtOfCredential(Map.of(
                StringPool.VP, newJwt
        ));
        Assertions.assertNotEquals(jwt, newJwt);
        Assertions.assertFalse(response.valid, String.format("The validation should fail because the vp is manipulated.\nOriginal JWT: %s\nNew JWT: %s", jwt, newJwt));
    }

    @SneakyThrows
    private VerifiablePresentationValidationResponse validateJwtOfCredential(Map<String, Object> presentationJwt) {
        HttpHeaders headers = AuthenticationUtils.getValidUserHttpHeaders(miwSettings.authorityWalletBpn());
        headers.set("Content-Type", "application/json");
        HttpEntity<Map> entity = new HttpEntity<>(presentationJwt, headers);

        ResponseEntity<String> response = restTemplate.exchange(RestURI.API_PRESENTATIONS_VALIDATION + "?asJwt=true", HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return OBJECT_MAPPER.readValue(response.getBody(), VerifiablePresentationValidationResponse.class);
        }

        throw new RuntimeException(String.format("JWT:\n%s\nResponse: %s",
                SerializeUtil.toPrettyJson(presentationJwt),
                OBJECT_MAPPER.writeValueAsString(response)));
    }

    private Map<String, Object> createPresentationJwt(List<VerifiableCredential> verifiableCredential, Did issuer) {
        return presentationService.createPresentation(Map.of(StringPool.VERIFIABLE_CREDENTIALS, verifiableCredential),
                true, issuer.toString(), issuer.toString());
    }

    private Map<String, Object> createPresentationJwt(VerifiableCredential verifiableCredential, Did issuer) {
        return presentationService.createPresentation(Map.of(StringPool.VERIFIABLE_CREDENTIALS, List.of(verifiableCredential)),
                true, issuer.toString(), issuer.toString());
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class VerifiablePresentationValidationResponse {
        boolean valid;
        String vp;
    }
}
