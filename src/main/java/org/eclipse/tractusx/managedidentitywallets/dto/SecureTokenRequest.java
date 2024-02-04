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

package org.eclipse.tractusx.managedidentitywallets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class SecureTokenRequest {
    @NotBlank
    private String audience;

    @NotBlank
    @NotNull
    @JsonProperty("client_id")
    private String clientId;

    @NotBlank
    @NotNull
    @JsonProperty("client_secret")
    private String clientSecret;

    @NotBlank
    @NotNull
    @JsonProperty("grant_type")
    private String grantType;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("bearer_access_alias")
    private String bearerAccessAlias;

    @JsonProperty("bearer_access_scope")
    private String bearerAccessScope;

    public boolean assertValidWithScopes() {
        return bearerAccessScope != null && accessToken == null && !bearerAccessScope.isEmpty();
    }

    public boolean assertValidWithAccessToken() {
        return accessToken != null && bearerAccessScope == null;
    }

    @Override
    public String toString() {
        return "SecureTokenRequest{" +
                "audience='" + audience + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", grantType='" + grantType + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", bearerAccessAlias='" + bearerAccessAlias + '\'' +
                ", bearerAccessScope='" + bearerAccessScope + '\'' +
                '}';
    }
}