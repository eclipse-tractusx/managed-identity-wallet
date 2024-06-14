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
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyStorageType;
import org.eclipse.tractusx.managedidentitywallets.service.WalletKeyService;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocalKeyProvider implements KeyProvider {

    private final WalletKeyService walletKeyService;

    private final WalletKeyRepository walletKeyRepository;

    private final EncryptionUtils encryptionUtils;

    @Override
    public Object getPrivateKey(String keyName, SupportedAlgorithms algorithm) {
        WalletKey walletKey = walletKeyRepository.getByAlgorithmAndWallet_Bpn(algorithm.name(), keyName);
        return walletKeyService.getPrivateKeyByKeyId(walletKey.getKeyId(), algorithm);
    }

    @Override
    public void saveKeys(List<WalletKey> walletKeys) {
        walletKeyRepository.saveAllAndFlush(walletKeys);
    }

    @Override
    public String getKeyId(String keyName, SupportedAlgorithms algorithm) {
        WalletKey walletKey = walletKeyRepository.getByAlgorithmAndWallet_Bpn(algorithm.name(), keyName);
        return walletKey.getKeyId();
    }

    @Override
    public KeyStorageType getKeyStorageType() {
        return KeyStorageType.DB;
    }

    @Override
    public KeyPair getKeyPair(DID self) {
        KeyPair dto = walletKeyRepository.findFirstByWallet_Did(self.toString()).toDto();

        return new KeyPair(
                dto.keyId(),
                encryptionUtils.decrypt(dto.privateKey()),
                encryptionUtils.decrypt(dto.publicKey())
        );
    }

    @Override
    public KeyPair getKeyPair(String bpn) {
        KeyPair dto = walletKeyRepository.findFirstByWallet_Bpn(bpn).toDto();

        return new KeyPair(
                dto.keyId(),
                encryptionUtils.decrypt(dto.privateKey()),
                encryptionUtils.decrypt(dto.publicKey())
        );
    }
}
