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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "VerifiablePresentations")
public class PresentationController {

    private final PresentationService presentationService;

    @Operation(summary = "Create Verifiable Presentation", description = "Permission: **update_wallets** OR **update_wallet** (The BPN of the issuer of the Verifiable Presentation must equal to BPN of caller) \n\n Create a verifiable presentation from a list of verifiable credentials, signed by the holder")
    @PostMapping(path = RestURI.API_PRESENTATIONS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = {
            @Content(examples = @ExampleObject("""
                                {
                                  "holderIdentifier": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                  "verifiableCredentials": [
                                    {
                                      "id": "http://example.edu/credentials/333",
                                      "@context": [
                                        "https://www.w3.org/2018/credentials/v1",
                                        "https://www.w3.org/2018/credentials/examples/v1"
                                      ],
                                      "type": [
                                        "University-Degree-Credential, VerifiableCredential"
                                      ],
                                      "issuer": "did:example:76e12ec712ebc6f1c221ebfeb1f",
                                      "issuanceDate": "2019-06-16T18:56:59Z",
                                      "expirationDate": "2019-06-17T18:56:59Z",
                                      "credentialSubject": {
                                        "college": "Test-University"
                                      },
                                      "proof": {
                                        "type": "Ed25519Signature2018",
                                        "created": "2021-11-17T22:20:27Z",
                                        "proofPurpose": "assertionMethod",
                                        "verificationMethod": "did:example:76e12ec712ebc6f1c221ebfeb1f#keys-1",
                                        "jws": "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..JNerzfrK46Mq4XxYZEnY9xOK80xsEaWCLAHuZsFie1-NTJD17wWWENn_DAlA_OwxGF5dhxUJ05P6Dm8lcmF5Cg"
                                      }
                                    }
                                  ]
                                }
                    """))
    })
    public ResponseEntity<Map<String, Object>> createPresentation(@RequestBody Map<String, Object> data,
                                                                  @RequestParam(name = "asJwt", required = false, defaultValue = "false") boolean asJwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presentationService.createPresentation(data, asJwt, "smartSense"));
    }
}
