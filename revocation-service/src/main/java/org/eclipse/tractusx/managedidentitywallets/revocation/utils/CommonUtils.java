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

package org.eclipse.tractusx.managedidentitywallets.revocation.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class CommonUtils {

    /**
     * Extracts the BPN number, purpose and credential index from the URL
     *
     * @param url the URL to extract the values from
     * @return an array containing the BPN number [0], purpose [1] and credential index [2]
     */
    public String[] extractValuesFromURL(String url) {
        // Define a regular expression pattern to match the desired parts
        Pattern pattern =
                Pattern.compile("/credentials/(B\\w+)/(.*?)/(\\d+)", Pattern.CASE_INSENSITIVE);
        // Create a Matcher object and find the first match in the URL
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String bpnlNumber = matcher.group(1);
            String purpose = matcher.group(2);
            String credentialIndex = matcher.group(3);
            return new String[]{ bpnlNumber.toUpperCase(), purpose, credentialIndex };
        } else {
            throw new IllegalArgumentException("No match found");
        }
    }
}
