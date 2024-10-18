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

package org.eclipse.tractusx.managedidentitywallets.domain;

import org.eclipse.tractusx.managedidentitywallets.commons.constant.SupportedAlgorithms;
import org.eclipse.tractusx.ssi.lib.model.did.Did;
import org.eclipse.tractusx.ssi.lib.model.did.DidDocument;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialStatus;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialCreationConfigTest {

    @Test
    void shouldBuildWithRequiredAttributes() {
        CredentialCreationConfig build = assertDoesNotThrow(() -> CredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .expiryDate(new Date())
                .subject(mockCredentialSubject())
                .types(Collections.emptyList())
                .issuerDoc(Mockito.mock(DidDocument.class))
                .holderDid(Mockito.mock(Did.class).toString())
                .contexts(Collections.emptyList())
                .verifiableCredentialStatus(Mockito.mock(VerifiableCredentialStatus.class))
                .vcId(URI.create("yada://test.com"))
                .keyName("keyName")
                .algorithm(SupportedAlgorithms.ED25519)
                .build());
        assertNotNull(build);
        assertNotNull(build.getExpiryDate());
        assertNotNull(build.getSubject());
        assertNotNull(build.getTypes());
        assertNotNull(build.getIssuerDoc());
        assertNotNull(build.getHolderDid());
        assertNotNull(build.getContexts());
        assertNotNull(build.getVerifiableCredentialStatus());
        assertNotNull(build.getVcId());
        assertNotNull(build.getKeyName());
        assertNotNull(build.getEncoding());
        assertFalse(build.isSelfIssued());
    }

    @Test
    void shouldBuildWhenVcIdIsString() {

        CredentialCreationConfig build = assertDoesNotThrow(() -> CredentialCreationConfig.builder()
                .encoding(VerifiableEncoding.JSON_LD)
                .expiryDate(new Date())
                .subject(mockCredentialSubject())
                .types(Collections.emptyList())
                .issuerDoc(Mockito.mock(DidDocument.class))
                .holderDid(Mockito.mock(Did.class).toString())
                .contexts(Collections.emptyList())
                .verifiableCredentialStatus(Mockito.mock(VerifiableCredentialStatus.class))
                .keyName("keyName")
                .algorithm(SupportedAlgorithms.ED25519)
                .build());
    }

    @ParameterizedTest
    @MethodSource("testConfigs")
    void shouldThrowIfRequiredAttributesMissing(TestConfig conf) {
        assertThrows(NullPointerException.class, () -> CredentialCreationConfig
                .builder().expiryDate(conf.expiryDate)
                .subject(conf.subject)
                .types(conf.types)
                .issuerDoc(conf.issuerDoc)
                .holderDid(conf.holderDid)
                .contexts(conf.contexts)
                .encoding(conf.encoding)
                .keyName(conf.keyName));


    }

    @Test
    void shouldThrowWhenSettingIllegalVcId() {
        CredentialCreationConfig.CredentialCreationConfigBuilder builder = CredentialCreationConfig.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.vcId(42));
    }

    @Test
    void shouldNotThrowWhenVcIdValid() {
        CredentialCreationConfig.CredentialCreationConfigBuilder builder = CredentialCreationConfig.builder();
        assertDoesNotThrow(() -> builder.vcId("https://test.com"));
        assertDoesNotThrow(() -> builder.vcId(URI.create("https://test.com")));
    }

    private static Stream<Arguments> testConfigs() {
        return Stream.of(
                Arguments.of(new TestConfig(null, null, null, null, null, null, null, null, null)),
                Arguments.of(new TestConfig(mockCredentialSubject(), null, null, null, null, null, null, null, null)),
                Arguments.of(new TestConfig(mockCredentialSubject(), Collections.emptyList(), null, null, null, null, null, null, null)),
                Arguments.of(new TestConfig(mockCredentialSubject(), Collections.emptyList(), new byte[]{}, null, null, null, null, null, null)),
                Arguments.of(new TestConfig(mockCredentialSubject(), Collections.emptyList(), new byte[]{}, Mockito.mock(DidDocument.class), null, null, null, null, null)),
                Arguments.of(new TestConfig(mockCredentialSubject(), Collections.emptyList(), new byte[]{}, Mockito.mock(DidDocument.class), Mockito.mock(Did.class).toString(), null, null, null, null)),
                Arguments.of(new TestConfig(mockCredentialSubject(), Collections.emptyList(), new byte[]{}, Mockito.mock(DidDocument.class), Mockito.mock(Did.class).toString(), Collections.emptyList(), null, null, null))
        );
    }

    private record TestConfig(
            VerifiableCredentialSubject subject,
            List<String> types,
            byte[] privateKey,
            DidDocument issuerDoc,
            String holderDid,
            List<URI> contexts,
            Date expiryDate,

            String keyName,

            VerifiableEncoding encoding

    ) {
    }


    private static VerifiableCredentialSubject mockCredentialSubject() {
        return new VerifiableCredentialSubject(Map.of("id", "42"));
    }

}
