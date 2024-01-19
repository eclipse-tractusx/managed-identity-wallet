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

package org.eclipse.tractusx.managedidentitywallets.service.impl;

import java.util.Optional;
import java.util.Set;

import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.SecureTokenIssuer;
import org.eclipse.tractusx.managedidentitywallets.domain.Wallet;
import org.eclipse.tractusx.managedidentitywallets.domain.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenService;

import com.nimbusds.jwt.JWT;

public class SecureTokenServiceImpl implements SecureTokenService {

    private final WalletRepository repo;
    private final SecureTokenIssuer tokenIssuer;

    public SecureTokenServiceImpl(final WalletRepository repo, final SecureTokenIssuer tokenIssuer) {
        this.repo = repo;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public JWT issueToken(final DID self, final DID partner, Set<String> scopes) {
        Optional<Wallet> wallet = repo.findWallet(self);
        KeyPair keyPair = wallet.map(w -> w.getKeys())
                .flatMap(k -> k.stream().findFirst())
                .orElseThrow();

        return this.tokenIssuer.issueIdToken(self, partner, keyPair, scopes);
    }

    @Override
    public JWT issueToken(DID self, DID partner, JWT accessToken) {
        Optional<Wallet> wallet = repo.findWallet(self);
        KeyPair keyPair = wallet.map(w -> w.getKeys())
                .flatMap(k -> k.stream().findFirst())
                .orElseThrow();

        return this.tokenIssuer.issueIdToken(self, partner, keyPair, accessToken);
    }

}
