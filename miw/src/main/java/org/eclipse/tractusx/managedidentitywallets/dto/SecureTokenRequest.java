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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SecureTokenRequest {

    /**
     * SecureTokenRequestScope request scope, may be null if token is present
     */
    @JsonProperty("grantAccess")
    private SecureTokenRequestScope grantAccess;

    /**
     * SecureTokenRequestToken request token, may be null if scope is present
     */
    @JsonProperty("signToken")
    private SecureTokenRequestToken signToken;

    @JsonIgnore
    public Optional<SecureTokenRequestScope> getSecureTokenRequestScope() {
        return Optional.ofNullable(grantAccess);
    }

    @JsonIgnore
    public Optional<SecureTokenRequestToken> getSecureTokenRequestToken() {
        return Optional.ofNullable(signToken);
    }
}
