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

package org.eclipse.tractusx.managedidentitywallets.revocation.controllers;

import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.Map;
import java.util.TreeMap;

public class BaseController {

    /**
     * Gets bpn from token.
     *
     * @param principal the principal
     * @return the bpn from token
     */
    public String getBPNFromToken(Principal principal) {
        Object principal1 = ((JwtAuthenticationToken) principal).getPrincipal();
        Jwt jwt = (Jwt) principal1;
        // this will misbehave if we have more then one claims with different case
        // ie. BPN=123456 and bpn=789456
        Map<String, Object> claims = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        claims.putAll(jwt.getClaims());
        Validate.isFalse(claims.containsKey(StringPool.BPN))
                .launch(new SecurityException("Invalid token, BPN not found"));
        return claims.get(StringPool.BPN).toString();
    }
}
