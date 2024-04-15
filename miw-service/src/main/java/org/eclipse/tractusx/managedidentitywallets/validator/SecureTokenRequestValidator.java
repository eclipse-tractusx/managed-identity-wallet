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

package org.eclipse.tractusx.managedidentitywallets.validator;

import org.eclipse.tractusx.managedidentitywallets.dto.SecureTokenRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.ACCESS_SCOPE;
import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.ACCESS_TOKEN;


public class SecureTokenRequestValidator  implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return SecureTokenRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "audience", "audience.empty", "The 'audience' cannot be empty or missing.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "clientId", "client_id.empty", "The 'client_id' cannot be empty or missing.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "clientSecret", "client_secret.empty", "The 'client_secret' cannot be empty or missing.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "grantType", "grant_type.empty", "The 'grant_type' cannot be empty or missing.");
        SecureTokenRequest secureTokenRequest = (SecureTokenRequest) target;
        if (secureTokenRequest.getAccessToken() != null && secureTokenRequest.getBearerAccessScope() != null) {
            errors.rejectValue(ACCESS_TOKEN, "access_token.incompatible", "The 'access_token' and the 'bearer_access_token' cannot be set together.");
            errors.rejectValue(ACCESS_SCOPE, "bearer_access_scope.incompatible", "The 'access_token' and the 'bearer_access_token' cannot be set together.");
        }
        if (secureTokenRequest.getAccessToken() == null && secureTokenRequest.getBearerAccessScope() == null) {
            errors.rejectValue(ACCESS_TOKEN, "access_token.incompatible", "Both the 'access_token' and the 'bearer_access_scope' are missing. At least one must be set.");
            errors.rejectValue(ACCESS_SCOPE, "bearer_access_scope.incompatible", "Both the 'access_token' and the 'bearer_access_scope' are missing. At least one must be set.");
        }
        if (secureTokenRequest.getAccessToken() != null) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "accessToken", "access_token.empty", "The 'access_token' cannot be empty or missing.");
        }
        if (secureTokenRequest.getBearerAccessScope() != null) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "bearerAccessScope", "bearer_access_scope.empty", "The 'bearer_access_scope' cannot be empty or missing.");
        }
    }
}
