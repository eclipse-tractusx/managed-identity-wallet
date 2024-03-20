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

package org.eclipse.tractusx.managedidentitywallets.domain;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;

import java.net.URI;
import java.util.List;

@Builder
@Getter
public class PresentationCreationConfig {

    private VerifiableEncoding encoding;
    private long walletId; //FIXME for DB to retrieve privateKey from DB, and retrieving keyVaultKey to retrieve publicKey
    private List<VerifiableCredential> verifiableCredentials;
    private Did vpIssuerDid;

    // all for JWT
    private String audience;

    // all for JsonLD
    URI verificationMethod;

}