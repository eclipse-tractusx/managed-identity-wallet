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

package org.eclipse.tractusx.managedidentitywallets.revocation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.RevocationPurpose;
import org.eclipse.tractusx.managedidentitywallets.commons.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.revocation.constant.RevocationApiEndpoints;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.CredentialStatusDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.dto.StatusEntryDto;
import org.eclipse.tractusx.managedidentitywallets.revocation.services.RevocationService;
import org.eclipse.tractusx.managedidentitywallets.revocation.utils.BitSetManager;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.BPN;
import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.DID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RevocationApiControllerTest {

    private static final String CALLER_BPN = UUID.randomUUID().toString();

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private RevocationService revocationService;

    @InjectMocks
    private RevocationApiController revocationApiController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(revocationApiController).build();
        objectMapper = new ObjectMapper();
        Mockito.reset(revocationService);
    }

    @Test
    void whenPostCreateStatusListVC_thenReturnStatus() throws Exception {
        // Given
        String validPurpose = RevocationPurpose.REVOCATION.name();
        StatusEntryDto statusEntryDto = new StatusEntryDto(validPurpose, DID);
        String validIndex =
                String.valueOf(BitSetManager.BITSET_SIZE / 2); // any valid index within range
        CredentialStatusDto credentialStatusDto =
                new CredentialStatusDto(
                        "https://example.com/revocations/credentials/" + BPN + "/revocation/1#" + validIndex,
                        RevocationPurpose.REVOCATION.name(),
                        validIndex, // this value is within the range [0, BitSetManager.BITSET_SIZE - 1]
                        "https://example.com/revocations/credentials/" + BPN + "/revocation/1",
                        "BitstringStatusListEntry");
        given(revocationService.createStatusList(statusEntryDto, "token"))
                .willReturn(credentialStatusDto);
        when(revocationService.extractBpnFromDid(DID)).thenReturn(BPN);

        Principal mockPrincipal = mockPrincipal(BPN);
        var name = mockPrincipal.getName();
        // When & Then
        mockMvc
                .perform(
                        MockMvcRequestBuilders.post(RevocationApiEndpoints.REVOCATION_API + RevocationApiEndpoints.STATUS_ENTRY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "token")
                                .principal(mockPrincipal)
                                .content(objectMapper.writeValueAsString(statusEntryDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(credentialStatusDto.id()));
    }

    private Principal mockPrincipal(String name) {

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(Map.of(StringPool.BPN, BPN));

        JwtAuthenticationToken principal = Mockito.mock(JwtAuthenticationToken.class);
        when(principal.getName()).thenReturn(name);
        when(principal.getPrincipal()).thenReturn(jwt);

        return principal;
    }

    @Test
    void whenPostRevokeCredential_thenReturnOkStatus() throws Exception {
        // Given
        String validIndex =
                String.valueOf(BitSetManager.BITSET_SIZE / 2); // any valid index within range
        CredentialStatusDto credentialStatusDto =
                new CredentialStatusDto(
                        "http://example.com/credentials/" + BPN + "/revocation/1#" + validIndex,
                        "revocation",
                        validIndex, // this value is within the range [0, BitSetManager.BITSET_SIZE - 1]
                        "http://example.com/credentials/" + BPN + "/revocation/1",
                        "BitstringStatusListEntry");
        doNothing().when(revocationService).revoke(credentialStatusDto, "token");
        when(revocationService.extractBpnFromURL(any())).thenReturn(BPN);

        Principal mockPrincipal = mockPrincipal(BPN);
        // When & Then
        mockMvc
                .perform(
                        MockMvcRequestBuilders.post(RevocationApiEndpoints.REVOCATION_API + RevocationApiEndpoints.REVOKE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "token")
                                .principal(mockPrincipal)
                                .content(objectMapper.writeValueAsString(credentialStatusDto)))
                .andExpect(status().isOk());
        verify(revocationService).revoke(credentialStatusDto, "token");
    }

    @Test
    void whenGetCredential_thenReturnCredentials() throws Exception {
        // Given
        String validPurpose = RevocationPurpose.REVOCATION.name();
        VerifiableCredential verifiableCredential =
                new VerifiableCredential(
                        createVerifiableCredentialTestData()); // Populate with valid test data
        given(revocationService.getStatusListCredential(any(), any(), any()))
                .willReturn(verifiableCredential);
        // When & Then
        mockMvc
                .perform(
                        MockMvcRequestBuilders.get(
                                RevocationApiEndpoints.REVOCATION_API
                                        + RevocationApiEndpoints.CREDENTIALS_STATUS_INDEX
                                        .replace("{issuerBPN}", BPN)
                                        .replace("{status}", validPurpose)
                                        .replace("{index}", "1")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(verifiableCredential.getId().toString()));
    }

    private VerifiableCredential createVerifiableCredentialTestData() {
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put(
                "id", "http://example/api/v1/revocations/credentials/" + BPN + "/revocation/1");
        credentialData.put("issuer", "https://issuer.example.com");
        credentialData.put("issuanceDate", Instant.now().toString());
        // Include 'type' field as a list because VerifiableCredential expects it to be non-null and a
        // list
        credentialData.put("type", List.of("VerifiableCredential", "StatusListCredential"));
        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("id", "subjectId");
        subjectData.put("type", "StatusList2021Credential");
        // 'credentialSubject' can be either a List or a single Map according to the code, so I'm
        // keeping it as a single Map
        credentialData.put("credentialSubject", subjectData);
        credentialData.put("@context", VerifiableCredential.DEFAULT_CONTEXT.toString());
        VerifiableCredential credential = new VerifiableCredential(credentialData);
        return credential;
    }

    private VerifiableCredential createVerifiableCredentialTestDataInvalidDID() {
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("id", UUID.randomUUID().toString());
        credentialData.put("issuer", "https://issuer.example.com");
        credentialData.put("issuanceDate", Instant.now().toString());
        // Include 'type' field as a list because VerifiableCredential expects it to be non-null and a
        // list
        credentialData.put("type", List.of("VerifiableCredential", "StatusListCredential"));
        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("id", "subjectId");
        subjectData.put("type", "StatusList2021Credential");
        // 'credentialSubject' can be either a List or a single Map according to the code, so I'm
        // keeping it as a single Map
        credentialData.put("credentialSubject", subjectData);
        credentialData.put("@context", VerifiableCredential.DEFAULT_CONTEXT.toString());
        VerifiableCredential credential = new VerifiableCredential(credentialData);
        return credential;
    }
}
