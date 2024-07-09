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

package org.eclipse.tractusx.managedidentitywallets.dto;

import com.github.tomakehurst.wiremock.common.StreamSources;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.reader.TractusXJsonLdReader;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * This test verifies that the serialized output of the Presentation Response DTO is JsonLD and Tractus-X compliant.
 * <p>
 * It does so by comparing the serialized output with a predefined expected output. Like a contract test.
 * </p>
 */
public class PresentationResponseSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* Please note: The order of the properties is important, as the Unit Tests does some String comparison. */
    final String Presentation = """
            {
              "id": "urn:uuid:3978344f-8596-4c3a-a978-8fcaba3903c5",
              "type": ["VerifiablePresentation", "ExamplePresentation"],
              "@context": [
                "https://www.w3.org/ns/credentials/v2"
              ],
              "verifiableCredential": [{
                "@context": "https://www.w3.org/ns/credentials/v2",
                "id": "data:application/vc+sd-jwt;QzVjV...RMjU",
                "issuer": "did:example:123",
                "issuanceDate": "2020-03-10T04:24:12.164Z",
                "type": "VerifiableCredential",
                "credentialSubject": {
                  "id": "did:example:456",
                  "degree": {
                    "type": "BachelorDegree",
                    "name": "Bachelor of Science and Arts"
                  }
                }
              }]
            }""";

    /* Please note: The order of the properties is important, as the Unit Tests does some String comparison. */
    final String ExpectedPresentationResponse = "{\n" +
            "  \"@context\" : [\n" +
            "    \"https://w3id.org/tractusx-trust/v0.8\"\n" +
            "  ],\n" +
            "  \"@type\" : [\n" +
            "    \"PresentationResponseMessage\"\n" +
            "  ],\n" +
            "  \"presentation\" : [\n" +
            Presentation +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    @Test
    @SneakyThrows
    public void testPresentationResponseSerialization() {
        var presentation = getPresentation();

        var response = new PresentationResponseMessage(presentation);

        var serialized = MAPPER.writeValueAsString(response);


        var serializedDocument = new StreamSources.StringInputStreamSource(serialized, StandardCharsets.UTF_8).getStream();
        var expectedDocument = new StreamSources.StringInputStreamSource(ExpectedPresentationResponse, StandardCharsets.UTF_8).getStream();

        var reader = new TractusXJsonLdReader();
        var normalizedSerializedDocument = reader.expand(serializedDocument).toString();
        var normalizedExpectedDocument = reader.expand(expectedDocument).toString();


        var isEqual = normalizedSerializedDocument.equals(normalizedExpectedDocument);

        Assertions.assertTrue(isEqual, "Expected both documents to be equal.\n%s\n%s".formatted(normalizedSerializedDocument, normalizedExpectedDocument));
    }

    @SneakyThrows
    private VerifiablePresentation getPresentation() {
        var map = MAPPER.readValue(Presentation, Map.class);
        return new VerifiablePresentation(map);
    }
}
