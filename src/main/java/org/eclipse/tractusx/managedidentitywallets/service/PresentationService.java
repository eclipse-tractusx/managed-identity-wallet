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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartsensesolutions.java.commons.base.repository.BaseRepository;
import com.smartsensesolutions.java.commons.base.service.BaseService;
import com.smartsensesolutions.java.commons.specification.SpecificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.constant.SupportedAlgorithms;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.HoldersCredential;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.JtiRecord;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.HoldersCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.dao.repository.JtiRepository;
import org.eclipse.tractusx.managedidentitywallets.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.exception.MissingVcTypesException;
import org.eclipse.tractusx.managedidentitywallets.exception.PermissionViolationException;
import org.eclipse.tractusx.managedidentitywallets.utils.Validate;
import org.eclipse.tractusx.ssi.lib.crypt.octet.OctetKeyPairFactory;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559PrivateKey;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.exception.InvalidJsonLdException;
import org.eclipse.tractusx.ssi.lib.exception.InvalidePrivateKeyFormat;
import org.eclipse.tractusx.ssi.lib.exception.JwtExpiredException;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtFactory;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtValidator;
import org.eclipse.tractusx.ssi.lib.jwt.SignedJwtVerifier;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidParser;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentationType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.eclipse.tractusx.ssi.lib.serialization.jsonLd.JsonLdSerializerImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactory;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedJwtPresentationFactoryImpl;
import org.eclipse.tractusx.ssi.lib.serialization.jwt.SerializedVerifiablePresentation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.security.interfaces.ECPrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.BLANK_SEPARATOR;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COMA_SEPARATOR;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.UNDERSCORE;
import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getClaimsSet;
import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getScope;
import static org.eclipse.tractusx.managedidentitywallets.utils.TokenParsingUtils.getStringClaim;
import static org.springframework.security.oauth2.jwt.JwtClaimNames.JTI;

