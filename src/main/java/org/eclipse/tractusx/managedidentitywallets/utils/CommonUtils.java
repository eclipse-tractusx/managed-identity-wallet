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
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.domain.CredentialCreationConfig;
import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.service.WalletKeyService;
import org.eclipse.tractusx.ssi.lib.crypt.octet.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.crypt.x25519.X25519PrivateKey;
import org.eclipse.tractusx.ssi.lib.exception.did.DidParseException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtFactory;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtVCFactoryImpl;
import org.springframework.util.MultiValueMap;

import java.io.StringWriter;
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

    @SneakyThrows({ DidParseException.class })
    public static String vcAsJwt(Wallet issuerWallet, Wallet holderWallet, VerifiableCredential vc, WalletKeyService walletKeyService) {

        Did issuerDid = DidParser.parse(issuerWallet.getDid());
        Did holderDid = DidParser.parse(holderWallet.getDid());

        // JWT Factory
        SerializedJwtVCFactoryImpl vcFactory = new SerializedJwtVCFactoryImpl(
                new SignedJwtFactory(new OctetKeyPairFactory()));

        X25519PrivateKey privateKey = (X25519PrivateKey) walletKeyService.getPrivateKeyByWalletIdAndAlgorithm(issuerWallet.getId(), SupportedAlgorithms.ED25519);
        // JWT Factory
        SignedJWT vcJWT = vcFactory.createVCJwt(issuerDid, holderDid, vc,
                privateKey,
                walletKeyService.getWalletKeyIdByWalletId(issuerWallet.getId(), SupportedAlgorithms.ED25519)
        );
        return vcJWT.serialize();
    }
}
