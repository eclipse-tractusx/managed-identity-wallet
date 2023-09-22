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

package org.eclipse.tractusx.managedidentitywallets.test.cucumber.StepDefinition.credential;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.eclipse.tractusx.managedidentitywallets.test.config.Configuration;
import org.eclipse.tractusx.managedidentitywallets.test.utils.AuthenticationHelper;
import org.eclipse.tractusx.managedidentitywallets.test.utils.CommonUtils;
import org.eclipse.tractusx.managedidentitywallets.test.utils.StringPool;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.testng.Assert;

import java.util.*;

import static org.eclipse.tractusx.managedidentitywallets.test.utils.CommonUtils.getAndVerifyVC;
import static org.eclipse.tractusx.managedidentitywallets.test.utils.CommonUtils.getObjectMapper;

@Slf4j
public class CredentialSteps {


    public static final String BEHAVIOR_TWIN_CREDENTIAL = "BehaviorTwinCredential";
    public static final String PCF_CREDENTIAL = "PcfCredential";
    public static final String SUSTAINABILITY_CREDENTIAL = "SustainabilityCredential";
    public static final String QUALITY_CREDENTIAL = "QualityCredential";
    public static final String TRACEABILITY_CREDENTIAL = "TraceabilityCredential";
    public static final String RESILIENCY_CREDENTIAL = "ResiliencyCredential";
    private String baseWalletAccessToken;

    private Response createWalletResponse;

    private String randomBpn;

    private String baseWalletClientId;

    private String baseWalletClientSecret;

    private String userWalletBpn;

    private String userWalletClientId;

    private String userWalletClientSecret;

    private String userWalletDid;

    private OkHttpClient client;
    private VerifiableCredential summaryCredential;
    private String audience;
    private Boolean asJwt;
    private Boolean withCredentialExpiryDate;

    private Map vpAsJwt;

    private Map membershipVPAsJwt;

    private Map dismantlerVPAsJwt;

    private Map frameworkVPAsJwt;

    private Map vpAsJwtOfMultipleVC;

    private Map vpAsJwtOfCustomVC;

    private String customVCType;

    private Response issueCustomVcResponse;

    private String userAccessToken;

    private Response issueDuplicateVCResponse;

    private VerifiableCredential membershipVC;

    private VerifiableCredential dismantlerVC;

    private VerifiableCredential frameworkVc;

    private VerifiableCredential bpnCredential;

    private VerifiableCredential customTypeVC;


    @Before
    public void SetUp() {
        log.info("Setting up credentialSteps wallet test");
        client = new OkHttpClient();
        baseWalletAccessToken = null;
        createWalletResponse = null;
        randomBpn = null;
        baseWalletClientId = null;
        baseWalletClientSecret = null;
        summaryCredential = null;
        audience = null;
        asJwt = null;
        withCredentialExpiryDate = null;
        userWalletBpn = null;
        userWalletClientId = null;
        userWalletClientSecret = null;
        customVCType = null;
        userWalletDid = null;
        issueCustomVcResponse = null;
        userAccessToken = null;
        issueDuplicateVCResponse = null;
        membershipVC = null;
        membershipVPAsJwt = null;
        dismantlerVPAsJwt = null;
        frameworkVc = null;
        frameworkVPAsJwt = null;
        bpnCredential = null;
        vpAsJwtOfCustomVC = null;
    }

    @Given("keycloak client_id and client_secret of base wallet, MIW host application host")
    public void keycloakClient_idAndClient_secretOfBaseWalletMIWHostApplicationHost() {
        baseWalletClientId = Configuration.getBaseWalletClientId();
        baseWalletClientSecret = Configuration.getBaseWalletClientSecret();
    }

    @Given("Keycloak client_id and client_secret of user wallet, MIW host and users BPN")
    public void keycloakClient_idAndClient_secretOfUserWalletMIWHostAndUsersBPN() {
        userWalletClientId = Configuration.getUserWalletClientId();
        userWalletClientSecret = Configuration.getUserWalletClientSecret();
    }


    @Then("Create access_token using client_id and client_secret")
    public void createAccess_tokenUsingClient_idAndClient_secret() {
        baseWalletAccessToken = AuthenticationHelper.getAccessToken(baseWalletClientId, baseWalletClientSecret);
        Assert.assertTrue(StringUtils.isNoneBlank(baseWalletAccessToken));
    }

