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

package org.eclipse.tractusx.managedidentitywallets.test.cucumber.StepDefinition.wallet;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
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
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.test.utils.CommonUtils.getObjectMapper;

@Slf4j
public class WalletSteps {


    private String accessToken;
    private String userAccessToken;

    private Response createWalletResponse;

    private Map userWalletMap;

    private String bpn;

    private String clientId;

    private String clientSecret;

    private OkHttpClient client;

    private String userWalletClientId;

    private String userWalletClientSecret;

    private String userWalletBPN;

    private String customVCType;

    private Response storeVCResponse;


    @Before
    public void SetUp() {
        log.info("Setting up create wallet test");
        accessToken = null;
        userAccessToken = null;
        createWalletResponse = null;
        userWalletMap = null;
        bpn = null;
        clientId = null;
        clientSecret = null;
        userWalletClientId = null;
        userWalletClientSecret = null;
        customVCType = null;
        userWalletBPN = null;
        client = new OkHttpClient();
    }


    @Given("keycloak client_id, client_secret of base wallet and MIW application host")
    public void keycloakClient_idClient_secretAndMIWApplicationHost() {
        clientId = Configuration.getBaseWalletClientId();
        clientSecret = Configuration.getBaseWalletClientSecret();
    }


    @Given("Keycloak client_id and client_secret of user wallet, MIW host and users BPN")
    public void keycloakClient_idAndClient_secretOfUserWalletMIWHostAndUsersBPN() {
        userWalletClientId = Configuration.getUserWalletClientId();
        userWalletClientSecret = Configuration.getUserWalletClientSecret();
        userWalletBPN = Configuration.getUserWalletBPN();
    }

    @Then("Create access_token using client_id and client_secret")
    public void createAccess_tokenUsingClient_idAndClient_secret() {
        accessToken = AuthenticationHelper.getAccessToken(clientId, clientSecret);
        Assert.assertTrue(StringUtils.isNoneBlank(accessToken));
    }

    @SneakyThrows
    @Then("Create random BPN and create wallet using this BPN number")
    public void createWalletUsingRandomBPN() {
        bpn = CommonUtils.getRandomBpmNumber();
        createWalletResponse = CommonUtils.createWallet(bpn, accessToken, client);
    }

    @Then("Verify API response code, it should be 201")
    public void checkResponseCode() {
        Assert.assertNotNull(createWalletResponse);
        Assert.assertEquals(createWalletResponse.code(), HttpStatus.SC_CREATED);
    }

    @SneakyThrows
    @Then("Verify API response body, it should contains wallet information")
    public void checkResponseBody() {
        Assert.assertNotNull(createWalletResponse.body());
        Map map = getObjectMapper().readValue(createWalletResponse.body().string(), Map.class);
        Assert.assertNotNull(map);
        Assert.assertEquals(map.get(StringPool.BPN).toString(), bpn);
        Assert.assertNotNull(map.get(StringPool.DID_DOCUMENT));
        //did document should be valid
        DidDocument didDocument = new DidDocument((Map<String, Object>) map.get(StringPool.DID_DOCUMENT));
        Assert.assertNotNull(didDocument);
    }

