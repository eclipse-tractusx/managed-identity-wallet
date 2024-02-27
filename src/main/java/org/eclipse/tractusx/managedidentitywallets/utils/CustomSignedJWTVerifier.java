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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.service.DidDocumentService;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.exception.UnsupportedVerificationMethodException;
import org.eclipse.tractusx.ssi.lib.model.MultibaseString;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.did.Ed25519VerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.did.JWKVerificationMethod;
import org.eclipse.tractusx.ssi.lib.model.did.VerificationMethod;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Data
public class CustomSignedJWTVerifier {
    private DidResolver didResolver;
    private final DidDocumentService didDocumentService;
    public static final String KID = "kid";

    public boolean verify(String did, SignedJWT jwt) throws JOSEException {
        VerificationMethod verificationMethod = checkVerificationMethod(did, jwt);
        if (JWKVerificationMethod.isInstance(verificationMethod)) {
            JWKVerificationMethod method = new JWKVerificationMethod(verificationMethod);
            String kty = method.getPublicKeyJwk().getKty();
            String crv = method.getPublicKeyJwk().getCrv();
            String x = method.getPublicKeyJwk().getX();
            if (!kty.equals("OKP") || !crv.equals("Ed25519")) {
                throw new UnsupportedVerificationMethodException(method, "Only kty:OKP with crv:Ed25519 is supported");
            }

            OctetKeyPair keyPair = (new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.from(x))).build();
            return jwt.verify(new Ed25519Verifier(keyPair));

        } else if (Ed25519VerificationMethod.isInstance(verificationMethod)) {
            Ed25519VerificationMethod method = new Ed25519VerificationMethod(verificationMethod);
            MultibaseString multibase = method.getPublicKeyBase58();
            Ed25519PublicKeyParameters publicKeyParameters = new Ed25519PublicKeyParameters(multibase.getDecoded(), 0);
            OctetKeyPair keyPair = (new OctetKeyPair.Builder(Curve.Ed25519, Base64URL.encode(publicKeyParameters.getEncoded()))).build();
            return jwt.verify(new Ed25519Verifier(keyPair));
        }
        return false;
    }

    public VerificationMethod checkVerificationMethod(String did, SignedJWT jwt) {
        Map<String, Object> headers = jwt.getHeader().toJSONObject();
        String kid = String.valueOf(headers.get(KID));
        DidDocument didDocument = didDocumentService.getDidDocument(did);
        for (VerificationMethod method : didDocument.getVerificationMethods()) {
            if (method.getId().toString().contains(kid)) {
                return method;
            }
        }
        throw new BadDataException("Verification method doesn't match 'kid' parameter");
    }
}
