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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.tractusx.managedidentitywallets.apidocs.DidDocumentControllerApiDocs.DidOrBpnParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.CreateWalletApiDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.PageNumberParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.RetrieveWalletApiDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.RetrieveWalletsApiDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.SizeParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.SortColumnParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.SortTypeParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.WalletControllerApiDocs.StoreVerifiableCredentialApiDoc;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * The type Wallet controller.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallets")
public class WalletController extends BaseController {

    private final WalletService service;

    /**
     * Create wallet response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @CreateWalletApiDoc
    @PostMapping(path = RestURI.WALLETS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request, Principal principal) {
        log.debug("Received request to create wallet with BPN {}. authorized by BPN: {}", request.getBusinessPartnerNumber(), getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWallet(request, getBPNFromToken(principal)));
    }

    /**
     * Store credential response entity.
     *
     * @param data       the data
     * @param identifier the identifier
     * @return the response entity
     */
    @StoreVerifiableCredentialApiDoc
    @PostMapping(path = RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> storeCredential(@RequestBody Map<String, Object> data,
                                                               @DidOrBpnParameterDoc @PathVariable(name = "identifier") String identifier, Principal principal) {
        log.debug("Received request to store credential in wallet with identifier {}. authorized by BPN: {}", identifier, getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.storeCredential(data, identifier, getBPNFromToken(principal)));
    }

    /**
     * Gets wallet by bpn.
     *
     * @param identifier      the identifier
     * @param withCredentials the with credentials
     * @return the wallet by bpn
     */
    @RetrieveWalletApiDoc
    @GetMapping(path = RestURI.API_WALLETS_IDENTIFIER, produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<Wallet> getWalletByIdentifier( @DidOrBpnParameterDoc @PathVariable(name = "identifier") String identifier,
                                                        @RequestParam(name = "withCredentials", defaultValue = "false") boolean withCredentials,
                                                        Principal principal) {
        log.debug("Received request to retrieve wallet with identifier {}. authorized by BPN: {}", identifier, getBPNFromToken(principal));
        return ResponseEntity.status(HttpStatus.OK).body(service.getWalletByIdentifier(identifier, withCredentials, getBPNFromToken(principal)));
    }

    /**
     * Gets wallets.
     *
     * @return the wallets
     */
    @RetrieveWalletsApiDoc
    @GetMapping(path = RestURI.WALLETS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Wallet>> getWallets(
            @PageNumberParameterDoc @RequestParam(required = false, defaultValue = "0") int pageNumber,
            @SizeParameterDoc @RequestParam(required = false, defaultValue = Integer.MAX_VALUE
                    + "") int size,
            @SortColumnParameterDoc @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
            @SortTypeParameterDoc @RequestParam(required = false, defaultValue = "desc") String sortTpe) {
        log.debug("Received request to retrieve wallets");
        return ResponseEntity.status(HttpStatus.OK).body(service.getWallets(pageNumber, size, sortColumn, sortTpe));
    }
}
