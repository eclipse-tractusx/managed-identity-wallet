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

package org.eclipse.tractusx.managedidentitywallets.constant;

/**
 * The type Application constant.
 */
public class ApplicationConstant {

    private ApplicationConstant() {
        throw new IllegalStateException("Constant class");
    }

    /**
     * The constant ROLE_VIEW_WALLETS.
     */
    public static final String ROLE_VIEW_WALLETS = "view_wallets";
    /**
     * The constant ROLE_VIEW_WALLET.
     */
    public static final String ROLE_VIEW_WALLET = "view_wallet";

    /**
     * The constant ROLE_ADD_WALLETS.
     */
    public static final String ROLE_ADD_WALLETS = "add_wallets";

    /**
     * The constant ROLE_UPDATE_WALLETS.
     */
    public static final String ROLE_UPDATE_WALLETS = "update_wallets";

    /**
     * The constant ROLE_UPDATE_WALLET.
     */
    public static final String ROLE_UPDATE_WALLET = "update_wallet";


}
