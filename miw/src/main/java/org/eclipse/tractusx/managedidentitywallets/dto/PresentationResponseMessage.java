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
 * As `presentationSubmission` a not well-defined,  so we will just skip them. Defining all types as 'Json' make the whole idea of using Json-Linked-Data a waste of time, but ok.
 * <p>
 * The `presentation` property is also specified as 'Json'. As we are now allowed to skip them we will assume these are Presentations from ether the <a href="https://www.w3.org/2018/credentials/v1">Verifiable Credential Data Model v1.1</a> or <a href="https://www.w3.org/ns/credentials/v2">Verifiable Credential Data Model v2.0</a>. ( It would be so convenient, if there would be a way do differentiate these different types of data! )
 */
@Getter
public class PresentationResponseMessage {


    public PresentationResponseMessage(VerifiablePresentation verifiablePresentation) {
        this(List.of(verifiablePresentation));
    }

    public PresentationResponseMessage(List<VerifiablePresentation> verifiablePresentations) {
        this.verifiablePresentations = verifiablePresentations;
    }

    @JsonProperty("@context")
    private List<String> contexts = List.of("https://w3id.org/tractusx-trust/v0.8");

    @JsonProperty("@type")
    private List<String> types = List.of("PresentationResponseMessage");

    @JsonProperty("presentation")
    private List<VerifiablePresentation> verifiablePresentations;
}
