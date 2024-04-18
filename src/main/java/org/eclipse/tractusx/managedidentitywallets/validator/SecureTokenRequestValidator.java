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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static org.eclipse.tractusx.managedidentitywallets.utils.CommonUtils.getSecureTokenRequest;

public class SecureTokenRequestValidator implements Validator {

    public static final String LINKED_MULTI_VALUE_MAP_CLASS_NAME = "org.springframework.util.LinkedMultiValueMap";

    @Override
    public boolean supports(Class<?> clazz) {
        return SecureTokenRequest.class.equals(clazz) || clazz.getCanonicalName().equals(LINKED_MULTI_VALUE_MAP_CLASS_NAME);
    }

    @Override
    public void validate(Object target, Errors errors) {
        LinkedMultiValueMap<String, String> requestParams = null;
        if (target instanceof LinkedMultiValueMap) {
            requestParams = (LinkedMultiValueMap) target;
        }
        SecureTokenRequest secureTokenRequest = requestParams != null ? getSecureTokenRequest(requestParams) : (SecureTokenRequest) target;
        Errors errorsHandled = new BeanPropertyBindingResult(secureTokenRequest, errors.getObjectName());
        ValidationUtils.rejectIfEmptyOrWhitespace(errorsHandled, "audience", "audience.empty", "The 'audience' cannot be empty or missing.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errorsHandled, "clientId", "client_id.empty", "The 'client_id' cannot be empty or missing.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errorsHandled, "clientSecret", "client_secret.empty", "The 'client_secret' cannot be empty or missing.");
        ValidationUtils.rejectIfEmptyOrWhitespace(errorsHandled, "grantType", "grant_type.empty", "The 'grant_type' cannot be empty or missing.");

        if (secureTokenRequest.getAccessToken() != null && secureTokenRequest.getBearerAccessScope() != null) {
            errorsHandled.rejectValue("accessToken", "access_token.incompatible", "The 'access_token' and the 'bearer_access_token' cannot be set together.");
            errorsHandled.rejectValue("bearerAccessScope", "bearer_access_scope.incompatible", "The 'access_token' and the 'bearer_access_token' cannot be set together.");
        }
        if (secureTokenRequest.getAccessToken() == null && secureTokenRequest.getBearerAccessScope() == null) {
            errorsHandled.rejectValue("accessToken", "access_token.incompatible", "Both the 'access_token' and the 'bearer_access_scope' are missing. At least one must be set.");
            errorsHandled.rejectValue("bearerAccessScope", "bearer_access_scope.incompatible", "Both the 'access_token' and the 'bearer_access_scope' are missing. At least one must be set.");
        }
        if (secureTokenRequest.getAccessToken() != null) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errorsHandled, "accessToken", "access_token.empty", "The 'access_token' cannot be empty or missing.");
        }
        if (secureTokenRequest.getBearerAccessScope() != null) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errorsHandled, "bearerAccessScope", "bearer_access_scope.empty", "The 'bearer_access_scope' cannot be empty or missing.");
        }
    }
}
