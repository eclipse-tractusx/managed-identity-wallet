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
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * The interface Credential repository.
 */
public interface HoldersCredentialRepository extends BaseRepository<HoldersCredential, Long> {
    /**
     * Gets by holder did.
     *
     * @param holderDid the holder did
     * @return the by holder did
     */
    List<HoldersCredential> getByHolderDid(String holderDid);

    /**
     * Gets credentials by holder.
     *
     * @param holderDid the holder did
     * @return the credentials by holder
     */
    @Query("select data from HoldersCredential where holderDid=:holderDid")
    List<VerifiableCredential> getCredentialsByHolder(@Param("holderDid") String holderDid);

    /**
     * Gets by holder did and type.
     *
     * @param holderDid the holder did
     * @param type      the type
     * @return the by holder did and type
     */
    List<HoldersCredential> getByHolderDidAndType(String holderDid, String type);
}
