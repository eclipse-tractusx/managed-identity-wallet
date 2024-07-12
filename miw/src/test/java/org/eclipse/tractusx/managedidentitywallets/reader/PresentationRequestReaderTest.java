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

import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.utils.ResourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

public class PresentationRequestReaderTest {

    private final TractusXPresentationRequestReader presentationRequestReader = new TractusXPresentationRequestReader();

    @Test
    @SneakyThrows
    public void readCredentialsTest() {

        final InputStream is = ResourceUtil.getResourceStream("identityminustrust/messages/presentation_query.json");

        final List<String> credentialScopes = presentationRequestReader.readVerifiableCredentialScopes(is);

        final String expected = "org.eclipse.tractusx.vc.type:MembershipCredential:read";

        System.out.printf("Found credentials: %s", credentialScopes.toString());
        Assertions.assertTrue(credentialScopes.contains(expected), "Expected %s".formatted(expected));
    }
}
