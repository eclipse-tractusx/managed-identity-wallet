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

package org.eclipse.tractusx.managedidentitywallets.revocation.services;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.CredentialStatus;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.commons.exception.BadDataException;
import org.eclipse.tractusx.managedidentitywallets.revocation.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.revocation.constant.RevocationApiEndpoints;
import org.eclipse.tractusx.managedidentitywallets.revocation.domain.BPN;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.CredentialStatusDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusEntryDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusListCredentialSubject;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.BitSetManagerException;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.RevocationServiceException;
import org.eclipse.tractusx.managedidentitywallets.revocation.jpa.StatusListCredential;
import org.eclipse.tractusx.managedidentitywallets.revocation.jpa.StatusListIndex;
import org.eclipse.tractusx.managedidentitywallets.revocation.repository.StatusListCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.revocation.repository.StatusListIndexRepository;
import org.eclipse.tractusx.managedidentitywallets.revocation.utils.BitSetManager;
import org.eclipse.tractusx.managedidentitywallets.revocation.utils.CommonUtils;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.eclipse.tractusx.ssi.lib.exception.did.DidParseException;
import org.eclipse.tractusx.ssi.lib.exception.json.TransformJsonLdException;
import org.eclipse.tractusx.ssi.lib.exception.key.InvalidPublicKeyFormatException;
import org.eclipse.tractusx.ssi.lib.exception.proof.NoVerificationKeyFoundException;
import org.eclipse.tractusx.ssi.lib.exception.proof.SignatureParseException;
import org.eclipse.tractusx.ssi.lib.exception.proof.SignatureVerificationFailedException;
import org.eclipse.tractusx.ssi.lib.exception.proof.UnsupportedSignatureTypeException;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The `RevocationService` class is a Java service that handles the revocation of credentials and
 * the creation of status lists.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RevocationService {

    public static final String ENCODED_LIST = "encodedList";

    private final StatusListCredentialRepository statusListCredentialRepository;

    private final StatusListIndexRepository statusListIndexRepository;

    private final HttpClientService httpClientService;

    private final MIWSettings miwSettings;

    /**
     * Verifies the status of a credential based on the provided CredentialStatusDto object.
     *
     * @param statusDto The CredentialStatusDto object containing the necessary information for status verification.
     * @return A Map object with the key "status" and the value "revoked" or "active" indicating the status of the credential.
     * @throws BadDataException If the status list VC is not found for the issuer.
     */
    @Transactional

    public Map<String, String> verifyStatus(CredentialStatusDto statusDto) {

        validateCredentialStatus(statusDto);

        String url = statusDto.statusListCredential();

        String[] values = CommonUtils.extractValuesFromURL(url);
        VerifiableCredential statusListCredential = getStatusListCredential(values[0], values[1], values[2]);
        if (Objects.isNull(statusListCredential)) {
            log.error("Status list VC not found for issuer -> {}",
                    values[0]);
            throw new BadDataException("Status list VC not found for issuer -> " + values[0]);
        }

        //validate status list VC
        validateStatusListVC(statusListCredential);

        String encodedList = statusListCredential.getCredentialSubject().get(0).get(ENCODED_LIST).toString();

        BitSet bitSet = BitSetManager.decompress(BitSetManager.decodeFromString(encodedList));
        int index = Integer.parseInt(statusDto.statusListIndex());
        boolean status = bitSet.get(index);
        return Map.of(StringPool.STATUS, status ? CredentialStatus.REVOKED.getName() : CredentialStatus.ACTIVE.getName());
    }


    private void validateStatusListVC(VerifiableCredential statusListCredential) {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        DidResolver didResolver = new DidWebResolver(httpClient, new DidWebParser(), true);
        LinkedDataProofValidation proofValidation = LinkedDataProofValidation.newInstance(didResolver);
        boolean valid = false;
        try {
            valid = proofValidation.verify(statusListCredential);
        } catch (UnsupportedSignatureTypeException | SignatureParseException | DidParseException |
                 InvalidPublicKeyFormatException | SignatureVerificationFailedException |
                 NoVerificationKeyFoundException | TransformJsonLdException e) {
            log.error("Verification failed with error -> {}", e.getMessage(), e);
        }
        if (!valid) {
            throw new BadDataException("Status list credential is not valid");
        }
    }

    /**
     * The `revoke` function revokes a credential by updating the status list credential with a new
     * subject and saving it to the repository.
     *
     * @param dto   The `dto` parameter is an instance of the `CredentialStatusDto` class.
     * @param token the token
     * @throws RevocationServiceException the revocation service exception
     */
    @Transactional
    public void revoke(CredentialStatusDto dto, String token) throws RevocationServiceException {
        StatusListCredential statusListCredential;
        StatusListCredentialSubject newSubject;
        VerifiableCredential statusListVC;
        VerifiableCredential signedStatusListVC;
        String encodedList;
        VerifiableCredentialSubject subjectCredential;

        validateCredentialStatus(dto);

        statusListCredential =
                statusListCredentialRepository
                        .findById(extractIdFromURL(dto.statusListCredential()))
                        .orElseThrow(() -> new RevocationServiceException("Status list credential not found"));
        statusListVC = statusListCredential.getCredential();
        subjectCredential = statusListVC.getCredentialSubject().get(0);
        encodedList = (String) subjectCredential.get(StatusListCredentialSubject.SUBJECT_ENCODED_LIST);
        String newEncodedList;
        try {
            newEncodedList =
                    BitSetManager.revokeCredential(encodedList, Integer.parseInt(dto.statusListIndex()));
        } catch (BitSetManagerException e) {
            log.error(null, e);
            throw new RevocationServiceException(e);
        }
        newSubject =
                StatusListCredentialSubject.builder()
                        .id((String) subjectCredential.get(StatusListCredentialSubject.SUBJECT_ID))
                        .type(StatusListCredentialSubject.TYPE_LIST)
                        .statusPurpose(
                                (String) subjectCredential.get(StatusListCredentialSubject.SUBJECT_STATUS_PURPOSE))
                        .encodedList(newEncodedList)
                        .build();
        statusListVC.remove("proof");
        // #TODO credentialSubject should not be a list fix that in SSI LIB
        statusListVC.put("credentialSubject", List.of(createCredentialSubject(newSubject)));
        signedStatusListVC = httpClientService.signStatusListVC(statusListVC, token);
        statusListCredential.setCredential(signedStatusListVC);
        log.info("Revoked credential with id:{} , index->{}", dto.id(), dto.statusListIndex());
        statusListCredentialRepository.saveAndFlush(statusListCredential);
    }

    /**
     * The function creates or updates a status list for a given issuer and purpose, and returns a
     * CredentialStatusDto object.
     *
     * @param dto   The parameter "dto" is of type "StatusEntryDto".
     * @param token the token
     * @return The method is returning a CredentialStatusDto object.
     */
    @Transactional
    public CredentialStatusDto createStatusList(StatusEntryDto dto, String token) {
        StatusListIndex statusListIndex;
        String vcUrl;
        List<StatusListIndex> statusListIndexs;
        String bpn;

        bpn = extractBpnFromDid(dto.issuerId());
        statusListIndexs =
                statusListIndexRepository.findByIssuerBpnStatus(bpn + "-" + dto.purpose().toLowerCase());
        statusListIndex =
                statusListIndexs.stream()
                        .filter(li -> Integer.parseInt(li.getCurrentIndex()) + 1 < BitSetManager.BITSET_SIZE)
                        .findFirst()
                        .orElseGet(
                                () ->
                                        createStatusListIndex(
                                                dto,
                                                statusListIndexs.size() + 1,
                                                createStatusListCredential(dto, statusListIndexs.size() + 1, token)));
        if (statusListIndex.getCurrentIndex().equals("-1")) {
            statusListIndex.setCurrentIndex("0");
            statusListIndexRepository.save(statusListIndex);
            log.info("Created new status list for issuer: " + bpn);
        } else {
            statusListIndex.setCurrentIndex(
                    String.valueOf(Integer.parseInt(statusListIndex.getCurrentIndex()) + 1));
            statusListIndexRepository.save(statusListIndex);
            log.info("Updated status list for issuer: " + bpn);
        }
        vcUrl =
                httpClientService.domainUrl
                        + RevocationApiEndpoints.REVOCATION_API
                        + RevocationApiEndpoints.CREDENTIALS_STATUS_INDEX
                        .replace("{issuerBPN}", bpn)
                        .replace("{status}", dto.purpose().toLowerCase())
                        .replace("{index}", String.valueOf(statusListIndex.getId().split("#")[1]));
        return new CredentialStatusDto(
                vcUrl + "#" + statusListIndex.getCurrentIndex(),
                dto.purpose(),
                statusListIndex.getCurrentIndex(),
                vcUrl,
                StatusListCredentialSubject.TYPE_ENTRY);
    }


    /**
     * The function `getStatusLisCredential` retrieves a `VerifiableCredential` object from the
     * `statusListCredentialRepository` based on identity
     *
     * @param issuerBpn the issuer bpn
     * @param status    the status
     * @param index     the index
     * @return the status list credential
     */
    @Transactional
    public VerifiableCredential getStatusListCredential(
            String issuerBpn, String status, String index) {
        return statusListCredentialRepository
                .findById(issuerBpn + "-" + status + "#" + index)
                .map(StatusListCredential::getCredential)
                .orElse(null);
    }

    /**
     * The function creates a map of key-value pairs representing the properties of a
     * StatusListCredentialSubject object.
     *
     * @param subject The `subject` parameter is an instance of the `StatusListCredentialSubject`
     *                class.
     * @return The method is returning a Map<String, Object> object.
     */
    private Map<String, Object> createCredentialSubject(StatusListCredentialSubject subject) {
        Map<String, Object> credentialSubjectMap = new HashMap<>();
        credentialSubjectMap.put(StatusListCredentialSubject.SUBJECT_ID, subject.getId());
        credentialSubjectMap.put(StatusListCredentialSubject.SUBJECT_TYPE, subject.getType());
        credentialSubjectMap.put(
                StatusListCredentialSubject.SUBJECT_STATUS_PURPOSE, subject.getStatusPurpose());
        credentialSubjectMap.put(
                StatusListCredentialSubject.SUBJECT_ENCODED_LIST, subject.getEncodedList());
        return credentialSubjectMap;
    }

    /**
     * The function creates a status list credential with a given status entry DTO.
     *
     * @param dto The "dto" parameter is an object of type "StatusEntryDto". It contains information
     *            about the status entry, such as the issuer ID and the purpose of the status.
     * @return The method `createStatusListCredential` returns a `StatusListCredential` object.
     */
    @SneakyThrows
    private StatusListCredential createStatusListCredential(
            StatusEntryDto dto, Integer size, String token) {
        String id;
        String bpn;
        List<String> types = new ArrayList<>();
        VerifiableCredential statusListVC;
        StatusListCredentialSubject subject;
        types.add(VerifiableCredentialType.VERIFIABLE_CREDENTIAL);
        types.add(StatusListCredentialSubject.TYPE_CREDENTIAL);

        bpn = extractBpnFromDid(dto.issuerId());
        id =
                httpClientService.domainUrl
                        + RevocationApiEndpoints.REVOCATION_API
                        + RevocationApiEndpoints.CREDENTIALS_STATUS_INDEX
                        .replace("{issuerBPN}", bpn)
                        .replace("{status}", dto.purpose().toLowerCase())
                        .replace("{index}", String.valueOf(size));
        subject =
                StatusListCredentialSubject.builder()
                        .id(id)
                        .statusPurpose(dto.purpose().toLowerCase())
                        .type(StatusListCredentialSubject.TYPE_LIST)
                        .encodedList(BitSetManager.initializeEncodedListString())
                        .build();

        // TODO credentialSubject should not be a list fix that in SSI LIB
        statusListVC =
                new VerifiableCredentialBuilder()
                        .context(miwSettings.vcContexts())
                        .id(URI.create(id))
                        .type(types)
                        .issuer(URI.create(dto.issuerId()))
                        .issuanceDate(Instant.now())
                        .credentialSubject(new VerifiableCredentialSubject(createCredentialSubject(subject)))
                        .build();
        return StatusListCredential.builder()
                .id(bpn + "-" + dto.purpose().toLowerCase() + "#" + size)
                .issuerBpn(bpn)
                .credential(httpClientService.signStatusListVC(statusListVC, token))
                .build();
    }


    private StatusListIndex createStatusListIndex(
            StatusEntryDto dto, Integer size, StatusListCredential statusListCredential) {
        String bpn = extractBpnFromDid(dto.issuerId());
        return StatusListIndex.builder()
                .id(bpn + "-" + dto.purpose().toLowerCase() + "#" + size)
                .currentIndex("-1")
                .statusListCredential(statusListCredential)
                .issuerBpnStatus(bpn + "-" + dto.purpose().toLowerCase())
                .build();
    }

    /**
     * The function extracts a BPN from a credential status.
     *
     * @param url The `url` parameter is a string that represents a credential status.
     * @return The method is returning a string that is a combination of the first part of the "id"
     */
    @SneakyThrows
    public String extractBpnFromURL(String url) {
        Pattern pattern = Pattern.compile("/credentials/(B\\w+)/", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        } else {
            throw new Exception("No match found");
        }
    }

    /**
     * The function extracts an ID from a status credential id .
     *
     * @param url The `url` parameter is a string that represents a credential status.
     * @return The method is returning a string that is a combination of the first part of the "id"
     */
    public String extractIdFromURL(String url) {
        Pattern pattern =
                Pattern.compile("/credentials/(B\\w+)/(.*?)/(\\d+)", Pattern.CASE_INSENSITIVE);
        // Create a Matcher object and find the first match in the URL
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String bpnlNumber = matcher.group(1);
            String purpose = matcher.group(2);
            String credentialIndex = matcher.group(3);
            return bpnlNumber.toUpperCase() + "-" + purpose + "#" + credentialIndex;
        } else {
            throw new IllegalArgumentException("No match found");
        }
    }

    /**
     * The function extracts an ID from a credential status.
     *
     * @param did The `did` parameter is a string that represents a credential status.
     * @return The method is returning a string that is a combination of the first part of the "id"
     */
    public String extractBpnFromDid(String did) {
        return new BPN(did.substring(did.lastIndexOf(":") + 1).toUpperCase()).value();
    }

    /**
     * The function should validate the Credential Status group(1) = BPN Number group(2) = Purpose
     * group(3) = status list credential Index group(4) = index of the verifiable credential in the
     * status list
     *
     * @param dto the dto
     * @throws IllegalArgumentException if the Credential Status is invalid
     */
    public void validateCredentialStatus(CredentialStatusDto dto) {
        String domainUrl = httpClientService.domainUrl + RevocationApiEndpoints.REVOCATION_API;
        String urlPattern = domainUrl + "/credentials/(B\\w+)/(.*?)/(\\d+)";
        Pattern pattern0 = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Pattern pattern1 = Pattern.compile(urlPattern + "#(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher0 = pattern0.matcher(dto.statusListCredential());
        Matcher matcher1 = pattern1.matcher(dto.id());
        if (!matcher0.find()) {
            throw new IllegalArgumentException("Invalid credential status url");
        }
        if (!matcher1.find()) {
            throw new IllegalArgumentException("Invalid credential status id");
        }

        if (!matcher0.group(1).equals(matcher1.group(1))
                || !matcher0.group(2).equals(matcher1.group(2))
                || !matcher0.group(3).equals(matcher1.group(3))) {
            throw new IllegalArgumentException("Credential status url and id do not match");
        }

        if (!matcher1.group(4).equals(dto.statusListIndex())) {
            throw new IllegalArgumentException(
                    "Credential status index in the id does not match the current index in the dto");
        }

        if (!matcher0.group(2).equals(dto.statusPurpose())) {
            throw new IllegalArgumentException(
                    "Credential status purpose does not match the statusPurpose in the dto");
        }
    }
}
