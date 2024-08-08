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

package org.eclipse.tractusx.managedidentitywallets.revocation;

import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.tractusx.managedidentitywallets.dto.StatusListRequest;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * The interface Revocation client.
 */
@FeignClient(value = "RevocationService", url = "${miw.revocation.url}", configuration = RevocationClientConfig.class)
public interface RevocationClient {

    /**
     * Gets status list credential.
     *
     * @param issuerBpn the issuer BPN
     * @param status    the status
     * @param index     the index
     * @param token     the token
     * @return the status list credential
     */
    @GetMapping(path = "/api/v1/revocations/credentials/{issuerBpn}/{status}/{index}", produces = MediaType.APPLICATION_JSON_VALUE)
    VerifiableCredential getStatusListCredential(@PathVariable(name = "issuerBpn") String issuerBpn,
                                                 @PathVariable(name = "status") String status,
                                                 @PathVariable(name = "index") String index,
                                                 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token);


    /**
     * Gets status list entry.
     *
     * @param statusListRequest the status list request
     * @param token             the token
     * @return the status list entry
     */
    @PostMapping(path = "/api/v1/revocations/status-entry", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> getStatusListEntry(@RequestBody StatusListRequest statusListRequest,
                                           @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token);


    /**
     * Revoke credential.
     *
     * @param verifiableCredentialStatus the verifiable credential status
     * @param token                      the token
     */
    @PostMapping(path = "/api/v1/revocations/revoke", consumes = MediaType.APPLICATION_JSON_VALUE)
    void revokeCredential(@RequestBody VerifiableCredentialStatus verifiableCredentialStatus,
                          @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token);


    /**
     * Verify credential status map.
     *
     * @param verifiableCredentialStatus the verifiable credential status
     * @param token                      the token
     * @return the map
     */
    @PostMapping(path = "/api/v1/revocations/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> verifyCredentialStatus(@RequestBody VerifiableCredentialStatus verifiableCredentialStatus,
                                               @Parameter(hidden = true) @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token);

}
