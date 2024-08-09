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

package org.eclipse.tractusx.managedidentitywallets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;

import java.util.List;

/**
 * Class to represent the response message of a presentation request.
 * Defined in JsonLD Tractus-X  <a href="https://github.com/eclipse-tractusx/identity-trust/blob/main/specifications/context.json">context.json</a>.
 * <p>
 * As `presentationSubmission` a not well-defined, we will just skip the property for HTTP responses. Defining all types as 'Json' make the whole idea of using Json-Linked-Data a waste of time, but ok.
 * <p>
 * The `presentation` property is only specified as 'Json'. For this implementation we will assume these are Presentations from ether the <a href="https://www.w3.org/2018/credentials/v1">Verifiable Credential Data Model v1.1</a> or <a href="https://www.w3.org/ns/credentials/v2">Verifiable Credential Data Model v2.0</a>.
 * <br/>
 * At the same time other applications require the Verifiable Presentation to be a Json Web Token. As this protocol is not able to define a good data type, and implementations of this protocol even require different types, object is the correct data type here...
 */
@Getter
public class PresentationResponseMessage {


    public PresentationResponseMessage(Object verifiablePresentation) {
        this(List.of(verifiablePresentation));
    }

    public PresentationResponseMessage(List<Object> verifiablePresentations) {
        this.verifiablePresentations = verifiablePresentations;
    }

    @JsonProperty("@context")
    private List<Object> contexts = List.of("https://w3id.org/tractusx-trust/v0.8");

    @JsonProperty("@type")
    private List<Object> types = List.of("PresentationResponseMessage");

    @JsonProperty("presentation")
    private List<Object> verifiablePresentations;
}
