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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.DidDocumentService;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Did document controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "DIDDocument")
public class DidDocumentController {
    private final DidDocumentService service;

    /**
     * Gets did document.
     *
     * @param identifier the identifier
     * @return the did document
     */
    @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {@Content(examples = {
            @ExampleObject(name = "Internal server error", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 500,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "404", description = "Wallet not found with provided bpn", content = {@Content(examples = {
            @ExampleObject(name = "Wallet not found with provided bpn", value = """
                    {
                      "type": "about:blank",
                      "title": "Wallet not found for identifier BPNL00000000000",
                      "status": 404,
                      "detail": "Wallet not found for identifier BPNL00000000000",
                      "instance": "/BPNL00000000000/did.json",
                      "properties": {
                        "timestamp": 1689767698010
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "200", description = "DID document", content = {
            @Content(examples = {
                    @ExampleObject(name = " DID document", value = """
                             {
                                "@context": [
                                 "https://www.w3.org/ns/did/v1",
                                 "https://w3c.github.io/vc-jws-2020/contexts/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000",
                               "verificationMethod": [
                                 {
                                   "controller": "did:web:localhost:BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "publicKeyJwk": {
                                     "crv": "Ed25519",
                                     "kty": "OKP",
                                     "x": "wAOQvr92L1m7RwrpeOrgWByVYvWmhRr4fJbiMwHEIdY"
                                   },
                                   "type": "JsonWebKey2020"
                                 }
                               ]
                             }
                            """)
            })
    })
    @Operation(description = "Resolve the DID document for a given DID or BPN", summary = "Resolve DID Document")
    @GetMapping(path = RestURI.DID_DOCUMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DidDocument> getDidDocument(@Parameter(description = "Did or BPN",examples = {@ExampleObject(name = "bpn", value = "BPNL000000000000", description = "bpn"), @ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000000")}) @PathVariable(name = "identifier") String identifier) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getDidDocument(identifier));
    }

    /**
     * Gets did resolve.
     *
     * @param bpn the bpn
     * @return the did resolve
     */
    @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {@Content(examples = {
            @ExampleObject(name = "Internal server error", value = """
                    {
                      "type": "about:blank",
                      "title": "Error Title",
                      "status": 500,
                      "detail": "Error Details",
                      "instance": "API endpoint",
                      "properties": {
                        "timestamp": 1689762476720
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "404", description = "Wallet not found with provided bpn", content = {@Content(examples = {
            @ExampleObject(name = "Wallet not found with provided bpn", value = """
                    {
                      "type": "about:blank",
                      "title": "Wallet not found for identifier BPNL00000000000",
                      "status": 404,
                      "detail": "Wallet not found for identifier BPNL00000000000",
                      "instance": "/BPNL00000000000/did.json",
                      "properties": {
                        "timestamp": 1689767698010
                      }
                    }
                    """)
    })})
    @ApiResponse(responseCode = "200", description = "DID document", content = {
            @Content(examples = {
                    @ExampleObject(name = " DID document", value = """
                             {
                               "@context": [
                                 "https://www.w3.org/ns/did/v1",
                                 "https://w3c.github.io/vc-jws-2020/contexts/v1"
                               ],
                               "id": "did:web:localhost:BPNL000000000000",
                               "verificationMethod": [
                                 {
                                   "controller": "did:web:localhost:BPNL000000000000",
                                   "id": "did:web:localhost:BPNL000000000000",
                                   "publicKeyJwk": {
                                     "crv": "Ed25519",
                                     "kty": "OKP",
                                     "x": "wAOQvr92L1m7RwrpeOrgWByVYvWmhRr4fJbiMwHEIdY"
                                   },
                                   "type": "JsonWebKey2020"
                                 }
                               ]
                             }
                            """)
            })
    })
    @Operation(description = "Resolve the DID document for a given BPN", summary = "Resolve DID Document")
    @GetMapping(path = RestURI.DID_RESOLVE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DidDocument> getDidResolve(@Parameter(description = "BPN",examples = {@ExampleObject(name = "bpn", value = "BPNL000000000000", description = "bpn")}) @PathVariable(name = "bpn") String bpn) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getDidDocument(bpn));
    }
}
