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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.revocation.validation.ValidVerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatusListCredential {
    @Id
    @Column(name = "id", length = 256) // Adjust length as per your schema constraints
    @NotBlank(message = "ID cannot be blank")
    @Size(max = 256, message = "ID cannot exceed 256 characters")
    private String id;

    /**
     * issuerBpn is a string field that represents the issuer' BPN with the status Purpose at the end
     *
     * <p>Example: "BPNL123456789123"
     */
    @Column(name = "issuer_bpn", length = 16) // Adjust length as per your schema constraints
    @NotBlank(message = "Issuer BPN cannot be blank")
    @Size(max = 16, message = "Issuer Bpn cannot exceed 16 characters")
    private String issuerBpn;

    // Annotation @Lob indicates that the field should be persisted as a large object to the database.
    @Lob
    @ValidVerifiableCredential // Custom validation annotation
    @NotNull(message = "Credential cannot be null")
    @Column(name = "credential", columnDefinition = "TEXT")
    @Convert(converter = StringToCredentialConverter.class)
    private VerifiableCredential credential;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private LocalDateTime updatedAt;

    // FetchType.LAZY should be used for large objects as Lob
    @OneToOne(mappedBy = "statusListCredential", fetch = FetchType.LAZY)
    private StatusListIndex index;

    // Ensure proper validation inside the setter if additional rules are needed
    public void setCredential(VerifiableCredential vc) {
        if (vc != null) {
            credential = vc;
        } else {
            throw new IllegalArgumentException("Credential cannot be null");
        }
    }
}
