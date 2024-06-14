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

package org.eclipse.tractusx.managedidentitywallets.controller;


import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.apidocs.IssuersCredentialControllerApiDocs;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dto.CredentialVerificationRequest;
import org.eclipse.tractusx.managedidentitywallets.service.revocation.RevocationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * The type Revocation controller.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Verifiable Credential - Revoke")
public class RevocationController extends BaseController {


    private final RevocationService revocationService;


    /**
     * Revoke credential .
     *
     * @param credentialVerificationRequest the credential verification request
     * @param token                         the token
     * @param principal                     the principal
     * @return the response entity
     */
    @PutMapping(path = RestURI.CREDENTIALS_REVOKE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @IssuersCredentialControllerApiDocs.ValidateVerifiableCredentialApiDocs
    public ResponseEntity<Map<String, Object>> revokeCredential(@RequestBody CredentialVerificationRequest credentialVerificationRequest,
                                                                @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token, Principal principal) {
        revocationService.revokeCredential(credentialVerificationRequest, getBPNFromToken(principal), token);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Credential has been revoked"));

    }

}
