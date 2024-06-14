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

package org.eclipse.tractusx.managedidentitywallets.signing;

import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyStorageType;

import java.util.List;

/**
 * This class will be used by LocalSigningService to retrieve and save keys
 *
 * @see LocalSigningService
 */
public interface KeyProvider {

    /**
     * @param keyName the name of the key that is to be retrieved
     * @return the key as a byte-array
     */
    Object getPrivateKey(String keyName, SupportedAlgorithms algorithm);

    /**
     * @param walletKey the key to save
     */
    void saveKeys(List<WalletKey> walletKey);


    String getKeyId(String keyName, SupportedAlgorithms algorithm);


    /**
     * @return the type of KeyProvider
     * @see KeyStorageType
     */
    KeyStorageType getKeyStorageType();

    KeyPair getKeyPair(DID self);

    KeyPair getKeyPair(String bpn);
}
