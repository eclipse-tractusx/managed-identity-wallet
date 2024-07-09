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

package org.eclipse.tractusx.managedidentitywallets.reader;

import com.apicatalog.jsonld.JsonLdError;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class TractusXPresentationRequestReader extends TractusXJsonLdReader {

    private static final String JSON_LD_TYPE = "@type";
    private static final String JSON_LD_VALUE = "@value";
    private static final String TRACTUS_X_PRESENTATION_QUERY_MESSAGE_TYPE = "https://w3id.org/tractusx-trust/v0.8/PresentationQueryMessage";
    private static final String TRACTUS_X_SCOPE_TYPE = "https://w3id.org/tractusx-trust/v0.8/scope";

    public List<String> readVerifiableCredentialScopes(InputStream is) throws InvalidPresentationQueryMessageResource {
        try {

            final JsonArray jsonArray = expand(is);

            if (jsonArray.size() != 1) {
                log.atDebug().addArgument(jsonArray::toString).log("Expanded JSON-LD: {}");
                throw new InvalidPresentationQueryMessageResource("Expected a single JSON object. Found %d".formatted(jsonArray.size()));
            }

            var jsonObject = jsonArray.getJsonObject(0);

            final JsonArray typeArray = jsonObject.getJsonArray(JSON_LD_TYPE);
            final List<String> types = typeArray.getValuesAs(JsonString.class).stream().map(JsonString::getString).toList();
            if (!types.contains(TRACTUS_X_PRESENTATION_QUERY_MESSAGE_TYPE)) {
                log.atDebug().addArgument(jsonArray::toString).log("Expanded JSON-LD: {}");
                throw new InvalidPresentationQueryMessageResource("Unexpected type. Expected %s".formatted(TRACTUS_X_PRESENTATION_QUERY_MESSAGE_TYPE));
            }

            final JsonArray scopes = jsonObject.getJsonArray(TRACTUS_X_SCOPE_TYPE);
            return scopes.getValuesAs(JsonObject.class)
                    .stream()
                    .map(o -> o.getJsonString(JSON_LD_VALUE))
                    .map(JsonString::getString)
                    .toList();

        } catch (JsonLdError e) {
            throw new InvalidPresentationQueryMessageResource(e);
        }
    }

    public static class InvalidPresentationQueryMessageResource extends Exception {
        public InvalidPresentationQueryMessageResource(String message) {
            super(message);
        }

        public InvalidPresentationQueryMessageResource(Throwable cause) {
            super(cause);
        }
    }

}

