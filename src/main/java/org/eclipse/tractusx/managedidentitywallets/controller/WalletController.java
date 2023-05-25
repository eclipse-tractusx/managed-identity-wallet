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

/**
 * The type Wallet controller.
 */
@RestController
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    /**
     * Create wallet response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(path = RestURI.WALLETS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWallet(request));
    }

    /**
     * Gets wallet by bpn.
     *
     * @param bpn the bpn
     * @return the wallet by bpn
     */
    @GetMapping(path = RestURI.WALLETS_BY_BPN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> getWalletByBpn(@PathVariable(name = "bpn") String bpn) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWalletByBpn(bpn));
    }

    /**
     * Gets wallets.
     *
     * @return the wallets
     */
    @GetMapping(path = RestURI.WALLETS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Wallet>> getWallets() {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWallets());
    }
}
