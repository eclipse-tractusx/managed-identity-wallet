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
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.springframework.stereotype.Repository;

/**
 * The interface Wallet repository.
 */
@Repository
public interface WalletRepository extends BaseRepository<Wallet, Long> {

    /**
     * Gets by bpn.
     *
     * @param bpn the bpn
     * @return the by bpn
     */
    Wallet getByBpn(String bpn);

    /**
     * Exists by bpn boolean.
     *
     * @param bpn the bpn
     * @return the boolean
     */
    boolean existsByBpn(String bpn);

    /**
     * Gets by did.
     *
     * @param did the did
     * @return the by did
     */
    Wallet getByDid(String did);

    int countByBpn(String bpn);

}
