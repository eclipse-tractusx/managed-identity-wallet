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

package org.eclipse.tractusx.managedidentitywallets.commons;

import org.eclipse.tractusx.managedidentitywallets.commons.utils.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateTest {


    @Test
    void validateTest() {
        RuntimeException runtimeException = new RuntimeException();
        Assertions.assertThrows(RuntimeException.class, () -> Validate.isFalse(false).launch(runtimeException));

        Assertions.assertThrows(RuntimeException.class, () -> Validate.isTrue(true).launch(runtimeException));

        Assertions.assertThrows(RuntimeException.class, () -> Validate.isNull(null).launch(runtimeException));

        Assertions.assertThrows(RuntimeException.class, () -> Validate.isNotNull("Test").launch(runtimeException));

        Assertions.assertThrows(RuntimeException.class, () -> Validate.value("").isNotEmpty().launch(runtimeException));

        Assertions.assertDoesNotThrow(() -> Validate.isFalse(true).launch(runtimeException));

        Assertions.assertDoesNotThrow(() -> Validate.isTrue(false).launch(runtimeException));

        Assertions.assertDoesNotThrow(() -> Validate.isNull("").launch(runtimeException));

        Assertions.assertDoesNotThrow(() -> Validate.isNotNull(null).launch(runtimeException));

        Assertions.assertDoesNotThrow(() -> Validate.value("Test").isNotEmpty().launch(runtimeException));

    }
}
