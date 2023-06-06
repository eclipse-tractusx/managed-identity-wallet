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

package org.eclipse.tractusx.managedidentitywallets.service;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.crypt.ed25519.Ed25519Key;
import org.springframework.stereotype.Service;

import java.io.StringReader;

/**
 * The type Wallet key service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WalletKeyService extends BaseService<WalletKey, Long> {

    private final WalletKeyRepository walletKeyRepository;

    private final SpecificationUtil<WalletKey> specificationUtil;

    private final EncryptionUtils encryptionUtils;

    @Override
    protected BaseRepository<WalletKey, Long> getRepository() {
        return walletKeyRepository;
    }

    @Override
    protected SpecificationUtil<WalletKey> getSpecificationUtil() {
        return specificationUtil;
    }


    /**
     * Get private key by wallet identifier as bytes byte [ ].
     *
     * @param walletId the wallet id
     * @return the byte [ ]
     */
    @SneakyThrows
    public byte[] getPrivateKeyByWalletIdentifierAsBytes(long walletId) {
        return getPrivateKeyByWalletIdentifier(walletId).getEncoded();
    }

    /**
     * Gets private key by wallet identifier.
     *
     * @param walletId the wallet id
     * @return the private key by wallet identifier
     */
    @SneakyThrows

    public Ed25519Key getPrivateKeyByWalletIdentifier(long walletId) {
        WalletKey wallet = walletKeyRepository.getByWalletId(walletId);
        String privateKey = encryptionUtils.decrypt(wallet.getPrivateKey());
        byte[] content = new PemReader(new StringReader(privateKey)).readPemObject().getContent();
        return Ed25519Key.asPrivateKey(content);
    }

}
