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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;


/**
 * The type Create wallet request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequest {

    @NotBlank(message = "Please provide BPN")
    @Pattern(regexp = StringPool.BPN_NUMBER_REGEX, message = "Please provide valid BPN")
    private String businessPartnerNumber;

    @NotBlank(message = "Please provide name")
    @Size(min = 1, max = 255, message = "Please provide valid name")
    private String companyName;

    @NotBlank(message = "Please provide url")
    @Size(min = 1, max = 2000, message = "Please provide url")
    private String didUrl;
}
