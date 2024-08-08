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

package org.eclipse.tractusx.managedidentitywallets.dao.repository;

import com.smartsensesolutions.commons.dao.base.BaseRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.springframework.stereotype.Repository;

/**
 * The interface Wallet key repository.
 */
@Repository
public interface WalletKeyRepository extends BaseRepository<WalletKey, Long> {
    /**
     * Gets by wallet id and algorithm.
     *
     * @param id        the wallet id
     * @param algorithm the algorithm
     * @return the by wallet id
     */
    WalletKey getByWalletIdAndAlgorithm(Long id, String algorithm);

    /**
     * Find first by wallet bpn wallet key.
     *
     * @param bpn the bpn
     * @return the wallet key
     */
    WalletKey findFirstByWallet_Bpn(String bpn);

    /**
     * Find first by wallet did wallet key.
     *
     * @param did the did
     * @return the wallet key
     */
    WalletKey findFirstByWallet_Did(String did);

    /**
     * Gets by key id and algorithm.
     *
     * @param keyId     the key id
     * @param algorithm the algorithm
     * @return the by key id and algorithm
     */
    WalletKey getByKeyIdAndAlgorithm(String keyId, String algorithm);

    /**
     * Gets by algorithm and wallet bpn.
     *
     * @param name    the name
     * @param keyName the key name
     * @return the by algorithm and wallet bpn
     */
    WalletKey getByAlgorithmAndWallet_Bpn(String name, String keyName);
}
