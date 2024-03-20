/*
 * *******************************************************************************
 *  Copyright (c) 2021;2024 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License; Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing; software
 *  distributed under the License is distributed on an "AS IS" BASIS; WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND; either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

package org.eclipse.tractusx.managedidentitywallets.domain;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatus;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Builder
@Getter
public class HoldersCredentialCreationConfig {
    private VerifiableCredentialSubject subject;
    private VerifiableCredentialStatus verifiableCredentialStatus;
    private DidDocument issuerDoc;
    private String holderDid;
    private List<String> types;
    private List<URI> contexts;
    private URI vcId;
    private Date expiryDate;
    private boolean selfIssued;
    private VerifiableEncoding encoding;

    // this will be used by the DB-Impl of storage to retrieve privateKey
    private long walletId;

    public static class HoldersCredentialCreationConfigBuilder {
        public HoldersCredentialCreationConfigBuilder vcId(Object object) {
            if (!(object instanceof URI) && !(object instanceof String)) {
                throw new IllegalArgumentException("vcId must be of type String or URI, argument has type%s".formatted(object.getClass().getName()));
            }

            if (object instanceof URI uri) {
                this.vcId = uri;
            } else {
                this.vcId = URI.create((String) object);
            }

            return this;

        }

        public HoldersCredentialCreationConfig build() {

            try {
                Objects.requireNonNull(encoding);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("encoding type must not be null");
            }


            try {
                Objects.requireNonNull(subject);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("subject must not be null");
            }

            try {
                Objects.requireNonNull(types);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("types must not be null");
            }


            try {
                Objects.requireNonNull(issuerDoc);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("issuer did document must not be null");
            }

            try {
                Objects.requireNonNull(holderDid);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("holder did must not be null");
            }

            try {
                Objects.requireNonNull(contexts);
            } catch (NullPointerException e) {
                throw new IllegalArgumentException("contexts must not be null");
            }

            return new HoldersCredentialCreationConfig(subject, verifiableCredentialStatus, issuerDoc, holderDid, types, contexts, vcId, expiryDate, selfIssued, encoding, walletId);
        }
    }
}
