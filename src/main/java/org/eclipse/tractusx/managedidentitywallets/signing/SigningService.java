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

import org.eclipse.tractusx.managedidentitywallets.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.PresentationCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.domain.SigningServiceType;
import org.eclipse.tractusx.ssi.lib.crypt.KeyPair;
import org.eclipse.tractusx.ssi.lib.exception.KeyGenerationException;

/**
 * Service used to sign the verifiable credentials and verifiable presentations
 */
public interface SigningService {
    /**
     * @param config the configuration for creating the credential
     * @return SignerResult containing the signed credential
     *
     * @see CredentialCreationConfig
     * @see SignerResult
     */
    SignerResult createCredential(CredentialCreationConfig config);

    /**
     * @param config the config for creating/retrieving the key
     * @return KeyPair containing the public and private key (private key depends on the type of signing service used)
     * @throws KeyGenerationException when something goes wrong
     *
     * @see KeyPair
     * @see KeyCreationConfig
     */
    KeyPair getKey(KeyCreationConfig config) throws KeyGenerationException;

    /**
     * @param key the key to be saved, LocalSigningService implementations may call KeyProvider.saveKey
     *
     * @see KeyProvider
     */
    void saveKey(WalletKey key);

    /**
     * @return the implementation's supported type
     */
    SigningServiceType getSupportedServiceType();

    /**
     * @param config the configuration for creating the presentation
     * @return SignerResult containing the signed presentation
     *
     * @see PresentationCreationConfig
     * @see SignerResult
     */
    SignerResult createPresentation(PresentationCreationConfig config);
}
