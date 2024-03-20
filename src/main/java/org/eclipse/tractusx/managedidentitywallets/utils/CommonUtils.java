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

import lombok.experimental.UtilityClass;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The type Common utils.
 */
@UtilityClass
public class CommonUtils {

    public static final Pattern BPN_NUMBER_PATTERN = Pattern.compile(StringPool.BPN_NUMBER_REGEX);

    /**
     * Gets identifier type.
     *
     * @param identifier the identifier
     * @return the identifier type
     */
    public static String getIdentifierType(String identifier) {
        if (identifier.startsWith("did:web")) {
            return StringPool.DID;
        } else {
            Validate.isFalse(BPN_NUMBER_PATTERN.matcher(identifier).matches()).launch(new BadDataException("Invalid BPN number - " + identifier));
            return StringPool.BPN;
        }
    }

    public static HoldersCredential convertVerifiableCredential(VerifiableCredential verifiableCredential, CredentialCreationConfig config) {
        List<String> cloneTypes = new ArrayList<>(config.getTypes());
        cloneTypes.remove(VerifiableCredentialType.VERIFIABLE_CREDENTIAL);

        // Create Credential
        return HoldersCredential.builder()
                .holderDid(config.getHolderDid())
                .issuerDid(config.getIssuerDoc().getId().toString())
                .type(String.join(",", cloneTypes))
                .credentialId(verifiableCredential.getId().toString())
                .data(verifiableCredential)
                .selfIssued(config.isSelfIssued())
                .build();
    }

}
