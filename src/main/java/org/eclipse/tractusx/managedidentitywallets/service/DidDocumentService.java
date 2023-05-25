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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.DidDocumentsNotFoundProblem;
import org.springframework.stereotype.Service;

/**
 * The type Did document service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DidDocumentService {

    private final WalletRepository walletRepository;

    /**
     * Gets did document.
     *
     * @param identifier the identifier
     * @return the did document
     */
    public String getDidDocument(String identifier) {
        Wallet wallet = identifier.startsWith("did:") ? walletRepository.getByDid(identifier) : walletRepository.getByBpn(identifier);
        if (wallet == null) {
            throw new DidDocumentsNotFoundProblem("DidDocument not found for identifier " + identifier);
        }
        return wallet.getDidDocument();
    }

}
