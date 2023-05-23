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
import org.bouncycastle.util.io.pem.PemReader;
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
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

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

    public List<Wallet> getWallets() {
        List<Wallet> wallets = walletRepository.findAll();
        return wallets;
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
        Did did = DidWebFactory.fromHostname(miwSettings.host()+":"+request.getBpn());
        DidDocument didDocument = new DidDocumentBuilder()
                .withDid(did)
                .withEd25519PublicKey(keyPair.getPublicKey())
                .build();
        log.debug("did document created for bpn ->{}", request.getBpn());

        //Save wallet
        Wallet wallet = walletRepository.save(Wallet.builder()
                .didDocument("did document")  //TODO remove once we have solution in lib  didDocument.toString or didDocument.toJson
                .bpn(request.getBpn())
                .did(did.toString())
                .active(true)
                .authority(true)
                .algorithm("ED25519")
                .build());

        //Save key
        walletKeyRepository.save(WalletKey.builder()
                .walletId(wallet.getId())
                .referenceKey("dummy ref key")  //TODO removed once vault setup is ready
                .vaultAccessToken("dummy vault access token") ////TODO removed once vault setup is ready
                .publicKey(encryptionUtils.encrypt(new String(keyPair.getPrivateKey())))
                .privateKey(encryptionUtils.encrypt(new String(keyPair.getPublicKey())))
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
    private Ed25519KeySet createKeyPair(){

        String keyLocation = "/tmp/"+ UUID.randomUUID();
        Files.createDirectories(Paths.get(keyLocation));

        File privateKey = new File(keyLocation+"/private.pem");
        File publicKey = new File(keyLocation+"/public.pem");
        try{
            //private key
            if(!privateKey.exists()){
                privateKey.createNewFile();
            }
            Process privateKeyProcess = Runtime.getRuntime().exec("openssl genpkey -algorithm ed25519");
           try( BufferedReader privateKeyReader = new BufferedReader(new InputStreamReader(privateKeyProcess.getInputStream()));
                BufferedWriter privateKeyWriter = new BufferedWriter(new FileWriter(privateKey))){
               FileCopyUtils.copy(privateKeyReader, privateKeyWriter);
           }

            //public key
            if(!publicKey.exists()){
                publicKey.createNewFile();
            }
            Process publicKeyProcess = Runtime.getRuntime().exec("openssl pkey -in "+privateKey.getAbsolutePath()+" -pubout");
            try(BufferedReader publicKeyReader = new BufferedReader(new InputStreamReader(publicKeyProcess.getInputStream()));
                BufferedWriter publicKeyWriter = new BufferedWriter(new FileWriter(publicKey))){
                FileCopyUtils.copy(publicKeyReader, publicKeyWriter);
            }
            return new Ed25519KeySet(readPEMFile(privateKey.getAbsolutePath()), readPEMFile(publicKey.getAbsolutePath()));
        }finally {
            FileSystemUtils.deleteRecursively(Paths.get(keyLocation));
        }
    }

    private byte[] readPEMFile(String path) throws IOException {
        PemReader pemReader = new PemReader(new FileReader(path));
        return pemReader.readPemObject().getContent();
    }
}
