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

package org.eclipse.tractusx.managedidentitywallets.revocation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.eclipse.tractusx.managedidentitywallets.revocation.TestUtil.BPN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BPNTest {

    @Test
    @DisplayName("BPN Should not be valid")
    public void invalidBPN() {
        String bpn = "thisnotbpn";

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new BPN(bpn);
                });
    }

    @Test
    @DisplayName("BPN Should be valid")
    public void validBPN() {
        assertDoesNotThrow(
                () -> {
                    new BPN(BPN);
                });
    }

    @Test
    @DisplayName("BPN Should return value")
    public void bpnValue() {
        BPN bpn = new BPN(BPN);
        assertEquals(BPN, bpn.value());
    }

    @Test
    @DisplayName("BPN Should be equal")
    public void bpnEqual() {
        BPN bpn1 = new BPN(BPN);
        BPN bpn2 = new BPN(BPN);
        assertTrue(bpn1.equals(bpn2));
    }

    @Test
    @DisplayName("BPN Should not be equal")
    public void bpnNotEqual() {
        BPN bpn1 = new BPN(BPN);
        BPN bpn2 = new BPN("BPNL000000000000");
        assertFalse(bpn1.equals(bpn2));
    }
}
