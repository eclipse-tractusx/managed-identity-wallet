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

package org.eclipse.tractusx.managedidentitywallets.utils;

import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

/**
 * The type Encryption utils.
 */
@Component
public class EncryptionUtils {

    private final Cipher cipher;
    private final Key aesKey;

    /**
     * Instantiates a new Encryption utils.
     *
     * @param miwSettings the miw settings
     */
    @SneakyThrows
    public EncryptionUtils(MIWSettings miwSettings) {
        aesKey = new SecretKeySpec(miwSettings.encryptionKey().getBytes(), "AES");
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

    }

    /**
     * Encrypt string.
     *
     * @param text the text
     * @return the string
     */
    @SneakyThrows
    public String encrypt(String text){
        byte[] encrypted = cipher.doFinal(text.getBytes());
        return  Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypt string.
     *
     * @param text the text
     * @return the string
     */
    @SneakyThrows
    public String decrypt(String text){
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(text)));
    }
}