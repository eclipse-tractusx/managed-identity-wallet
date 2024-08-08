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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BPN {

    private static final String PATTERN = "^(BPN)([LSA])[0-9A-Z]{12}";

    private final String value;

    public BPN(final String bpn) {

        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(bpn);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("BPN %s is not valid".formatted(bpn));
        }

        this.value = bpn;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final BPN bpn = (BPN) o;

        return value.equals(bpn.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