    @SneakyThrows
    @Then("Create a wallet using a random BPN")
    public void createAWalletUsingARandomBPN() {
        randomBpn = CommonUtils.getRandomBpmNumber();
        log.info(randomBpn, baseWalletAccessToken, client);
        createWalletResponse = CommonUtils.createWallet(randomBpn, baseWalletAccessToken, client);
        Assert.assertEquals(createWalletResponse.code(), HttpStatus.SC_CREATED);
        Assert.assertNotNull(createWalletResponse.body());
        Map map = getObjectMapper().readValue(createWalletResponse.body().string(), Map.class);
        Assert.assertNotNull(map);
        Assert.assertEquals(map.get(StringPool.BPN).toString(), randomBpn);
    }

    @SneakyThrows
    @Then("Issue a membership verifiable credential\\(VC) to that wallet")
    public void issueAMembershipVerifiableCredentialVCToThatWallet() {
        String url = Configuration.getMIWHost() + StringPool.URI_ISSUE_MEMBERSHIP_VC;
        Map<String, String> body = Map.of(StringPool.BPN, randomBpn);
        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        Response response = CommonUtils.callPostEndpoint(client, url, baseWalletAccessToken, requestBody);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
        membershipVC = new VerifiableCredential(getObjectMapper().readValue(Objects.requireNonNull(response.body()).string(), Map.class));
    }

    @SneakyThrows
    @Then("Try to issue membership VC again")
    public void issueAMembershipVerifiableCredentialAgain() {
        String url = Configuration.getMIWHost() + StringPool.URI_ISSUE_MEMBERSHIP_VC;
        Map<String, String> body = Map.of(StringPool.BPN, randomBpn);
        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        issueDuplicateVCResponse = CommonUtils.callPostEndpoint(client, url, baseWalletAccessToken, requestBody);
    }

    @Then("Validate membership VC")
    public void validateMembershipVC() {
        Assert.assertNotNull(membershipVC);
        CommonUtils.validateVC(client, membershipVC, baseWalletAccessToken, true);
    }

    @Then("It should give duplicate error with status code 409")
    public void itShouldGiveDuplicateErrorWithStatusCode() {
        Assert.assertNotNull(issueDuplicateVCResponse);
        Assert.assertEquals(issueDuplicateVCResponse.code(), HttpStatus.SC_CONFLICT);
    }

    @SneakyThrows
    @Then("Issue dismantler VC to that wallet")
    public void issueDismantlerVCToThatWallet() {
        Response response = CommonUtils.issueDismantleVC(client, randomBpn, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
        dismantlerVC = new VerifiableCredential(getObjectMapper().readValue(Objects.requireNonNull(response.body()).string(), Map.class));
    }


    @SneakyThrows
    @Then("Issue BehaviorTwinCredential VC to that Wallet")
    public void issueBehaviorTwinCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, BEHAVIOR_TWIN_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Issue PcfCredential VC to that Wallet")
    public void issuePcfCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, PCF_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Issue SustainabilityCredential VC to that Wallet")
    public void issueSustainabilityCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, SUSTAINABILITY_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Issue QualityCredential VC to that Wallet")
    public void issueQualityCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, QUALITY_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Issue TraceabilityCredential VC to that Wallet")
    public void issueTraceabilityCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, TRACEABILITY_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Issue ResiliencyCredential VC to that Wallet")
    public void issueResiliencyCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, RESILIENCY_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
    }

