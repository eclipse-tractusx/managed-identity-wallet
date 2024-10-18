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

package org.eclipse.tractusx.managedidentitywallets.utils;

import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.TreeMap;

/**
 * The type Bpn validator.
 */
public class BpnValidator implements OAuth2TokenValidator<Jwt> {

    OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, StringPool.BPN_NOT_FOUND, null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        //this will misbehave if we have more then one claims with different case
        // ie. BPN=123456 and bpn=789456
        Map<String, Object> claims = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        claims.putAll(jwt.getClaims());
        if (claims.containsKey(StringPool.BPN)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
