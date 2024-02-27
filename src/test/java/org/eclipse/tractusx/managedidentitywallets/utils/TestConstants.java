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

package org.eclipse.tractusx.managedidentitywallets.utils;

public class TestConstants {

    public static final String DID_BPN_1 = "did:web:localhost:BPNL000000000001";
    public static final String DID_BPN_2 = "did:web:localhost:BPNL000000000002";
    public static final String BPN_1 = "BPNL000000000001";
    public static final String BPN_2 = "BPNL000000000002";
    public static final String DID_JSON_STRING_1 = """
            {
              "@context": [
                "https://www.w3.org/ns/did/v1",
                "https://w3c.github.io/vc-jws-2020/contexts/v1"
              ],
              "id": "did:web:localhost:BPNL000000000001",
              "verificationMethod": [
                {
                  "publicKeyJwk": {
                    "kty": "OKP",
                    "crv": "Ed25519",
                    "x": "4Q5HCXPyutfcj7gLmbAKlYttlJPkykIkRjh7DH2NtZ0"
                  },
                  "controller": "did:web:localhost:BPNL000000000001",
                  "id": "did:web:localhost:BPNL000000000001#58cb4b32-c2e4-46f0-a3ad-3286e34765ed",
                  "type": "JsonWebKey2020"
                }
              ]
            }
            """;
    public static final String DID_JSON_STRING_2 = """
            {
              "@context": [
                "https://www.w3.org/ns/did/v1",
                "https://w3c.github.io/vc-jws-2020/contexts/v1"
              ],
              "id": "did:web:localhost:BPNL000000000002",
              "verificationMethod": [
                {
                  "publicKeyJwk": {
                    "kty": "OKP",
                    "crv": "Ed25519",
                    "x": "Z-8DEkN6pw2E01niDWqrp1kROLF-syIPIpFgmyrVUOU"
                  },
                  "controller": "did:web:localhost:BPNL000000000002",
                  "id": "did:web:localhost:BPNL000000000001#58cb4b32-c2e4-46f0-a3ad-3286e34765ty",
                  "type": "JsonWebKey2020"
                }
              ]
            }
            """;
}