    @Then("Verify the Summary VC of that wallet also check the items, it should contain all required values")
    public void verifyTheSummaryVCOfThatWalletAlsoCheckTheItemsItShouldContainAllRequiredValues() {
        summaryCredential = CommonUtils.getAndVerifyVC(client, baseWalletAccessToken, randomBpn, StringPool.SUMMARY_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
        List<String> items = ((ArrayList<String>) summaryCredential.getCredentialSubject().get(0).get(StringPool.ITEMS));
        Assert.assertTrue(items.contains(BEHAVIOR_TWIN_CREDENTIAL));
        Assert.assertTrue(items.contains(PCF_CREDENTIAL));
        Assert.assertTrue(items.contains(SUSTAINABILITY_CREDENTIAL));
        Assert.assertTrue(items.contains(QUALITY_CREDENTIAL));
        Assert.assertTrue(items.contains(TRACEABILITY_CREDENTIAL));
        Assert.assertTrue(items.contains(RESILIENCY_CREDENTIAL));
    }

    @SneakyThrows
    @Then("Validate Summary VC with VC expiry date check")
    public void validateSummaryVCWithVCExpiryDateCheck() {
        withCredentialExpiryDate = true;
        CommonUtils.validateVC(client, summaryCredential, baseWalletAccessToken, true);
    }

    @SneakyThrows
    @Then("Create a Verifiable presentation\\(VP) of summary VC as JWT")
    public void createAVerifiablePresentationVPOfSummaryVCAsJWT() {
        audience = "smart";
        vpAsJwt = CommonUtils.createVP(audience, true, List.of(summaryCredential), client, baseWalletAccessToken);
    }

    @SneakyThrows
    @Then("Validate VP")
    public void validateVP() {
        CommonUtils.validateVP(audience, withCredentialExpiryDate, vpAsJwt, baseWalletAccessToken, client);
    }

    @Given("keycloak client_id and client_secret of base wallet, Keycloak client_id and client_secret of user's wallet  MIW host application host")
    public void keycloakClient_idAndClient_secretOfBaseWalletKeycloakClient_idAndClient_secretOfUserSWalletMIWHostApplicationHost() {
        baseWalletClientId = Configuration.getBaseWalletClientId();
        baseWalletClientSecret = Configuration.getBaseWalletClientSecret();

        userWalletBpn = Configuration.getUserWalletBPN();
        userWalletClientId = Configuration.getUserWalletClientId();
        userWalletClientSecret = Configuration.getUserWalletClientSecret();
    }

    @SneakyThrows
    @Then("Create a wallet with the user's BPN if not created")
    public void createAWalletWithTheUserSBPNIfNotCreated() {
        createWalletResponse = CommonUtils.createWallet(Configuration.getUserWalletBPN(), baseWalletAccessToken, client);
        Assert.assertNotNull(createWalletResponse);
        Assert.assertTrue(createWalletResponse.code() == HttpStatus.SC_CREATED || createWalletResponse.code() == HttpStatus.SC_CONFLICT);
    }

    @SneakyThrows
    @Then("Issue any custom type VC to wallet")
    public void issueAnyCustomTypeVCToWallet() {
        customVCType = UUID.randomUUID().toString();
        VerifiableCredential vc = CommonUtils.getCustomTypeVerifiableCredential(customVCType);

        //issue VC to user wallet
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_ISSUER))
                .newBuilder()
                .addQueryParameter(StringPool.HOLDER_DID, userWalletDid)
                .build();

