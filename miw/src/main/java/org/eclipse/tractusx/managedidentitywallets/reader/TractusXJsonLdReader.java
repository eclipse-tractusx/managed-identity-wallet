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
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.processor.ExpansionProcessor;
import jakarta.json.JsonArray;
import lombok.NonNull;
import org.eclipse.tractusx.managedidentitywallets.utils.ResourceUtil;
import org.eclipse.tractusx.ssi.lib.model.RemoteDocumentLoader;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public class TractusXJsonLdReader {

    private static final String TRACTUS_X_CONTEXT_RESOURCE = "jsonld/IdentityMinusTrust.json";
    private static final URI TRACTUS_X_CONTEXT = URI.create("https://w3id.org/tractusx-trust/v0.8");

    private final RemoteDocumentLoader documentLoader = RemoteDocumentLoader.DOCUMENT_LOADER;

    public TractusXJsonLdReader() {

        documentLoader.setEnableHttps(true); // HTTPS context loading is required

        final boolean isOfflineContextCached = documentLoader.getLocalCache().containsKey(TRACTUS_X_CONTEXT);
        if (!isOfflineContextCached) {
            loadOfflineContext();
        }
    }

    public JsonArray expand(@NonNull final InputStream documentStream) throws JsonLdError {

        final JsonLdOptions jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setDocumentLoader(documentLoader);

        final JsonDocument document = JsonDocument.of(com.apicatalog.jsonld.http.media.MediaType.JSON_LD, documentStream);
        return ExpansionProcessor.expand(document, jsonLdOptions, false);
    }

    private void loadOfflineContext() {
        try {
            final InputStream context = ResourceUtil.getResourceStream(TRACTUS_X_CONTEXT_RESOURCE);
            final JsonDocument identityMinusTrustDocument;
            identityMinusTrustDocument = JsonDocument.of(com.apicatalog.jsonld.http.media.MediaType.JSON_LD, context);
            documentLoader.setEnableLocalCache(true);
            documentLoader.setLocalCache(Map.of(TRACTUS_X_CONTEXT, identityMinusTrustDocument));
        } catch (JsonLdError e) {
            // If this ever fails, it is a programming error. Loading of the embedded context resource is checked by Unit Tests.
            throw new RuntimeException("Could not parse Tractus-X JsonL-d context from resource. This should never happen. Resource: '%s'".formatted(TRACTUS_X_CONTEXT_RESOURCE), e);
        }
    }
}
