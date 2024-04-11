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

package org.eclipse.tractusx.managedidentitywallets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


/**
 * The type Issue framework credential request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueFrameworkCredentialRequest {

    @NotBlank(message = "Please provide holder identifier")
    @Size(min = 5, max = 255, message = "Please provide valid identifier")
    private String holderIdentifier;

    @NotBlank(message = "Please provide type")
    private String type;

    @NotBlank(message = "Please provide contract-template")
    @JsonProperty("contract-template")
    private String contractTemplate;

    @NotBlank(message = "Please provide contract-template")
    @JsonProperty("contract-version")
    private String contractVersion;
}
