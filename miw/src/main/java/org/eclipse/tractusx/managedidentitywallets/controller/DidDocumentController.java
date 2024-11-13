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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.apidocs.DidDocumentControllerApiDocs.BpnParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.DidDocumentControllerApiDocs.DidOrBpnParameterDoc;
import org.eclipse.tractusx.managedidentitywallets.apidocs.DidDocumentControllerApiDocs.GetDidDocumentApiDocs;
import org.eclipse.tractusx.managedidentitywallets.apidocs.DidDocumentControllerApiDocs.GetDidResolveApiDocs;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.service.DidDocumentService;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The type Did document controller.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "DIDDocument")
@Slf4j
public class DidDocumentController {
    private final DidDocumentService service;

    /**
     * Gets did document.
     *
     * @param identifier the identifier
     * @return the did document
     */
    @GetDidDocumentApiDocs
    @GetMapping(path = RestURI.DID_DOCUMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DidDocument> getDidDocument(
        @DidOrBpnParameterDoc @PathVariable(name = "identifier") String identifier
        ) {
        log.debug("Received request to get DID document for identifier: {}", identifier);
        return ResponseEntity.status(HttpStatus.OK).body(service.getDidDocument(identifier));
    }

    /**
     * Gets did resolve.
     *
     * @param bpn the bpn
     * @return the did resolve
     */
    @GetDidResolveApiDocs
    @GetMapping(path = RestURI.DID_RESOLVE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DidDocument> getDidResolve(
        @BpnParameterDoc @PathVariable(name = "bpn") String bpn
        ) {
        log.debug("Received request to get DID document for identifier: {}", bpn);
        return ResponseEntity.status(HttpStatus.OK).body(service.getDidDocument(bpn));
    }
}
