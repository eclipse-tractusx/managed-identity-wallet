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

package org.eclipse.tractusx.managedidentitywallets.revocation.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatusListIndex {
    @Id
    @Column(name = "id", length = 256) // Adjust length as per your schema constraints
    @NotBlank(message = "ID cannot be blank")
    @Size(max = 256, message = "ID cannot exceed 256 characters")
    private String id;

    /**
     * issuerBpn is a string field that represents the issuer' BPN with the status Purpose at the end
     *
     * <p>Example: "issuerBpn-revocation"
     */
    @Column(name = "issuer_bpn_status", length = 27) // Adjust length as per your schema constraints
    @NotBlank(message = "Issuer BPN with status cannot be blank")
    @Size(max = 27, message = "Issuer Bpn with status cannot exceed 27 characters")
    private String issuerBpnStatus;

    @Column(name = "current_index", length = 16) // Adjust length as per your schema constraints
    @NotBlank(message = "Current index cannot be blank")
    @Pattern(regexp = "^[0-9]+$", message = "Current index must be numeric")
    @Size(max = 16, message = "Current index cannot exceed 16 characters")
    private String currentIndex;

    // Using LAZY fetching strategy to fetch statusListCredential on-demand
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "status_list_credential_id", referencedColumnName = "id")
    private StatusListCredential statusListCredential;

    public void setCurrentIndex(String index) {
        if (index != null && !index.trim().isEmpty() && index.matches("^[0-9]+$")) {
            this.currentIndex = index;
        } else {
            throw new IllegalArgumentException("Invalid index value");
        }
    }
}
