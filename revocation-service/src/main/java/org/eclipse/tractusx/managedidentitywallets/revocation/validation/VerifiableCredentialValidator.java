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

package org.eclipse.tractusx.managedidentitywallets.revocation.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.NonNull;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class VerifiableCredentialValidator
        implements ConstraintValidator<ValidVerifiableCredential, VerifiableCredential> {

    @Override
    public void initialize(ValidVerifiableCredential constraintAnnotation) {
        // You can add initialization logic here if needed.
    }

    @Override
    public boolean isValid(VerifiableCredential credential, ConstraintValidatorContext context) {
        if (credential == null) {
            return true; // @NotNull should handle null cases
        }

        // Assuming 'id' within 'credentialSubject' is the field to be validated as a URI
        @NonNull
        List<VerifiableCredentialSubject> credentialSubject = credential.getCredentialSubject();
        return validateCredentialSubject(credentialSubject, context);

        // Additional validation checks can be added here, e.g., checking the proof object
    }

    private boolean validateCredentialSubject(
            List<VerifiableCredentialSubject> credentialSubjects, ConstraintValidatorContext context) {
        // We iterate over the list of credential subjects to validate each one
        for (Map<String, Object> subject : credentialSubjects) {
            // Extract the 'id' of the credential subject if it exists
            Object subjectId = subject.get("id");
            if (subjectId == null || !(subjectId instanceof String)) {
                addConstraintViolation(context, "credentialSubject.id must be a valid String");
                return false;
            }

            // Check for a valid URI in the subject ID
            if (!isValidUri((String) subjectId)) {
                addConstraintViolation(context, "credentialSubject.id must be a valid URI");
                return false;
            }
        }
        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private boolean isValidUri(String uriStr) {
        try {
            URI uri = new URI(uriStr);
            return uri.isAbsolute();
        } catch (Exception e) {
            return false;
        }
    }
}
