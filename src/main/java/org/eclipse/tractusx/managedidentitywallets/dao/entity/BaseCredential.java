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

package org.eclipse.tractusx.managedidentitywallets.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.tractusx.managedidentitywallets.utils.StringToCredentialConverter;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseCredential extends MIWBaseEntity {

    @Column(nullable = false)
    private String holderDid;

    @Column(nullable = false)
    private String issuerDid;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    @Convert(converter = StringToCredentialConverter.class)
    private VerifiableCredential data;

    @Column(nullable = false)
    private String credentialId;
}