/**
 * The type Presentation service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PresentationService extends BaseService<HoldersCredential, Long> {

    private final HoldersCredentialRepository holdersCredentialRepository;


    private final SpecificationUtil<HoldersCredential> credentialSpecificationUtil;

    private final CommonService commonService;

    private final WalletKeyService walletKeyService;

    private final MIWSettings miwSettings;

    private final DidDocumentResolverService didDocumentResolverService;

    private final JtiRepository jtiRepository;

    @Override
    protected BaseRepository<HoldersCredential, Long> getRepository() {
        return holdersCredentialRepository;
    }

    @Override
    protected SpecificationUtil<HoldersCredential> getSpecificationUtil() {
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
        List<Map<String, Object>> verifiableCredentialList = (List<Map<String, Object>>) data.get(StringPool.VERIFIABLE_CREDENTIALS);

        //check if holder wallet is in the system
        Wallet callerWallet = commonService.getWalletByIdentifier(callerBpn);

        List<VerifiableCredential> verifiableCredentials = new ArrayList<>(verifiableCredentialList.size());
        verifiableCredentialList.forEach(map -> {
            VerifiableCredential verifiableCredential = new VerifiableCredential(map);
            verifiableCredentials.add(verifiableCredential);
        });

        return buildVP(asJwt, audience, callerBpn, callerWallet, verifiableCredentials, SupportedAlgorithms.ED25519);
    }

    @SneakyThrows({ InvalidePrivateKeyFormat.class })
    private Map<String, Object> buildVP(boolean asJwt, String audience, String callerBpn, Wallet callerWallet,
                                        List<VerifiableCredential> verifiableCredentials, SupportedAlgorithms algorithm) {
        Map<String, Object> response = new HashMap<>();
        if (asJwt && algorithm.equals(SupportedAlgorithms.ES256K)) {
            buildVPJwtES256K(audience, callerBpn, callerWallet, verifiableCredentials, algorithm, response);
        } else if (asJwt && algorithm.equals(SupportedAlgorithms.ED25519)) {
            buildVPJwtEdDSA(audience, callerBpn, callerWallet, verifiableCredentials, algorithm, response);
        } else {
            buildVPJsonLd(callerBpn, verifiableCredentials, response);
        }
        return response;
    }

    private void buildVPJsonLd(String callerBpn, List<VerifiableCredential> verifiableCredentials, Map<String, Object> response) {
        log.debug("Creating VP as JSON-LD for bpn ->{}", callerBpn);
        VerifiablePresentationBuilder verifiablePresentationBuilder =
                new VerifiablePresentationBuilder();

        VerifiablePresentation verifiablePresentation =
                verifiablePresentationBuilder
                        .id(URI.create(miwSettings.authorityWalletDid() + "#" + UUID.randomUUID()))
                        .type(List.of(VerifiablePresentationType.VERIFIABLE_PRESENTATION))
                        .verifiableCredentials(verifiableCredentials)
                        .build();
        response.put(StringPool.VP, verifiablePresentation);
    }

    private void buildVPJwtEdDSA(String audience, String callerBpn, Wallet callerWallet, List<VerifiableCredential> verifiableCredentials, SupportedAlgorithms algorithm, Map<String, Object> response) throws InvalidePrivateKeyFormat {
        Pair<Did, Object> result = getPrivateKey(callerWallet, algorithm, audience, callerBpn);

        SerializedJwtPresentationFactory presentationFactory = new SerializedJwtPresentationFactoryImpl(
                new SignedJwtFactory(new OctetKeyPairFactory()), new JsonLdSerializerImpl(), result.getKey());

        x21559PrivateKey ed25519Key = (x21559PrivateKey) result.getRight();
        x21559PrivateKey privateKey = new x21559PrivateKey(ed25519Key.asByte());
        SignedJWT presentation = presentationFactory.createPresentation(result.getLeft(), verifiableCredentials, audience, privateKey);

        response.put(StringPool.VP, presentation.serialize());
    }

    private void buildVPJwtES256K(String audience, String callerBpn, Wallet callerWallet, List<VerifiableCredential> verifiableCredentials, SupportedAlgorithms algorithm, Map<String, Object> response) {
        Pair<Did, Object> result = getPrivateKey(callerWallet, algorithm, audience, callerBpn);
        ECPrivateKey ecPrivateKey = (ECPrivateKey) result.getRight();

        JwtPresentationES256KService presentationFactory = new JwtPresentationES256KService(result.getLeft(), new JsonLdSerializerImpl());
        SignedJWT presentation = presentationFactory.createPresentation(result.getLeft(), verifiableCredentials, audience, ecPrivateKey);

        response.put(StringPool.VP, presentation.serialize());
    }

    private Pair<Did, Object> getPrivateKey(Wallet callerWallet, SupportedAlgorithms algorithm, String audience, String callerBpn) {
        log.debug("Creating VP as JWT for bpn ->{}", callerBpn);
        Validate.isFalse(StringUtils.hasText(audience)).launch(new BadDataException("Audience needed to create VP as JWT"));

        //Issuer of VP is holder of VC
        Did vpIssuerDid = DidParser.parse(callerWallet.getDid());

        //Build JWT
        return Pair.of(vpIssuerDid, walletKeyService.getPrivateKeyByWalletIdentifierAndAlgorithm(callerWallet.getId(), algorithm));
    }


    /**
     * Validate presentation map.
     *
     * @param vp                       the vp
     * @param asJwt                    the as jwt
     * @param withCredentialExpiryDate the with credential expiry date
     * @param audience                 the audience
     * @return the map
     */
    @SneakyThrows
    public Map<String, Object> validatePresentation(Map<String, Object> vp, boolean asJwt, boolean withCredentialExpiryDate, String audience) {

        Map<String, Object> response = new HashMap<>();
        if (asJwt) {
            log.debug("Validating VP as JWT");
            //verify as jwt
            Validate.isNull(vp.get(StringPool.VP)).launch(new BadDataException("Can not find JWT"));
            String jwt = vp.get(StringPool.VP).toString();
            response.put(StringPool.VP, jwt);

            SignedJWT signedJWT = SignedJWT.parse(jwt);

            boolean validateSignature = validateSignature(signedJWT);

            //validate audience
            boolean validateAudience = validateAudience(audience, signedJWT);

            //validate jwt date
            boolean validateJWTExpiryDate = validateJWTExpiryDate(signedJWT);
            response.put(StringPool.VALIDATE_JWT_EXPIRY_DATE, validateJWTExpiryDate);

            boolean validCredential = true;
            boolean validateExpiryDate = true;
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> claims = mapper.readValue(signedJWT.getPayload().toBytes(), Map.class);
                String vpClaim = mapper.writeValueAsString(claims.get("vp"));

                JsonLdSerializerImpl jsonLdSerializer = new JsonLdSerializerImpl();
                VerifiablePresentation presentation = jsonLdSerializer.deserializePresentation(new SerializedVerifiablePresentation(vpClaim));

                for (VerifiableCredential credential : presentation.getVerifiableCredentials()) {
                    validateExpiryDate = CommonService.validateExpiry(withCredentialExpiryDate, credential, response);
                    if (!validateCredential(credential)) {
                        validCredential = false;
                    }
                }
            } catch (InvalidJsonLdException e) {
                throw new BadDataException(String.format("Invalid Json-LD: %s", e.getMessage()));
            }

            response.put(StringPool.VALID, (validateSignature && validateAudience && validateExpiryDate && validCredential && validateJWTExpiryDate));

            if (StringUtils.hasText(audience)) {
                response.put(StringPool.VALIDATE_AUDIENCE, validateAudience);

            }

        } else {
            log.debug("Validating VP as json-ld");
            throw new BadDataException("Validation of VP in form of JSON-LD is not supported");
        }

        return response;
    }

    private boolean validateSignature(SignedJWT signedJWT) {
        //validate jwt signature
        try {
            SignedJwtVerifier jwtVerifier = new SignedJwtVerifier(didDocumentResolverService.getCompositeDidResolver());
            return jwtVerifier.verify(signedJWT);
        } catch (Exception e) {
            log.error("Can not verify signature of jwt", e);
            return false;
        }
    }

    private boolean validateJWTExpiryDate(SignedJWT signedJWT) {
        try {
            SignedJwtValidator jwtValidator = new SignedJwtValidator();
            jwtValidator.validateDate(signedJWT);
            return true;
        } catch (Exception e) {
            if (!(e instanceof JwtExpiredException)) {
                log.error("Can not validate jwt expiry date ", e);
            }
            return false;
        }
    }

    private boolean validateAudience(String audience, SignedJWT signedJWT) {
        if (StringUtils.hasText(audience)) {
            try {
                SignedJwtValidator jwtValidator = new SignedJwtValidator();
                jwtValidator.validateAudiences(signedJWT, audience);
                return true;
            } catch (Exception e) {
                log.error("Can not validate audience ", e);
                return false;
            }
        } else {
            return true;
        }
    }

    @SneakyThrows
    private boolean validateCredential(VerifiableCredential credential) {
        final DidResolver resolver = didDocumentResolverService.getCompositeDidResolver();
        final LinkedDataProofValidation linkedDataProofValidation = LinkedDataProofValidation.newInstance(resolver);
        final boolean isValid = linkedDataProofValidation.verify(credential);

        if (isValid) {
            log.debug("Credential validation result: (valid: {}, credential-id: {})", isValid, credential.getId());
        } else {
            log.info("Credential validation result: (valid: {}, credential-id: {})", isValid, credential.getId());
        }
        return isValid;
    }

    public Map<String, Object> createVpWithRequiredScopes(SignedJWT innerJWT, boolean asJwt) {

        JWTClaimsSet jwtClaimsSet = getClaimsSet(innerJWT);
        JtiRecord jtiRecord = getJtiRecord(jwtClaimsSet);

        List<HoldersCredential> holdersCredentials = new ArrayList<>();
        List<String> missingVCTypes = new ArrayList<>();
        List<VerifiableCredential> verifiableCredentials = new ArrayList<>();

        String scopeValue = getScope(jwtClaimsSet);
        String[] scopes = scopeValue.split(BLANK_SEPARATOR);

        for (String scope : scopes) {
            String[] scopeParts = scope.split(COLON_SEPARATOR);
            String vcType = scopeParts[1];
            checkReadPermission(scopeParts[2]);
            String vcTypeNoVersion = removeVersion(vcType);

            List<HoldersCredential> credentials =
                    holdersCredentialRepository.getByHolderDidAndType(jwtClaimsSet.getIssuer(), vcTypeNoVersion);
            if ((null == credentials) || credentials.isEmpty()) {
                missingVCTypes.add(vcTypeNoVersion);
            } else {
                holdersCredentials.addAll(credentials);
            }
        }

        checkMissingVcs(missingVCTypes);

        Wallet callerWallet = commonService.getWalletByIdentifier(jwtClaimsSet.getIssuer());

        holdersCredentials.forEach(c -> verifiableCredentials.add(c.getData()));

        // if as JWT true -> get key ES256K and sign with it
        Map<String, Object> vp = buildVP(asJwt, jwtClaimsSet.getAudience().get(0), callerWallet.getBpn(),
                callerWallet, verifiableCredentials, SupportedAlgorithms.ES256K);
        changeJtiStatus(jtiRecord);
        return vp;
    }

    private void checkReadPermission(String permission) {
        if (!"read".equals(permission)) {
            throw new PermissionViolationException("Scopes must have only READ permission");
        }
    }

    private void checkMissingVcs(List<String> missingVCTypes) {
        if (!missingVCTypes.isEmpty()) {
            throw new MissingVcTypesException(String.format("Missing VC types: %s",
                    String.join(COMA_SEPARATOR, missingVCTypes)));
        }
    }

    private String removeVersion(String vcType) {
        String[] parts = vcType.split(UNDERSCORE);
        return (parts.length > 1) ? parts[0] : vcType;
    }

    private JtiRecord getJtiRecord(JWTClaimsSet jwtClaimsSet) {
        String jtiValue = getStringClaim(jwtClaimsSet, JTI);
        JtiRecord jtiRecord = jtiRepository.getByJti(UUID.fromString(jtiValue));
        if (Objects.isNull(jtiRecord)) {
            JtiRecord jtiToAdd = JtiRecord.builder().jti(UUID.fromString(jtiValue)).isUsedStatus(false).build();
            jtiRepository.save(jtiToAdd);
            return jtiToAdd;
        } else if (jtiRecord.isUsedStatus()) {
            throw new BadDataException("The token was already used");
        } else {
            return jtiRecord;
        }
    }

    private void changeJtiStatus(JtiRecord jtiRecord) {
        jtiRecord.setUsedStatus(true);
        jtiRepository.save(jtiRecord);
    }
}
