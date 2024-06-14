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

package org.eclipse.tractusx.managedidentitywallets.service;

import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.UnsupportedAlgorithmException;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519PrivateKey;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPrivateKeyFormatException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * The type Wallet key service.
 */
@Service
@RequiredArgsConstructor
public class WalletKeyService extends BaseService<WalletKey, Long> {

    public static final String EC = "EC";

    private final WalletKeyRepository walletKeyRepository;

    private final SpecificationUtil<WalletKey> specificationUtil;

    private final EncryptionUtils encryptionUtils;

    @Override
    public BaseRepository<WalletKey, Long> getRepository() {
        return walletKeyRepository;
    }

    @Override
    protected SpecificationUtil<WalletKey> getSpecificationUtil() {
        return specificationUtil;
    }

    /**
     * Get private key by wallet identifier as bytes byte [ ].
     *
     * @param walletId  the wallet id
     * @param algorithm the algorithm
     * @return the byte [ ]
     */
    @SneakyThrows
    public byte[] getPrivateKeyByWalletIdAsBytes(long walletId, String algorithm) {
        Object privateKey = getPrivateKeyByWalletIdAndAlgorithm(walletId, SupportedAlgorithms.valueOf(algorithm));
        if (privateKey instanceof X25519PrivateKey x25519PrivateKey) {
            return x25519PrivateKey.asByte();
        } else {
            return ((ECPrivateKey) privateKey).getEncoded();
        }
    }


    @SneakyThrows
    public Object getPrivateKeyByKeyId(String keyId, SupportedAlgorithms supportedAlgorithms) {
        WalletKey walletKey = walletKeyRepository.getByKeyIdAndAlgorithm(keyId, supportedAlgorithms.name());
        Object privateKey = getKeyObject(SupportedAlgorithms.valueOf(walletKey.getAlgorithm()), encryptionUtils.decrypt(walletKey.getPrivateKey()));
        if (privateKey instanceof X25519PrivateKey x25519PrivateKey) {
            return x25519PrivateKey;
        } else {
            return privateKey;
        }
    }

    /**
     * Gets private key by wallet identifier.
     *
     * @param walletId  the wallet id
     * @param algorithm the algorithm
     * @return the private key by wallet identifier
     */
    @SneakyThrows
    public Object getPrivateKeyByWalletIdAndAlgorithm(long walletId, SupportedAlgorithms algorithm) {
        WalletKey wallet = walletKeyRepository.getByWalletIdAndAlgorithm(walletId, algorithm.toString());
        String privateKey = encryptionUtils.decrypt(wallet.getPrivateKey());
        return getKeyObject(algorithm, privateKey);
    }

    private static Object getKeyObject(SupportedAlgorithms algorithm, String privateKey) throws IOException, InvalidPrivateKeyFormatException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] content = new PemReader(new StringReader(privateKey)).readPemObject().getContent();
        if (SupportedAlgorithms.ED25519.equals(algorithm)) {
            return new X25519PrivateKey(content);
        } else if (SupportedAlgorithms.ES256K.equals(algorithm)) {
            KeyFactory kf = KeyFactory.getInstance(EC);
            return kf.generatePrivate(new PKCS8EncodedKeySpec(content));
        } else {
            throw new UnsupportedAlgorithmException("Unsupported algorithm: " + algorithm);
        }
    }

    /**
     * Gets wallet key by wallet id.
     *
     * @param walletId            the wallet id
     * @param supportedAlgorithms the algorithm  of private key
     * @return the wallet key by wallet identifier
     */
    @SneakyThrows
    public String getWalletKeyIdByWalletId(long walletId, SupportedAlgorithms supportedAlgorithms) {
        return walletKeyRepository.getByWalletIdAndAlgorithm(walletId, supportedAlgorithms.name()).getKeyId();
    }

}
