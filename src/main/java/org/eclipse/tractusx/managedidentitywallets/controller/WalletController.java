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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Wallets")
public class WalletController extends BaseController {

    private final WalletService service;

    /**
     * Create wallet response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = {

                    @ExampleObject(name = "Create wallet with BPN", value = """
                                                        {
                                                          "bpn": "BPNL000000000001",
                                                          "name": "companyA"
                                                        }
                            """)
            })
    })
    @Operation(summary = "Create Wallet", description = "Permission: **add_wallets** \n\n Create a wallet and store it")
    @PostMapping(path = RestURI.WALLETS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWallet(request));
    }

    /**
     * Store credential response entity.
     *
     * @param data       the data
     * @param identifier the identifier
     * @return the response entity
     */
    @Operation(summary = "Store Verifiable Credential", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of wallet to extract credentials from must equal BPN of caller) \n\n Store a verifiable credential in the wallet of the given identifier")
    @PostMapping(path = RestURI.API_WALLETS_IDENTIFIER_CREDENTIALS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                 {
                                   "id": "http://example.edu/credentials/3732",
                                   "@context": [
                                     "https://www.w3.org/2018/credentials/v1",
                                     "https://www.w3.org/2018/credentials/examples/v1"
                                   ],
                                   "type": [
                                     "University-Degree-Credential", "VerifiableCredential"
                                   ],
                                   "issuer": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                   "issuanceDate": "2019-06-16T18:56:59Z",
                                   "expirationDate": "2019-06-17T18:56:59Z",
                                   "credentialSubject": [{
                                     "college": "Test-University"
                                   }],
                                   "proof": {
                                     "type": "Ed25519Signature2018",
                                     "created": "2021-11-17T22:20:27Z",
                                     "proofPurpose": "assertionMethod",
                                     "verificationMethod": "did:example:76e12ec712ebc6f1c221ebfeb1f#key-1",
                                     "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                                   }
                                 }
                    """))
    })
    public ResponseEntity<Map<String, String>> storeCredential(@RequestBody Map<String, Object> data,
                                                               @Parameter(description = "Did or BPN") @PathVariable(name = "identifier") String identifier, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.storeCredential(data, identifier, getBPNFromToken(principal)));
    }

    /**
     * Gets wallet by bpn.
     *
     * @param identifier      the identifier
     * @param withCredentials the with credentials
     * @return the wallet by bpn
     */
    @Operation(summary = "Retrieve wallet by identifier", description = "Permission: **view_wallets** OR **view_wallet** (The BPN of Wallet to retrieve must equal the BPN of caller or Base wallet, authority wallet can see all wallets) \n\n Retrieve single wallet by identifier, with or without its credentials")
    @GetMapping(path = RestURI.API_WALLETS_IDENTIFIER, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Wallet> getWalletByIdentifier(@Parameter(description = "Did or BPN") @PathVariable(name = "identifier") String identifier,
                                                        @RequestParam(name = "withCredentials", defaultValue = "false") boolean withCredentials,
                                                        Principal principal) {

        return ResponseEntity.status(HttpStatus.OK).body(service.getWalletByIdentifier(identifier, withCredentials, getBPNFromToken(principal)));
    }

    /**
     * Gets wallets.
     *
     * @return the wallets
     */
    @Operation(summary = "List of wallets", description = "Permission: **view_wallets** \n\n Retrieve list of registered wallets")
    @GetMapping(path = RestURI.WALLETS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Wallet>> getWallets(@RequestParam(required = false, defaultValue = "0") int pageNumber,
                                                   @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") int size,
                                                   @RequestParam(required = false, defaultValue = "createdAt") String sortColumn,
                                                   @RequestParam(required = false, defaultValue = "desc") String sortTpe) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getWallets(pageNumber, size, sortColumn, sortTpe));
    }
}