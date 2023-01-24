/********************************************************************************
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.managedidentitywallets

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.eclipse.tractusx.managedidentitywallets.models.WalletDto
import org.eclipse.tractusx.managedidentitywallets.services.IBusinessPartnerDataService

class BusinessPartnerDataMockedService: IBusinessPartnerDataService {

    override suspend fun pullDataAndUpdateBaseWalletCredentialsAsync(
        identifier: String?
    ): Deferred<Boolean> {
        return CompletableDeferred(true)
    }

    override suspend fun issueAndStoreBaseWalletCredentialsAsync(
        walletHolderDto: WalletDto,
        type: String,
        data: Any?
    ): Deferred<Boolean> {
        return CompletableDeferred(true)
    }

    override suspend fun issueAndSendBaseWalletCredentialsForSelfManagedWalletsAsync(
        targetWallet: WalletDto,
        connectionId: String,
        webhookUrl: String?,
        type: String,
        data: Any?
    ): Deferred<Boolean> {
        return CompletableDeferred(true)
    }

}
