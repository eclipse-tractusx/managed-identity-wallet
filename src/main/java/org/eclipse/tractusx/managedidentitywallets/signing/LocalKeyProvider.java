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

import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyStorageType;
import org.eclipse.tractusx.managedidentitywallets.service.WalletKeyService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalKeyProvider implements KeyProvider {

    private final WalletKeyService walletKeyService;

    private final WalletKeyRepository walletKeyRepository;

    private final WalletRepository walletRepository;

    @Override
    public byte[] getPrivateKey(String keyName) { //
        Wallet wallet = walletRepository.getByBpn(keyName);
        return walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(wallet.getId());
    }

    @Override
    public void saveKeys(WalletKey walletKey) {
        walletKeyRepository.save(walletKey);
    }

    @Override
    public KeyStorageType getKeyStorageType() {
        return KeyStorageType.DB;
    }
}
