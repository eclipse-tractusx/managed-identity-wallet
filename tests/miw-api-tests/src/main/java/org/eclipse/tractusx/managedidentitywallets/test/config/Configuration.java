/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.managedidentitywallets.test.config;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * The config which read config from env
 *
 * @author Nitin
 * @since 01 /24/2022
 */
@Slf4j
public class Configuration {


    private static final String ENV_MIW_HOST = "MIW_HOST";

    private static final String ENV_AUTH_SERVER = "AUTH_SERVER_URL";

    private static final String ENV_BASE_WALLET_CLIENT_ID = "BASE_WALLET_CLIENT_ID";

    private static final String ENV_BASE_WALLET_CLIENT_SECRET = "BASE_WALLET_CLIENT_SECRET";

    private static final String ENV_BASE_WALLET_BPN = "BASE_WALLET_BPN";

    private static final String ENV_USER_WALLET_CLIENT_ID = "USER_WALLET_CLIENT_ID";

    private static final String ENV_USER_WALLET_CLIENT_SECRET = "USER_WALLET_CLIENT_SECRET";

    private static final String ENV_USER_WALLET_BPN = "USER_WALLET_BPN";

    private static final String ENV_REALM = "REALM";

    private static Configuration configuration;

    private final Map<String, String> env;

    private Configuration() {
        env = System.getenv();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        if (configuration == null) {
            configuration = new Configuration();
        }
        return configuration;
    }

    private Map<String, String> getEnv() {
        return env;
    }


    public static String getMIWHost() {
        return getInstance().getEnv().get(ENV_MIW_HOST);
    }

    public static String getAuthServer() {
        return getInstance().getEnv().get(ENV_AUTH_SERVER);
    }

    public static String getBaseWalletClientId() {
        return getInstance().getEnv().get(ENV_BASE_WALLET_CLIENT_ID);
    }

    public static String getBaseWalletClientSecret() {
        return getInstance().getEnv().get(ENV_BASE_WALLET_CLIENT_SECRET);
    }

    public static String getBaseWalletBPN() {
        return getInstance().getEnv().get(ENV_BASE_WALLET_BPN);
    }

    public static String getUserWalletClientId() {
        return getInstance().getEnv().get(ENV_USER_WALLET_CLIENT_ID);

    }

    public static String getUserWalletClientSecret() {
        return getInstance().getEnv().get(ENV_USER_WALLET_CLIENT_SECRET);
    }

    public static String getUserWalletBPN() {
        return getInstance().getEnv().get(ENV_USER_WALLET_BPN);
    }

    public static String getRealm() {
        return getInstance().getEnv().get(ENV_REALM);
    }
}
