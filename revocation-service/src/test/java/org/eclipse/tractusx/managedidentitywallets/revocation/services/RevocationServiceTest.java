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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.CredentialStatus;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil;
import org.eclipse.tractusx.managedidentitywallets.revocation.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.CredentialStatusDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusEntryDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusListCredentialSubject;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.BitSetManagerException;
import org.eclipse.tractusx.managedidentitywallets.revocation.exception.RevocationServiceException;
import org.eclipse.tractusx.managedidentitywallets.revocation.jpa.StatusListIndex;
import org.eclipse.tractusx.managedidentitywallets.revocation.repository.StatusListCredentialRepository;
import org.eclipse.tractusx.managedidentitywallets.revocation.repository.StatusListIndexRepository;
import org.eclipse.tractusx.managedidentitywallets.revocation.utils.BitSetManager;
import org.eclipse.tractusx.ssi.lib.did.resolver.DidResolver;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofValidation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Base64;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.BITSET_SIZE;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.BPN;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.DID;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.VC_CONTEXTS;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.decompressGzip;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockEmptyEncodedList;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockStatusListCredential;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockStatusListIndex;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.mockStatusListVC;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class RevocationServiceTest {

    private static final String CALLER_BPN = UUID.randomUUID().toString();

    private static StatusListCredentialRepository statusListCredentialRepository;

    private static StatusListIndexRepository statusListIndexRepository;

    private static HttpClientService httpClientService;

    private static RevocationService revocationService;

    private static MIWSettings miwSettings;

    @BeforeAll
    public static void beforeAll() {
        statusListCredentialRepository = Mockito.mock(StatusListCredentialRepository.class);
        statusListIndexRepository = Mockito.mock(StatusListIndexRepository.class);
        httpClientService = Mockito.mock(HttpClientService.class);
        miwSettings = new MIWSettings(VC_CONTEXTS);
        httpClientService.domainUrl = "http://example.com";
        revocationService =
                new RevocationService(
                        statusListCredentialRepository,
                        statusListIndexRepository,
                        httpClientService,
                        miwSettings);
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(statusListCredentialRepository, statusListIndexRepository, httpClientService);
    }


    @Nested
    class VerifyStatusTest {
        @SneakyThrows
        @Test
        void shouldVerifyStatusActive() {
            final var issuer = DID;
            var encodedList = mockEmptyEncodedList();
            var credentialBuilder = mockStatusListVC(issuer, "1", encodedList);
            var statusListCredential = mockStatusListCredential(issuer, credentialBuilder);
            // 1. create status list with the credential
            var statusListIndex = mockStatusListIndex(issuer, statusListCredential, "0");
            when(statusListIndex.getStatusListCredential()).thenReturn(statusListCredential);
            when(statusListCredentialRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(statusListCredential));
            CredentialStatusDto credentialStatusDto = Mockito.mock(CredentialStatusDto.class);
            when(credentialStatusDto.id())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1#0");
            when(credentialStatusDto.statusPurpose()).thenReturn("revocation");
            when(credentialStatusDto.statusListIndex()).thenReturn("0");
            when(credentialStatusDto.statusListCredential())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1");
            when(credentialStatusDto.type()).thenReturn("BitstringStatusListEntry");


            try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {
                LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
                utils.when(() -> {
                    LinkedDataProofValidation.newInstance(Mockito.any(DidResolver.class));
                }).thenReturn(mock);
                Mockito.when(mock.verify(Mockito.any(VerifiableCredential.class))).thenReturn(true);
                Map<String, String> status = revocationService.verifyStatus(credentialStatusDto);
                Assertions.assertTrue(status.get(StringPool.STATUS).equals(CredentialStatus.ACTIVE.getName()));
            }
        }

        @SneakyThrows
        @Test
        void shouldVerifyStatusRevoke() {

            String indexTORevoke = "0";
            final var issuer = DID;

            //set bit at index
            String encodedList = mockEmptyEncodedList();
            encodedList = BitSetManager.revokeCredential(encodedList, Integer.parseInt(indexTORevoke));


            var credentialBuilder = mockStatusListVC(issuer, "1", encodedList);
            var statusListCredential = mockStatusListCredential(issuer, credentialBuilder);
            // 1. create status list with the credential
            var statusListIndex = mockStatusListIndex(issuer, statusListCredential, "0");
            when(statusListIndex.getStatusListCredential()).thenReturn(statusListCredential);
            when(statusListCredentialRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(statusListCredential));
            CredentialStatusDto credentialStatusDto = Mockito.mock(CredentialStatusDto.class);
            when(credentialStatusDto.id())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1#0");
            when(credentialStatusDto.statusPurpose()).thenReturn("revocation");
            when(credentialStatusDto.statusListIndex()).thenReturn(indexTORevoke);
            when(credentialStatusDto.statusListCredential())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1");
            when(credentialStatusDto.type()).thenReturn("BitstringStatusListEntry");
            try (MockedStatic<LinkedDataProofValidation> utils = Mockito.mockStatic(LinkedDataProofValidation.class)) {
                LinkedDataProofValidation mock = Mockito.mock(LinkedDataProofValidation.class);
                utils.when(() -> {
                    LinkedDataProofValidation.newInstance(Mockito.any(DidResolver.class));
                }).thenReturn(mock);
                Mockito.when(mock.verify(Mockito.any(VerifiableCredential.class))).thenReturn(true);
                Map<String, String> status = revocationService.verifyStatus(credentialStatusDto);

                Assertions.assertTrue(status.get(StringPool.STATUS).equals(CredentialStatus.REVOKED.getName()));
            }
        }
    }


    @Nested
    class RevokeTest {

        @Test
        void shouldRevokeCredential() {
            final var issuer = DID;
            var encodedList = mockEmptyEncodedList();
            var credentialBuilder = mockStatusListVC(issuer, "1", encodedList);
            var statusListCredential = mockStatusListCredential(issuer, credentialBuilder);
            // 1. create status list with the credential
            var statusListIndex = mockStatusListIndex(issuer, statusListCredential, "0");
            when(statusListIndex.getStatusListCredential()).thenReturn(statusListCredential);
            when(statusListCredentialRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(statusListCredential));
            CredentialStatusDto credentialStatusDto = Mockito.mock(CredentialStatusDto.class);
            when(credentialStatusDto.id())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1#0");
            when(credentialStatusDto.statusPurpose()).thenReturn("revocation");
            when(credentialStatusDto.statusListIndex()).thenReturn("0");
            when(credentialStatusDto.statusListCredential())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1");
            when(credentialStatusDto.type()).thenReturn("BitstringStatusListEntry");
            assertDoesNotThrow(() -> revocationService.revoke(credentialStatusDto, "token"));
            Mockito.verify(statusListCredentialRepository, Mockito.times(1))
                    .saveAndFlush(eq(statusListCredential));
            ArgumentCaptor<VerifiableCredential> captor =
                    ArgumentCaptor.forClass(VerifiableCredential.class);
            Mockito.verify(httpClientService)
                    .signStatusListVC(captor.capture(), Mockito.any(String.class));
            VerifiableCredential newList = captor.getValue();
            VerifiableCredentialSubject verifiableCredentialSubject =
                    newList.getCredentialSubject().get(0);
            String encodedNewList = (String) verifiableCredentialSubject.get("encodedList");
            byte[] decodedNewList = Base64.getDecoder().decode(encodedNewList);
            BitSet decompressedNewList = decompressGzip(decodedNewList);
            byte[] decodedList = Base64.getDecoder().decode(encodedList);
            BitSet decompressedList = decompressGzip(decodedList);
            assertFalse(decompressedList.get(0));
            assertTrue(decompressedNewList.get(0));
        }

        @Test
        void shouldThrowRevocationServiceException() {
            final var issuer = DID;
            var encodedList = mockEmptyEncodedList();
            var credentialBuilder = mockStatusListVC(issuer, "1", encodedList);
            var statusListCredential = mockStatusListCredential(issuer, credentialBuilder);
            // 1. create status list with the credential
            var statusListIndex = mockStatusListIndex(issuer, statusListCredential, "0");
            when(statusListIndex.getStatusListCredential()).thenReturn(statusListCredential);
            when(statusListCredentialRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(statusListCredential));
            CredentialStatusDto credentialStatusDto = Mockito.mock(CredentialStatusDto.class);
            when(credentialStatusDto.id())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1#0");
            when(credentialStatusDto.statusPurpose()).thenReturn("revocation");
            when(credentialStatusDto.statusListIndex()).thenReturn("0");
            when(credentialStatusDto.statusListCredential())
                    .thenReturn(
                            "http://this-is-my-domain/api/v1/revocations/credentials/"
                                    + TestUtil.extractBpnFromDid(issuer)
                                    + "/revocation/1");
            when(credentialStatusDto.type()).thenReturn("BitstringStatusListEntry");
            try (MockedStatic<BitSetManager> utilities = Mockito.mockStatic(BitSetManager.class)) {
                utilities
                        .when(() -> BitSetManager.revokeCredential(any(String.class), any(Integer.class)))
                        .thenThrow(new BitSetManagerException());
                assertThrows(
                        RevocationServiceException.class,
                        () -> revocationService.revoke(credentialStatusDto, "token"));
            }
        }
    }

    @Nested
    class CreateStatusListTest {

        @Test
        void shouldCreateNewStatusList() {
            ReflectionTestUtils.setField(httpClientService, "domainUrl", "http://this-is-my-domain");
            StatusEntryDto mockStatus = Mockito.mock(StatusEntryDto.class);
            when(mockStatus.issuerId()).thenReturn(DID);
            when(mockStatus.purpose()).thenReturn("revocation");
            CredentialStatusDto credentialStatusDto =
                    assertDoesNotThrow(() -> revocationService.createStatusList(mockStatus, "token"));
            assertEquals("revocation", credentialStatusDto.statusPurpose());
            assertEquals(
                    "http://this-is-my-domain/api/v1/revocations/credentials/" + BPN + "/revocation/1#0",
                    credentialStatusDto.id());
            assertEquals("0", credentialStatusDto.statusListIndex());
            assertEquals(
                    "http://this-is-my-domain/api/v1/revocations/credentials/" + BPN + "/revocation/1",
                    credentialStatusDto.statusListCredential());
            assertEquals(StatusListCredentialSubject.TYPE_ENTRY, credentialStatusDto.type());
            Mockito.verify(statusListIndexRepository, times(1)).save(any(StatusListIndex.class));
        }

        @Test
        void shouldUpdateExistingStatusList() {
            ReflectionTestUtils.setField(httpClientService, "domainUrl", "http://this-is-my-domain");
            StatusListIndex statusListIndex =
                    StatusListIndex.builder()
                            .currentIndex("0")
                            .id(BPN + "-revocation#1")
                            .issuerBpnStatus(BPN + "-revocation")
                            .build();
            when(statusListIndexRepository.findByIssuerBpnStatus(BPN + "-revocation"))
                    .thenReturn(List.of(statusListIndex));
            StatusEntryDto mockStatus = Mockito.mock(StatusEntryDto.class);
            when(mockStatus.issuerId()).thenReturn(DID);
            when(mockStatus.purpose()).thenReturn("revocation");
            CredentialStatusDto credentialStatusDto =
                    assertDoesNotThrow(() -> revocationService.createStatusList(mockStatus, "token"));
            assertEquals("revocation", credentialStatusDto.statusPurpose());
            assertEquals(
                    "http://this-is-my-domain/api/v1/revocations/credentials/" + BPN + "/revocation/1#1",
                    credentialStatusDto.id());
            assertEquals("1", credentialStatusDto.statusListIndex());
            assertEquals(
                    "http://this-is-my-domain/api/v1/revocations/credentials/" + BPN + "/revocation/1",
                    credentialStatusDto.statusListCredential());
            assertEquals(StatusListCredentialSubject.TYPE_ENTRY, credentialStatusDto.type());
            Mockito.verify(statusListIndexRepository, times(1)).save(any(StatusListIndex.class));
        }

        @Test
        void shouldCreateNewStatusListWhenFirstFull() {
            ReflectionTestUtils.setField(httpClientService, "domainUrl", "http://this-is-my-domain");
            StatusListIndex statusListIndex =
                    StatusListIndex.builder()
                            .currentIndex(String.valueOf(BITSET_SIZE - 1))
                            .id(BPN + "-revocation#1")
                            .issuerBpnStatus(BPN + "-revocation")
                            .build();
            when(statusListIndexRepository.findByIssuerBpnStatus(BPN + "-revocation"))
                    .thenReturn(List.of(statusListIndex));
            StatusEntryDto mockStatus = Mockito.mock(StatusEntryDto.class);
            when(mockStatus.issuerId()).thenReturn(DID);
            when(mockStatus.purpose()).thenReturn("revocation");
            CredentialStatusDto credentialStatusDto =
                    assertDoesNotThrow(() -> revocationService.createStatusList(mockStatus, "token"));
            assertEquals("revocation", credentialStatusDto.statusPurpose());
            assertEquals(
                    "http://this-is-my-domain/api/v1/revocations/credentials/" + BPN + "/revocation/2#0",
                    credentialStatusDto.id());
            assertEquals("0", credentialStatusDto.statusListIndex());
            assertEquals(
                    "http://this-is-my-domain/api/v1/revocations/credentials/" + BPN + "/revocation/2",
                    credentialStatusDto.statusListCredential());
            assertEquals(StatusListCredentialSubject.TYPE_ENTRY, credentialStatusDto.type());
            Mockito.verify(statusListIndexRepository, times(1)).save(any(StatusListIndex.class));
        }
    }

    @Nested
    class GetStatusListCredential {

        @Test
        void shouldGetList() throws JsonProcessingException {
            final var issuer = DID;
            var fragment = UUID.randomUUID().toString();
            var encodedList = mockEmptyEncodedList();
            var credentialBuilder = mockStatusListVC(issuer, fragment, encodedList);
            var statusListCredential = mockStatusListCredential(issuer, credentialBuilder);
            when(statusListCredentialRepository.findById(any(String.class)))
                    .thenReturn(Optional.of(statusListCredential));
            VerifiableCredential verifiableCredential =
                    assertDoesNotThrow(
                            () -> revocationService.getStatusListCredential(BPN, "revocation", "1"));
            assertNotNull(verifiableCredential);
            assertEquals(
                    URI.create(TestUtil.STATUS_LIST_CREDENTIAL_SUBJECT_ID),
                    verifiableCredential.getCredentialSubject().get(0).getId());
            assertEquals(URI.create(issuer), verifiableCredential.getIssuer());
            assertEquals(URI.create(issuer + "#" + fragment), verifiableCredential.getId());
            assertEquals(verifiableCredential.getContext(), miwSettings.vcContexts());
        }

        @Test
        void shouldReturnNull() {
            when(statusListCredentialRepository.findById(any(String.class))).thenReturn(Optional.empty());
            VerifiableCredential verifiableCredential =
                    assertDoesNotThrow(() -> revocationService.getStatusListCredential("", "", ""));
            assertNull(verifiableCredential);
        }
    }

    @Nested
    class CheckSubStringExtraction {
        @Test
        void shouldExtractBpnFromDid() {
            assertEquals(revocationService.extractBpnFromDid(DID), BPN);
        }

        @Test
        void shouldExtractIdFromURL() {
            assertEquals(
                    revocationService.extractIdFromURL(
                            "http://this-is-my-domain/api/v1/revocations/credentials/BPNL123456789000/revocation/1"),
                    "BPNL123456789000-revocation#1");
        }

        @Test
        void shouldExtractIdFromURLCaseSensitive() {
            assertEquals(
                    revocationService.extractIdFromURL(
                            "http://this-is-my-domain/api/v1/revocations/credentials/bpnl123456789000/revocation/1"),
                    "BPNL123456789000-revocation#1");
        }

        @Test
        void shouldExtractBpnFromURL() {
            assertEquals(
                    revocationService.extractBpnFromURL(
                            "http://this-is-my-domain/api/v1/revocations/credentials/BPNL123456789000/revocation/1"),
                    BPN);
        }

        @Test
        void shouldExtractBpnFromURLCaseSensitive() {
            assertEquals(
                    revocationService.extractBpnFromURL(
                            "http://this-is-my-domain/api/v1/revocations/creDENTials/bpNl123456789000/revocation/1"),
                    BPN);
        }
    }

    @Nested
    class ValidateCredentialStatus {

        @Test
        @DisplayName("statusPurpose is valid")
        void validCredentialStatusDto() {
            String statusIndex = "1";
            String statusListCredential =
                    "http://example.com/api/v1/revocations/credentials/" + BPN + "/revocation/1";
            String id = statusListCredential + "#" + statusIndex;

            CredentialStatusDto dto =
                    new CredentialStatusDto(
                            id, "revocation", statusIndex, statusListCredential, "BitstringStatusListEntry");

            assertDoesNotThrow(() -> revocationService.validateCredentialStatus(dto));
        }

        @Test
        @DisplayName("statusPurpose from dto is not matching the credential status list url")
        void invalidStatusPurpose_ThrowsIllegalArgumentException() {
            String statusIndex = "1";
            String invalidPurpose = "/break/";
            String statusListCredential =
                    "http://example.com/api/v1/revocations/credentials/" + BPN + invalidPurpose + "1";
            String id = statusListCredential + "#" + statusIndex;

            CredentialStatusDto dto =
                    new CredentialStatusDto(
                            id, "revocation", statusIndex, statusListCredential, "BitstringStatusListEntry");
            assertThrows(
                    IllegalArgumentException.class, () -> revocationService.validateCredentialStatus(dto));
        }

        @Test
        @DisplayName("id url from dto is not matching the credential status list url")
        void invalidId_ThrowsIllegalArgumentException() {
            String statusIndex = "1";
            String statusListCredential =
                    "http://example.com/api/v1/revocations/credentials/" + BPN + "/revocation/1";
            String id = statusListCredential.replace(BPN, "BPN0101010101010") + "#2";

            CredentialStatusDto dto =
                    new CredentialStatusDto(
                            id, "revocation", statusIndex, statusListCredential, "BitstringStatusListEntry");
            assertThrows(
                    IllegalArgumentException.class, () -> revocationService.validateCredentialStatus(dto));
        }

        @Test
        @DisplayName("credential status index is not matching the index in the url")
        void invalidStatusIndex_ThrowsIllegalArgumentException() {
            String statusIndex = "1";
            String statusListCredential =
                    "http://example.com/api/v1/revocations/credentials/" + BPN + "/revocation/1";
            String id = statusListCredential + "#2";

            CredentialStatusDto dto =
                    new CredentialStatusDto(
                            id, "revocation", statusIndex, statusListCredential, "BitstringStatusListEntry");
            assertThrows(
                    IllegalArgumentException.class, () -> revocationService.validateCredentialStatus(dto));
        }
    }
}
