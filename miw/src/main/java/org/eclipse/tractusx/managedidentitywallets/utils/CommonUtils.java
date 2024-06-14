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

package org.eclipse.tractusx.managedidentitywallets.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.springframework.util.MultiValueMap;

import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            Validate.isFalse(BPN_NUMBER_PATTERN.matcher(identifier).matches())
                    .launch(new BadDataException("Invalid BPN number - " + identifier));
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

    @SneakyThrows
    public static String getKeyString(byte[] privateKeyBytes, String keyType) {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject(keyType, privateKeyBytes));
        pemWriter.flush();
        pemWriter.close();
        return stringWriter.toString();
    }


    public static SecureTokenRequest getSecureTokenRequest(MultiValueMap<String, String> map) {
        final ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> singleValueMap = map.toSingleValueMap();
        return objectMapper.convertValue(singleValueMap, SecureTokenRequest.class);
    }

    @SneakyThrows
    public static ECPrivateKey ecPrivateFrom(byte[] encoded) {
        var kf = KeyFactory.getInstance("EC");
        var privateKeySpec = new PKCS8EncodedKeySpec(encoded);
        return (ECPrivateKey) kf.generatePrivate(privateKeySpec);
    }

    @SneakyThrows
    public static ECPublicKey ecPublicFrom(byte[] encoded) {
        var kf = KeyFactory.getInstance("EC");
        var publicKeySpec = new X509EncodedKeySpec(encoded);
        return (ECPublicKey) kf.generatePublic(publicKeySpec);
    }

}
