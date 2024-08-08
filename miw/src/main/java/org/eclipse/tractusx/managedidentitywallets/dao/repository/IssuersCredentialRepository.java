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

package org.eclipse.tractusx.managedidentitywallets.dao.repository;

import com.smartsensesolutions.commons.dao.base.BaseRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.IssuersCredential;

import java.util.List;

/**
 * The interface Credential repository.
 */
public interface IssuersCredentialRepository extends BaseRepository<IssuersCredential, Long> {


    /**
     * Gets by issuer did and holder did and type.
     *
     * @param issuerDid the issuer did
     * @param holderDid the holder did
     * @param type      the type
     * @return the by issuer did and holder did and type
     */
    List<IssuersCredential> getByIssuerDidAndHolderDidAndType(String issuerDid, String holderDid, String type);
}
