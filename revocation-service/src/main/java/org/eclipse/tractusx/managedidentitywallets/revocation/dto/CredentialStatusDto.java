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

package org.eclipse.tractusx.managedidentitywallets.revocation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.tractusx.managedidentitywallets.revocation.constant.PurposeType;
import org.eclipse.tractusx.managedidentitywallets.revocation.utils.BitSetManager;

public record CredentialStatusDto(
        @NotBlank @NotNull @JsonProperty("id") String id,
        @NotBlank @NotNull @JsonProperty("statusPurpose") String statusPurpose,
        @NotBlank @NotNull @JsonProperty("statusListIndex") String statusListIndex,
        @NotBlank @NotNull @JsonProperty("statusListCredential") String statusListCredential,
        @NotBlank @NotNull @JsonProperty("type") String type) {
    public CredentialStatusDto {
        if (Integer.parseInt(statusListIndex) < 0
                || Integer.parseInt(statusListIndex) > BitSetManager.BITSET_SIZE - 1) {
            throw new IllegalArgumentException("statusListIndex is out of range");
        }
        if (!statusPurpose.equalsIgnoreCase(PurposeType.REVOCATION.toString())) {
            throw new IllegalArgumentException("invalid statusPurpose");
        }
        if (!type.equals(StatusListCredentialSubject.TYPE_ENTRY)) {
            throw new IllegalArgumentException("invalid type");
        }
    }
}