        RequestBody requestBody = RequestBody.create(vc.toJson(), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));

        issueCustomVcResponse = CommonUtils.callPostEndpoint(client, url.url().toString(), baseWalletAccessToken, requestBody);
    }

    @Then("Verify API response code, it should be 201")
    public void checkResponseCode() {
        Assert.assertNotNull(issueCustomVcResponse);
        Assert.assertEquals(issueCustomVcResponse.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Verify API response body , it should contain VC and type of VC as same as issued  type")
    public void verifyAPIResponseBodyItShouldContainVCAndTypeOfVCAsSameAsIssuedType() {
        Assert.assertNotNull(issueCustomVcResponse);
        Assert.assertEquals(issueCustomVcResponse.code(), HttpStatus.SC_CREATED);
        VerifiableCredential vc = new VerifiableCredential(getObjectMapper().readValue(Objects.requireNonNull(issueCustomVcResponse.body()).string(), Map.class));
        Assert.assertNotNull(vc);
        Assert.assertTrue(vc.getTypes().contains(customVCType));
    }

    @SneakyThrows
    @Then("Get did of user's wallet")
    public void getDidOfThatWallet() {
        if (createWalletResponse.code() == HttpStatus.SC_CREATED) {
            //wallet is created, get did from response
            Map map = getObjectMapper().readValue(Objects.requireNonNull(createWalletResponse.body()).string(), Map.class);
            userWalletDid = map.get(StringPool.DID).toString();
        } else {
            //wallet is already created. get did using API
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_WALLETS))
                    .newBuilder()
                    .addPathSegment(userWalletBpn)
                    .build();

            Request request = new Request.Builder()
                    .url(url.url())
                    .header(StringPool.HEADER_AUTHORIZATION, baseWalletAccessToken)
                    .get()
                    .build();
            Response userGetWalletResponse = client.newCall(request).execute();
            Assert.assertEquals(userGetWalletResponse.code(), HttpStatus.SC_OK);
            Map map = getObjectMapper().readValue(Objects.requireNonNull(userGetWalletResponse.body()).string(), Map.class);
            userWalletDid = map.get(StringPool.DID).toString();
        }
    }


    @SneakyThrows
    @Then("Check that credential is issued using issuer API")
    public void checkThatCredentialIsIssuedUsingIssuerAPI() {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_CREDENTIAL_ISSUER))
                .newBuilder()
                .addQueryParameter(StringPool.HOLDER_IDENTIFIER, userWalletDid)
                .addQueryParameter(StringPool.TYPE, customVCType)
                .build();

        Request request = new Request.Builder()
                .url(url.url())
                .header(StringPool.HEADER_AUTHORIZATION, baseWalletAccessToken)
                .get()
                .build();
        Response getVCResponse = client.newCall(request).execute();
        Assert.assertEquals(getVCResponse.code(), HttpStatus.SC_OK);
        Map map = getObjectMapper().readValue(Objects.requireNonNull(getVCResponse.body()).string(), Map.class);
        ArrayList<Map<String, Object>> vcs = (ArrayList<Map<String, Object>>) map.get(StringPool.CONTENT);
        VerifiableCredential verifiableCredential = new VerifiableCredential(vcs.get(0));
        Assert.assertTrue(verifiableCredential.getTypes().contains(customVCType));
    }

    @Then("Create access_token using the client_id and client_secret of the user's wallet")
    public void createAccess_tokenUsingTheClient_idAndClient_secretOfTheUserSWallet() {
        userAccessToken = AuthenticationHelper.getAccessToken(userWalletClientId, userWalletClientSecret);
        Assert.assertTrue(StringUtils.isNoneBlank(userAccessToken));
    }

    @Then("Get issued type credential using holder API")
    public void getIssuedTypeCredentialUsingHolderAPI() {
        VerifiableCredential userVC = CommonUtils.getUserVC(client, userAccessToken, Configuration.getBaseWalletBPN(), customVCType, 1, 0, StringPool.CREATED_AT);
        Assert.assertNotNull(userVC);
    }

    @Then("Create VP as JWT of membership VC")
    public void createVPAsJWTOfMembershipVC() {
        audience = "smart";
        asJwt = true;
        membershipVPAsJwt = CommonUtils.createVP(audience, asJwt, List.of(membershipVC), client, baseWalletAccessToken);
    }

    @Then("Validate membership VP")
    public void validateMembershipVP() {
        final boolean withCredentialExpiryDate = true;
        CommonUtils.validateVP(audience, withCredentialExpiryDate, membershipVPAsJwt, baseWalletAccessToken, client);
    }

    @SneakyThrows
    @Then("Try to issue dismentaler VC again")
    public void tryToIssueDismentalerVCAgain() {
        issueDuplicateVCResponse = CommonUtils.issueDismantleVC(client, randomBpn, baseWalletAccessToken);
        Assert.assertNotNull(issueDuplicateVCResponse);
        Assert.assertEquals(issueDuplicateVCResponse.code(), HttpStatus.SC_CONFLICT);
    }

    @Then("Validate dismentaler VC")
    public void validateDismentalerVC() {
        CommonUtils.validateVC(client, dismantlerVC, baseWalletAccessToken, true);
    }

    @Then("Create VP as JWT of dismentaler VC")
    public void createVPAsJWTOfDismentalerVC() {
        audience = "smart";
        asJwt = true;
        dismantlerVPAsJwt = CommonUtils.createVP(audience, asJwt, List.of(dismantlerVC), client, baseWalletAccessToken);
    }

    @Then("Validate dismentaler VP")
    public void validateDismentalerVP() {
        final boolean withCredentialExpiryDate = true;
        CommonUtils.validateVP(audience, withCredentialExpiryDate, dismantlerVPAsJwt, baseWalletAccessToken, client);
    }

    @SneakyThrows
    @Then("Issue a framework verifiable credential\\(VC) to that wallet")
    public void issueAFrameworkVerifiableCredentialVCToThatWallet() {
        Map<String, String> body = Map.of(StringPool.HOLDER_IDENTIFIER, randomBpn, StringPool.TYPE, BEHAVIOR_TWIN_CREDENTIAL, "contract-template", "https://example.com", "contract-version", "1.0.0");
        Response response = CommonUtils.issueFrameworkVC(client, randomBpn, body, baseWalletAccessToken);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_CREATED);
        frameworkVc = new VerifiableCredential(getObjectMapper().readValue(Objects.requireNonNull(response.body()).string(), Map.class));
    }

    @Then("Validate framework VC")
    public void validateFrameworkVC() {
        final boolean withCredentialExpiryDate = true;
        CommonUtils.validateVC(client, frameworkVc, baseWalletAccessToken, true);
    }

    @Then("Create VP as JWT of framework VC")
    public void createVPAsJWTOfFrameworkVC() {
        audience = "smart";
        asJwt = true;
        frameworkVPAsJwt = CommonUtils.createVP(audience, asJwt, List.of(frameworkVc), client, baseWalletAccessToken);
    }

    @Then("Validate framework VP")
    public void validateFrameworkVP() {
        Assert.assertNotNull(frameworkVPAsJwt);
        final boolean withCredentialExpiryDate = true;
        CommonUtils.validateVP(audience, withCredentialExpiryDate, frameworkVPAsJwt, baseWalletAccessToken, client);
    }

    @Then("Get BPN credential")
    public void getBPNCredential() {
        bpnCredential = getAndVerifyVC(client, baseWalletAccessToken, randomBpn, StringPool.BPN_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
    }

    @Then("Get Summary credential")
    public void getSummaryCredential() {
        summaryCredential = getAndVerifyVC(client, baseWalletAccessToken, randomBpn, StringPool.SUMMARY_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
    }

    @Then("Create VP as JWT of BPN credential and Summary credential")
    public void createVPAsJWTOfBPNCredentialAndSummaryCredential() {
        audience = "smart";
        asJwt = true;
        vpAsJwtOfMultipleVC = CommonUtils.createVP(audience, asJwt, List.of(bpnCredential, summaryCredential), client, baseWalletAccessToken);
    }

    @Then("Validate create VP\\(JWT)")
    public void validateCreateVPJWT() {
        final boolean withCredentialExpiryDate = true;
        CommonUtils.validateVP(audience, withCredentialExpiryDate, vpAsJwtOfMultipleVC, baseWalletAccessToken, client);
    }

    @SneakyThrows
    @Then("Issue any random type of credential to self wallet using holder API")
    public void issueAnyRandomTypeOfCredentialToSelfWalletUsingHolderAPI() {
        customVCType = UUID.randomUUID().toString();
        VerifiableCredential vc = CommonUtils.getCustomTypeVerifiableCredential(customVCType, userWalletBpn);

        //issue VC to user wallet
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_CREDENTIAL_HOLDER))
                .newBuilder()
                .addQueryParameter("holderDid", userWalletDid)
                .build();

        RequestBody requestBody = RequestBody.create(vc.toJson(), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));

        issueCustomVcResponse = CommonUtils.callPostEndpoint(client, url.url().toString(), userAccessToken, requestBody);
        Assert.assertEquals(issueCustomVcResponse.code(), HttpStatus.SC_CREATED);
    }

    @Then("Get this type of credential using holder API")
    public void getThisTypeOfCredentialUsingHolderAPI() {
        customTypeVC = CommonUtils.getUserVC(client, userAccessToken, userWalletBpn, customVCType, 1, 0, StringPool.CREATED_AT);
        Assert.assertNotNull(customTypeVC);
    }

    @Then("Validate this VC")
    public void validateThisVC() {
        CommonUtils.validateVC(client, customTypeVC, userAccessToken, true);
    }

    @Then("Create VP as JWT of issued VC")
    public void createVPAsJWTOfIssuedVC() {
        audience = "smart";
        vpAsJwtOfCustomVC = CommonUtils.createVP(audience, true, List.of(customTypeVC), client, userAccessToken);
    }

    @Then("Validate custom type of  VP")
    public void validateCustomTypeOfVP() {
        CommonUtils.validateVP(audience, true, vpAsJwtOfCustomVC, userAccessToken, client);
    }
}