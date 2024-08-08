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

package org.eclipse.tractusx.managedidentitywallets.service;

import lombok.Getter;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.ssi.lib.did.resolver.CompositeDidResolver;
import org.eclipse.tractusx.ssi.lib.did.web.DidWebResolver;
import org.eclipse.tractusx.ssi.lib.did.web.util.DidWebParser;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;

@Service
public class DidDocumentResolverService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Getter
    private final DidWebResolver didDocumentResolverRegistry;

    @Getter
    private final CompositeDidResolver compositeDidResolver;

    public DidDocumentResolverService(MIWSettings miwSettings) {

        final boolean enforceHttps = miwSettings.enforceHttps();
        final DidWebParser didParser = new DidWebParser();

        didDocumentResolverRegistry =
                new DidWebResolver(HTTP_CLIENT, didParser, enforceHttps);

        compositeDidResolver = new CompositeDidResolver(
                new DidWebResolver(HTTP_CLIENT, didParser, enforceHttps)
        );
    }
}
