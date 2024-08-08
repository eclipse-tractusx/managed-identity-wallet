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

package org.eclipse.tractusx.managedidentitywallets.revocation.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MIWSettingsTest {

    @Test
    void testMIWSettingsWithValidData() {
        List<URI> vcContexts =
                Arrays.asList(
                        URI.create("https://example.com/context1"), URI.create("https://example.com/context2"));

        MIWSettings miwSettings = new MIWSettings(vcContexts);

        assertEquals(vcContexts, miwSettings.vcContexts());
    }

    @Test
    void testMIWSettingsWithNullVCContexts() {
        assertThrows(NullPointerException.class, () -> new MIWSettings(null));
    }

    @Test
    void testMIWSettingsWithEmptyVCContexts() {
        assertThrows(IllegalArgumentException.class, () -> new MIWSettings(List.of()));
    }
}
