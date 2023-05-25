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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.dto.CreateWalletRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.DuplicateWalletProblem;
import org.eclipse.tractusx.managedidentitywallets.exception.WalletNotFoundProblem;
import org.eclipse.tractusx.managedidentitywallets.utils.EncryptionUtils;
import org.eclipse.tractusx.ssi.agent.lib.DidDocumentBuilder;
import org.eclipse.tractusx.ssi.lib.crypt.ed25519.Ed25519KeySet;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebFactory;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.List;

/**
 * The type Wallet service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;

    private final MIWSettings miwSettings;

    private final EncryptionUtils encryptionUtils;

    private final WalletKeyRepository walletKeyRepository;


    /**
     * Gets wallet by bpn.
     *
     * @param bpn the bpn
     * @return the wallet by bpn
     */
    public Wallet getWalletByBpn(String bpn) {
        Wallet wallet = walletRepository.getByBpn(bpn);
        if (wallet == null) {
            throw new WalletNotFoundProblem("Wallet not found for bpn " + bpn);
        }
        return wallet;
    }

    /**
     * Gets wallets.
     *
     * @return the wallets
     */
    public List<Wallet> getWallets() {
        return walletRepository.findAll();
    }

    /**
     * Create wallet.
     *
     * @param request the request
     * @return the wallet
     */
    @SneakyThrows
    public Wallet createWallet(CreateWalletRequest request) {
        validateCreateWallet(request);

        //create private key pair
        Ed25519KeySet keyPair = createKeyPair();

        //create did json
        Did did = DidWebFactory.fromHostname(URLDecoder.decode(miwSettings.host() + ":" + request.getBpn(), Charset.defaultCharset()));
        DidDocument didDocument = new DidDocumentBuilder()
                .withDid(did)
                .withEd25519PublicKey(keyPair.getPublicKey())
                .build();
        log.debug("did document created for bpn ->{}", request.getBpn());

        //Save wallet
        Wallet wallet = walletRepository.save(Wallet.builder()
                .didDocument("did document")  //TODO remove once we have solution in lib  didDocument.toString or didDocument.toJson
                .bpn(request.getBpn())
                .name(request.getName())
                .did(did.toString())
                .algorithm("ED25519")
                .build());

        //Save key
        walletKeyRepository.save(WalletKey.builder()
                .walletId(wallet.getId())
                .referenceKey("dummy ref key")  //TODO removed once vault setup is ready
                .vaultAccessToken("dummy vault access token") ////TODO removed once vault setup is ready
                .privateKey(encryptionUtils.encrypt(getPrivateKeyString(keyPair.getPrivateKey())))
                .publicKey(encryptionUtils.encrypt(getPublicKeyString(keyPair.getPublicKey())))
                .build());
        log.debug("Wallet created for bpn ->{}", request.getBpn());
       return  wallet;
    }

    private void validateCreateWallet(CreateWalletRequest request){
        boolean exist = walletRepository.existsByBpn(request.getBpn());
        if(exist){
            throw  new DuplicateWalletProblem("Wallet is already exists for bpn "+request.getBpn());
        }

    }

    @SneakyThrows
    private Ed25519KeySet createKeyPair() {
        SecureRandom secureRandom = new SecureRandom();

        Ed25519KeyPairGenerator keyPairGenerator = new Ed25519KeyPairGenerator();
        keyPairGenerator.init(new Ed25519KeyGenerationParameters(secureRandom));

        AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();
        Ed25519PrivateKeyParameters privateKey = (Ed25519PrivateKeyParameters) keyPair.getPrivate();
        Ed25519PublicKeyParameters publicKey = (Ed25519PublicKeyParameters) keyPair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();
        byte[] publicKeyBytes = publicKey.getEncoded();
        return new Ed25519KeySet(privateKeyBytes, publicKeyBytes);
    }


    @SneakyThrows
    private String getPrivateKeyString(byte[] privateKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(PrivateKeyInfoFactory.createPrivateKeyInfo(new Ed25519PrivateKeyParameters(privateKeyBytes, 0)));
        pemWriter.close();
        return stringWriter.toString();
    }

    @SneakyThrows
    private String getPublicKeyString(byte[] publicKeyBytes) {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter);
        pemWriter.writeObject(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(new Ed25519PublicKeyParameters(publicKeyBytes, 0)));
        pemWriter.close();
        return stringWriter.toString();
    }
}
