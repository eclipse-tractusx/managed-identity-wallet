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

import com.nimbusds.jwt.SignedJWT;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Credential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.CredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.ForbiddenException;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebDocumentResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.exception.DidDocumentResolverNotRegisteredException;
import org.eclipse.tractusx.ssi.lib.exception.JwtException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtFactory;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtValidator;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtVerifier;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationType;
import org.eclipse.tractusx.ssi.lib.resolver.DidDocumentResolverRegistryImpl;
import org.eclipse.tractusx.ssi.lib.resolver.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.serialization.jsonLd.JsonLdSerializerImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactory;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactoryImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;

/**
 * The type Presentation service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PresentationService extends BaseService<Credential, Long> {

    private final CredentialRepository credentialRepository;


    private final SpecificationUtil<Credential> credentialSpecificationUtil;

    private final WalletService walletService;

    private final WalletKeyService walletKeyService;

    @Override
    protected BaseRepository<Credential, Long> getRepository() {
        return credentialRepository;
    }

    @Override
    protected SpecificationUtil<Credential> getSpecificationUtil() {
        return credentialSpecificationUtil;
    }

    /**
     * Create presentation map.
     *
     * @param data      the data
     * @param asJwt     the as jwt
     * @param audience  the audience
     * @param callerBpn the caller bpn
     * @return the map
     */
    public Map<String, Object> createPresentation(Map<String, Object> data, boolean asJwt, String audience, String callerBpn) {
        Map<String, Object> response = new HashMap<>();

        String holderIdentifier = data.get("holderIdentifier").toString();

        //check if holder wallet is in the system
        Wallet holderWallet = walletService.getWalletByIdentifier(holderIdentifier);


        List<Map<String, Object>> verifiableCredentialList = (List<Map<String, Object>>) data.get("verifiableCredentials");

        //only support one credential at a time to create VP
        Validate.isTrue(verifiableCredentialList.size() > 1).launch(new BadDataException("Only one credentials is supported to create presentation"));

        List<VerifiableCredential> verifiableCredentials = new ArrayList<>(verifiableCredentialList.size());
        verifiableCredentialList.forEach(map -> {
            VerifiableCredential verifiableCredential = new VerifiableCredential(map);
            validateCredential(verifiableCredential, holderIdentifier);

            verifiableCredentials.add(verifiableCredential);
        });

        String issuerDidString = URLDecoder.decode(verifiableCredentials.get(0).getIssuer().toString(), Charset.defaultCharset());
        Did issuerDid = DidParser.parse(verifiableCredentials.get(0).getIssuer());
        Wallet issuerWallet = walletService.getWalletByIdentifier(issuerDidString);

        //validate BPN access
        Validate.isFalse(holderWallet.getBpn().equalsIgnoreCase(callerBpn)).launch(new ForbiddenException("Issuer wallet BPN is not matching with request BPN(from the token)"));

        if (asJwt) {

            Validate.isFalse(StringUtils.hasText(audience)).launch(new BadDataException("Audience needed to create VP as JWT"));

            //JWT Factory
            SerializedJwtPresentationFactory presentationFactory = new SerializedJwtPresentationFactoryImpl(
                    new SignedJwtFactory(new OctetKeyPairFactory()), new JsonLdSerializerImpl(), issuerDid);

            //Build JWT
            SignedJWT presentation = presentationFactory.createPresentation(
                    issuerDid, verifiableCredentials, audience, walletKeyService.getEd25519Key(holderWallet.getId()));

            response.put("vp", presentation.serialize());
        } else {
            VerifiablePresentationBuilder verifiablePresentationBuilder =
                    new VerifiablePresentationBuilder();

            // Build VP
            VerifiablePresentation verifiablePresentation =
                    verifiablePresentationBuilder
                            .id(URI.create(UUID.randomUUID().toString()))
                            .type(List.of(VerifiablePresentationType.VERIFIABLE_PRESENTATION))
                            .verifiableCredentials(verifiableCredentials)
                            .build();
            response.put("vp", verifiablePresentation);
        }
        return response;
    }


    public Map<String, Object> validatePresentation(Map<String, Object> vp, boolean asJwt, boolean withCredentialExpiryDate, String audience) {

        Map<String, Object> response = new HashMap<>();
        if (asJwt) {
            //verify as jwt
            Validate.isNull(vp.get("vp")).launch(new BadDataException("Can not find JWT"));
            String jwt = vp.get("vp").toString();
            response.put("vp", jwt);
            try {
                SignedJWT signedJWT = SignedJWT.parse(jwt);

                //validate jwt signature
                DidWebParser didParser = new DidWebParser();
                var httpClient = HttpClient.newHttpClient();
                var enforceHttps = true;

                var didDocumentResolverRegistry = new DidDocumentResolverRegistryImpl();
                didDocumentResolverRegistry.register(
                        new DidWebDocumentResolver(httpClient, didParser, enforceHttps));

                SignedJwtVerifier jwtVerifier = new SignedJwtVerifier(didDocumentResolverRegistry);
                jwtVerifier.verify(signedJWT);


                //validate audience
                if (StringUtils.hasText(audience)) {
                    SignedJwtValidator jwtValidator = new SignedJwtValidator();
                    jwtValidator.validateAudiences(signedJWT, audience);
                }

                //validate date
                if (withCredentialExpiryDate) {
                    SignedJwtValidator jwtValidator = new SignedJwtValidator();
                    jwtValidator.validateDate(signedJWT);
                }

                response.put("valid", true);
            } catch (JwtException | DidDocumentResolverNotRegisteredException | ParseException e) {
                log.error("Can not verify VP as JWT ", e);
                response.put("valid", false);
            }

        } else {
            //verify as JSON-LD

        }

        return response;
    }

    private void validateCredential(VerifiableCredential verifiableCredential, String holderIdentifier) {

        //check holders
        Validate.isFalse(verifiableCredential.getCredentialSubject().get(0).get("id").toString().equals(holderIdentifier)).launch(new ForbiddenException("VC " + verifiableCredential.getTypes() + " is not match with holder identifier " + holderIdentifier));

        //TODO need to validate policies
    }
}
