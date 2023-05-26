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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * The type Wallet controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Wallets")
public class WalletController {

    private final WalletService service;

    /**
     * Create wallet response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @Operation(summary = "Create Wallet", description = "Permission: **add_wallets** \n\n Create a wallet and store it")
    @PostMapping(path = RestURI.WALLETS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWallet(request));
    }

    /**
     * Store credential response entity.
     *
     * @param data the data
     * @param bpn  the bpn
     * @return the response entity
     */
    @Operation(summary = "Store Verifiable Credential", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of wallet to extract credentials from must equal BPN of caller) \n\n Store a verifiable credential in the wallet of the given identifier")
    @PostMapping(path = RestURI.WALLETS_BY_BPN_CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> storeCredential(@RequestBody Map<String, Object> data, @PathVariable(name = "bpn") String bpn) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.storeCredential(data, bpn));
    }

    /**
     * Gets wallet by bpn.
     *
     * @param bpn the bpn
     * @return the wallet by bpn
     */
    @Operation(summary = "Retrieve wallet by identifier", description = "Permission: **view_wallets** OR **view_wallet** (The BPN of Wallet to retrieve must equal the BPN of caller) \n\n Retrieve single wallet by identifier, with or without its credentials")
    @GetMapping(path = RestURI.WALLETS_BY_BPN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> getWalletByBpn(@PathVariable(name = "bpn") String bpn) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWalletByBpn(bpn));
    }

    /**
     * Gets wallets.
     *
     * @return the wallets
     */
    @Operation(summary = "List of wallets", description = "Permission: **view_wallets** \n\n Retrieve list of registered wallets")
    @GetMapping(path = RestURI.WALLETS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Wallet>> getWallets() {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWallets());
    }
}