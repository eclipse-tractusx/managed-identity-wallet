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

package org.eclipse.tractusx.managedidentitywallets.test.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.curiousoddman.rgxgen.RgxGen;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import okhttp3.*;
import org.apache.http.HttpStatus;
import org.eclipse.tractusx.managedidentitywallets.test.config.Configuration;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;

import java.util.*;

/**
 * The type Common utils.
 */
@UtilityClass
public class CommonUtils {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Gets random bpm number.
     *
     * @return the random bpm number
     */
    public static String getRandomBpmNumber() {
        RgxGen rgxGen = new RgxGen(StringPool.BPN_NUMBER_REGEX);
        return rgxGen.generate();
    }

    /**
     * Gets object mapper.
     *
     * @return the object mapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }


    /**
     * Create wallet response.
     *
     * @param bpn         the bpn
     * @param accessToken the access token
     * @param client      the client
     * @return the response
     */
    @SneakyThrows
    public static Response createWallet(String bpn, String accessToken, OkHttpClient client) {
        Map<String, String> body = Map.of(StringPool.BPN, bpn,
                StringPool.NAME, bpn);

        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));

        Request request = new Request.Builder()
                .url(Configuration.getMIWHost() + StringPool.URI_WALLETS)
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .header(StringPool.HEADER_CONTENT_TYPE, StringPool.CONTENT_TYPE_APPLICATION_JSON)
                .post(requestBody)
                .build();

        return client.newCall(request).execute();
    }

    /**
     * Call post endpoint response.
     *
     * @param client      the client
     * @param url         the url
     * @param accessToken the access token
     * @param requestBody the request body
     * @return the response
     */
    @SneakyThrows
    public static Response callPostEndpoint(OkHttpClient client, String url, String accessToken, RequestBody requestBody) {
        Request request = new Request.Builder()
                .url(url)
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .header(StringPool.HEADER_CONTENT_TYPE, StringPool.CONTENT_TYPE_APPLICATION_JSON)
                .post(requestBody)
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Gets and verify vc.
     *
     * @param client      the client
     * @param accessToken the access token
     * @param bpn         the bpn
     * @param type        the type
     * @param pageSize    the page size
     * @param pageNumber  the page number
     * @param sortBy      the sort by
     * @return the and verify vc
     */
    @SneakyThrows
    public static VerifiableCredential getAndVerifyVC(OkHttpClient client, String accessToken, String bpn, String type, int pageSize, int pageNumber, String sortBy) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_CREDENTIAL_ISSUER))
                .newBuilder()
                .addQueryParameter(StringPool.HOLDER_IDENTIFIER, bpn)
                .addQueryParameter(StringPool.TYPE, type)
                .addQueryParameter(StringPool.PAGE_NUMBER, String.valueOf(pageNumber))
                .addQueryParameter(StringPool.SIZE, String.valueOf(pageSize))
                .addQueryParameter(StringPool.SORT_COLUMN, sortBy)
                .build();

        Request request = new Request.Builder()
                .url(url.url())
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .header(StringPool.HEADER_CONTENT_TYPE, StringPool.CONTENT_TYPE_APPLICATION_JSON)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), HttpStatus.SC_OK);
            Assert.assertNotNull(response.body());
            Map map = getObjectMapper().readValue(response.body().string(), Map.class);
            Assert.assertTrue(map.containsKey(StringPool.CONTENT));
            ArrayList<Map<String, Object>> vcs = (ArrayList<Map<String, Object>>) map.get(StringPool.CONTENT);
            VerifiableCredential verifiableCredential = new VerifiableCredential(vcs.get(0));
            Assert.assertTrue(verifiableCredential.getTypes().contains(type));
            Assert.assertEquals(verifiableCredential.getCredentialSubject().get(0).get(StringPool.TYPE).toString(), type);
            return verifiableCredential;
        }
    }

    @SneakyThrows
    public static VerifiableCredential getUserVC(OkHttpClient client, String accessToken, String bpn, String type, int pageSize, int pageNumber, String sortBy) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_CREDENTIAL_HOLDER))
                .newBuilder()
                .addQueryParameter(StringPool.ISSUER_IDENTIFIER, bpn)
                .addQueryParameter(StringPool.TYPE, type)
                .addQueryParameter(StringPool.PAGE_NUMBER, String.valueOf(pageNumber))
                .addQueryParameter(StringPool.SIZE, String.valueOf(pageSize))
                .addQueryParameter(StringPool.SORT_COLUMN, sortBy)
                .build();

        Request request = new Request.Builder()
                .url(url.url())
                .header(StringPool.HEADER_AUTHORIZATION, accessToken)
                .header(StringPool.HEADER_CONTENT_TYPE, StringPool.CONTENT_TYPE_APPLICATION_JSON)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), HttpStatus.SC_OK);
            Assert.assertNotNull(response.body());
            Map map = getObjectMapper().readValue(response.body().string(), Map.class);
            Assert.assertTrue(map.containsKey(StringPool.CONTENT));
            ArrayList<Map<String, Object>> vcs = (ArrayList<Map<String, Object>>) map.get(StringPool.CONTENT);
            VerifiableCredential verifiableCredential = new VerifiableCredential(vcs.get(0));
            Assert.assertTrue(verifiableCredential.getTypes().contains(type));
            Assert.assertEquals(verifiableCredential.getCredentialSubject().get(0).get(StringPool.TYPE).toString(), type);
            return verifiableCredential;
        }
    }

    public static VerifiableCredential getCustomTypeVerifiableCredential(String customVCType) throws JsonProcessingException {
        return getCustomTypeVerifiableCredential(customVCType, Configuration.getBaseWalletBPN());
    }

    @NotNull
    public static VerifiableCredential getCustomTypeVerifiableCredential(String customVCType, String issuer) throws JsonProcessingException {
        String vcString = """
                {
                  "@context": [
                    "https://www.w3.org/2018/credentials/v1",
                    "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
                  ],
                  "id": "did:web:localhost.in#123456789",
                  "type": [
                    "VerifiableCredential",
                    "customVCType"
                  ],
                  "issuer": "issuerBPN",
                  "issuanceDate": "2023-05-04T07:36:03.633Z",
                  "expirationDate":"2030-05-04T07:36:03.633Z",
                  "credentialSubject": {
                    "id": "https://localhost/.well-known/participant.json",
                    "type": "customVCType"
                  },
                  "proof": {
                    "type": "JsonWebSignature2020",
                    "created": "2023-05-04T07:36:04.079Z",
                    "proofPurpose": "assertionMethod",
                    "verificationMethod": "did:web:localhost",
                    "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..iHki8WC3nPfcSRkC_AV4tXh0ikfT7BLPTGc_0ecI8zontTmJLqwcpPfAt0PFsoo3SkZgc6j636z55jj5tagBc-OKoiDu7diWryNAnL9ASsmWJyrPhOKVARs6x6PxVaTFBuyCfAHZeipxmkcYfNB_jooIXO2HuRcL2odhsQHELkGc5IDD-aBMWyNpfVAaYQ-cCzvDflZQlsowziUKfMkBfwpwgMdXFIgKWYdDIRvzA-U-XiC11-6QV7tPeKsMguEU0F5bh8cCEm2rooqXtENcsM_7cqFdQoOyblJyM-agoz2LUTj9QIdn9_gnNkGN-2U7_qBJWmHkK1Hm_mHqcNeeQw"
                  }
                }
                """;
        vcString = vcString.replaceAll("customVCType", customVCType).replaceAll("issuerBPN", issuer);
        return new VerifiableCredential(CommonUtils.getObjectMapper().readValue(String.format(vcString, customVCType), Map.class));
    }


    @SneakyThrows
    public static void validateVC(OkHttpClient client, VerifiableCredential credential, String accessToken, boolean withCredentialExpiryDate) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_VALIDATE_VC))
                .newBuilder()
                .addQueryParameter(StringPool.WITH_CREDENTIALS_EXPIRY_DATE, String.valueOf(withCredentialExpiryDate))
                .build();
        RequestBody requestBody = RequestBody.create(credential.toJson(), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        Response response = CommonUtils.callPostEndpoint(client, url.url().toString(), accessToken, requestBody);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_OK);
        Assert.assertNotNull(response.body());
        Map map = getObjectMapper().readValue(response.body().string(), Map.class);
        Assert.assertTrue((Boolean) map.get(StringPool.VALID));
        Assert.assertTrue((Boolean) map.get(StringPool.VALIDATE_EXPIRY_DATE));
    }

    @SneakyThrows
    public static Map createVP(String audience, boolean asJwt, List<VerifiableCredential> vcs, OkHttpClient client, String accessToken) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_CREATE_VP))
                .newBuilder()
                .addQueryParameter(StringPool.AUDIENCE, audience)
                .addQueryParameter(StringPool.AS_JWT, String.valueOf(asJwt))
                .build();
        Map<String, Object> body = Map.of("verifiableCredentials", vcs);
        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        Response responseVPasJWT = CommonUtils.callPostEndpoint(client, url.url().toString(), accessToken, requestBody);
        Assert.assertNotNull(responseVPasJWT);
        Assert.assertEquals(responseVPasJWT.code(), HttpStatus.SC_CREATED);
        Assert.assertNotNull(responseVPasJWT.body());
        Map vp = getObjectMapper().readValue(responseVPasJWT.body().string(), Map.class);
        Assert.assertNotNull(vp.get("vp"));
        return vp;
    }

    @SneakyThrows
    public static void validateVP(String audience, boolean withCredentialExpiryDate, Map vpAsJwt, String accessToken, OkHttpClient client) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(Configuration.getMIWHost() + StringPool.URI_VALIDATE_VP))
                .newBuilder()
                .addQueryParameter(StringPool.AUDIENCE, audience)
                .addQueryParameter(StringPool.AS_JWT, String.valueOf(true))
                .addQueryParameter(StringPool.WITH_CREDENTIALS_EXPIRY_DATE, String.valueOf(withCredentialExpiryDate))
                .build();
        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(vpAsJwt), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        Response response = CommonUtils.callPostEndpoint(client, url.url().toString(), accessToken, requestBody);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.code(), HttpStatus.SC_OK);
        Assert.assertNotNull(response.body());
        Map map = getObjectMapper().readValue(response.body().string(), Map.class);
        Assert.assertTrue((Boolean) map.get(StringPool.VALID));
        Assert.assertTrue((Boolean) map.get(StringPool.VALIDATE_EXPIRY_DATE));
        Assert.assertTrue((Boolean) map.get(StringPool.VALIDATE_JWT_EXPIRY_DATE));
        Assert.assertTrue((Boolean) map.get(StringPool.VALIDATE_AUDIENCE));
    }


    @NotNull
    public Response issueDismantleVC(OkHttpClient client, String holderBpn, String baseWalletAccessToken) throws JsonProcessingException {
        String url = Configuration.getMIWHost() + StringPool.URI_ISSUE_DISMANTLER_VC;
        Map<String, Object> body = Map.of(StringPool.BPN, holderBpn, "activityType", "vehicleDismantle", "allowedVehicleBrands", Set.of("Company1"));
        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        Response response = CommonUtils.callPostEndpoint(client, url, baseWalletAccessToken, requestBody);
        return response;
    }

    @SneakyThrows
    public Response issueFrameworkVC(OkHttpClient client, String holderBpn, Map<String, String> body, String accessToken) {
        String url = Configuration.getMIWHost() + StringPool.URI_ISSUE_FRAMEWORK_VC;
        RequestBody requestBody = RequestBody.create(getObjectMapper().writeValueAsString(body), MediaType.parse(StringPool.CONTENT_TYPE_APPLICATION_JSON));
        return CommonUtils.callPostEndpoint(client, url, accessToken, requestBody);

    }
}
