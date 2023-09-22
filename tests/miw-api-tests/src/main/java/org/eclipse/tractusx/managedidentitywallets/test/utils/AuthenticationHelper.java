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

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import okhttp3.*;
import org.eclipse.tractusx.managedidentitywallets.test.config.Configuration;

import java.util.Map;

/**
 * The type Authentication helper.
 */
@UtilityClass
public class AuthenticationHelper {

    /**
     * Gets access token.
     *
     * @param clientId     the client id
     * @param clientSecret the client secret
     * @return the access token
     */
    @SneakyThrows
    public static String getAccessToken(String clientId, String clientSecret) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "client_id=" + clientId + "&grant_type=client_credentials&client_secret=" + clientSecret + "&scope=openid");
        Request request = new Request.Builder()
                .url(Configuration.getAuthServer() + "realms/" + Configuration.getRealm() + "/protocol/openid-connect/token")
                .method("POST", body)
                .addHeader(StringPool.HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded")
                .build();
        Response response = client.newCall(request).execute();

        Map<String, Object> map = CommonUtils.getObjectMapper().readValue(response.body().string(), Map.class);
        return map.get("token_type") + " " + map.get("access_token");
    }

    public static void main(String[] args) {
        System.out.println(getAccessToken(Configuration.getBaseWalletClientId(), Configuration.getBaseWalletClientSecret()));
    }
}
