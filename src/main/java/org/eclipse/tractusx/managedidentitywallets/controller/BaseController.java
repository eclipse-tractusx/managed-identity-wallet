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

package org.eclipse.tractusx.managedidentitywallets.controller;

import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;

/**
 * The type Base controller.
 */
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

        Validate.isFalse(jwt.getClaims().containsKey("BPN")).launch(new ForbiddenException("Invalid token, BPN not found"));

        return jwt.getClaims().get("BPN").toString();
    }
}