    @SneakyThrows
    @Then("Verify did document is resolvable")
    public void verifyDidDocumentIsResolvable() {


        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost()))
                .newBuilder()
                .addPathSegment(bpn)
                .addPathSegment("did.json")
                .build();

        Request request = new Request.Builder()
                .url(url.url())
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), HttpStatus.SC_OK);
            Assert.assertNotNull(response.body());
            DidDocument didDocument = new DidDocument(getObjectMapper().readValue(response.body().string(), Map.class));
            Assert.assertNotNull(didDocument);
        }
    }

    @Then("Verify BPN credential should be issued")
    public void verifyBPNCredentialShouldBeIssued() {
        CommonUtils.getAndVerifyVC(client, accessToken, bpn, StringPool.BPN_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
    }


    @Then("Verify Summary credential should be issued with BPN credential entry")
    public void verifySummaryCredentialShouldBeIssuedWithBPNCredentialEntry() {
        VerifiableCredential summaryCredential = CommonUtils.getAndVerifyVC(client, accessToken, bpn, StringPool.SUMMARY_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
        Assert.assertTrue(((ArrayList<String>) summaryCredential.getCredentialSubject().get(0).get(StringPool.ITEMS)).contains(StringPool.BPN_CREDENTIAL));
    }


    @SneakyThrows
    @Then("Get any one wallet information and take BPN of this wallet")
    public void getAnyOneWalletInformationAndTakeBPNOfThisWallet() {

        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_WALLETS))
                .newBuilder()
                .addQueryParameter(StringPool.PAGE_NUMBER, String.valueOf(0))
                .addQueryParameter(StringPool.SIZE, String.valueOf(1))
                .addQueryParameter(StringPool.SORT_COLUMN, StringPool.CREATED_AT)
                .addQueryParameter("sortTpe", "desc")
                .build();

        Request request = new Request.Builder()
                .url(url.url())
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), HttpStatus.SC_OK);
            Assert.assertNotNull(response.body());
            Map map = getObjectMapper().readValue(response.body().string(), Map.class);
            Assert.assertTrue(map.containsKey(StringPool.CONTENT));
            ArrayList<Map<String, Object>> wallets = (ArrayList<Map<String, Object>>) map.get(StringPool.CONTENT);
            bpn = wallets.get(0).get(StringPool.BPN).toString();
        }

    }

    @SneakyThrows
    @Then("Create wallet with this BPN")
    public void createWalletWithThisBPN() {
        Map<String, String> body = Map.of(StringPool.BPN, bpn,
                StringPool.NAME, bpn);
        RequestBody requestBody = RequestBody.create(CommonUtils.getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        Request request = new Request.Builder()
                .url(Configuration.getMIWHost() + StringPool.URI_WALLETS)
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .header(StringPool.HEADER_CONTENT_TYPE, StringPool.CONTENT_TYPE_APPLICATION_JSON)
                .post(requestBody)
                .build();
        createWalletResponse = client.newCall(request).execute();
    }

    @Then("Verify wallet creation should be failed with http status code 409")
    public void verifyWalletCreationShouldBeFailedWithHttpStatusCode() {
        Assert.assertEquals(createWalletResponse.code(), HttpStatus.SC_CONFLICT);
    }

    @Given("Keycloak client_id and client_secret of base wallet, client_id and client_secret of user wallet, MIW host and users BPN")
    public void keycloakClient_idAndClient_secretOfBaseWalletClient_idAndClient_secretOfUserWalletMIWHostAndUsersBPN() {
        clientId = Configuration.getBaseWalletClientId();
        clientSecret = Configuration.getBaseWalletClientSecret();

        userWalletClientId = Configuration.getUserWalletClientId();
        userWalletClientSecret = Configuration.getUserWalletClientSecret();
    }

    @Then("Create access_token using client_id and client_secret of the base wallet")
    public void createAccess_tokenUsingClient_idAndClient_secretOfTheBaseWallet() {
        accessToken = AuthenticationHelper.getAccessToken(clientId, clientSecret);
        Assert.assertTrue(StringUtils.isNoneBlank(accessToken));
    }

    @SneakyThrows
    @Then("Create a wallet with the user's BPN if not created")
    public void createAWalletWithTheUserSBPNIfNotCreated() {
        bpn = Configuration.getUserWalletBPN();
        createWalletResponse = CommonUtils.createWallet(bpn, accessToken, client);
        Assert.assertNotNull(createWalletResponse);
        Assert.assertTrue(createWalletResponse.code() == HttpStatus.SC_CREATED || createWalletResponse.code() == HttpStatus.SC_CONFLICT);
    }

    @Then("Create access_token using the client_id and client_secret of the user's wallet")
    public void createAccess_tokenUsingTheClient_idAndClient_secretOfTheUserSWallet() {
        userAccessToken = AuthenticationHelper.getAccessToken(userWalletClientId, userWalletClientSecret);
        Assert.assertTrue(StringUtils.isNoneBlank(userAccessToken));
    }

    @SneakyThrows
    @Then("Get user wallet with credentials")
    public void getUserWalletWithCredentials() {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_WALLETS + "/" + bpn))
                .newBuilder()
                .addQueryParameter(StringPool.WITH_CREDENTIALS, String.valueOf(true))
                .build();

        Request request = new Request.Builder()
                .url(url.url())
                .header(StringPool.HEADER_AUTHORIZATION, userAccessToken)
                .header(StringPool.HEADER_CONTENT_TYPE, StringPool.CONTENT_TYPE_APPLICATION_JSON)
                .get()
                .build();

        Response userGetWalletResponse = client.newCall(request).execute();
        Assert.assertEquals(userGetWalletResponse.code(), HttpStatus.SC_OK);
        Assert.assertNotNull(userGetWalletResponse.body());
        userWalletMap = getObjectMapper().readValue(userGetWalletResponse.body().string(), Map.class);
        Assert.assertTrue(userWalletMap.containsKey(StringPool.VERIFIABLE_CREDENTIALS));
    }

    @SneakyThrows
    @Then("Verify that the user must have DID Document")
    public void verifyThatTheUserMustHaveDIDDocument() {
        Assert.assertTrue(userWalletMap.containsKey(StringPool.DID_DOCUMENT));
    }

    @SneakyThrows
    @Then("Verify that the user must have BPN and Summary credentials")
    public void verifyThatTheUserMustHaveBPNAndSummaryCredentials() {
        CommonUtils.getUserVC(client, userAccessToken, Configuration.getBaseWalletBPN(), StringPool.BPN_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
        CommonUtils.getUserVC(client, userAccessToken, Configuration.getBaseWalletBPN(), StringPool.SUMMARY_CREDENTIAL, 1, 0, StringPool.CREATED_AT);
    }


    @SneakyThrows
    @Then("Store any custom VC in wallet")
    public void storeAnyCustomVCInWallet() {
        customVCType = UUID.randomUUID().toString();
        VerifiableCredential vc = CommonUtils.getCustomTypeVerifiableCredential(customVCType);

        //store VC
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_WALLETS))
                .newBuilder()
                .addPathSegment(Configuration.getUserWalletBPN())
                .addPathSegment("credentials")
                .build();

        RequestBody requestBody = RequestBody.create(vc.toJson(), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));

        storeVCResponse = CommonUtils.callPostEndpoint(client, url.url().toString(), userAccessToken, requestBody);
    }


    @Then("verify API response, status should be 201")
    public void verifyAPIResponseStatusShouldBe() {
        Assert.assertNotNull(storeVCResponse);
        Assert.assertEquals(storeVCResponse.code(), HttpStatus.SC_CREATED);
    }

    @Then("Get this stored VC using holder API, it should return stored VC")
    public void getThisStoredVCUsingHolderAPI() {
        VerifiableCredential userVC = CommonUtils.getUserVC(client, userAccessToken, "", customVCType, 1, 0, StringPool.CREATED_AT);
        Assert.assertNotNull(userVC);
    }

    @After()
    public static void teardown(Scenario scenario) {

    }


}
