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

package org.eclipse.tractusx.managedidentitywallets.controller;

import com.nimbusds.jwt.SignedJWT;
import io.swagger.v3.oas.annotations.Parameter;
import liquibase.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.managedidentitywallets.apidocs.PresentationControllerApiDocs.GetVerifiablePresentationIATPApiDocs;
import org.eclipse.tractusx.managedidentitywallets.apidocs.PresentationControllerApiDocs.PostVerifiablePresentationApiDocs;
import org.eclipse.tractusx.managedidentitywallets.apidocs.PresentationControllerApiDocs.PostVerifiablePresentationValidationApiDocs;
import org.eclipse.tractusx.managedidentitywallets.commons.utils.TokenParsingUtils;
import org.eclipse.tractusx.managedidentitywallets.constant.RestURI;
import org.eclipse.tractusx.managedidentitywallets.dto.PresentationResponseMessage;
import org.eclipse.tractusx.managedidentitywallets.reader.TractusXPresentationRequestReader;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.eclipse.tractusx.managedidentitywallets.service.STSTokenValidationService;
import org.eclipse.tractusx.ssi.lib.model.verifiable.presentation.VerifiablePresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.eclipse.tractusx.managedidentitywallets.commons.utils.TokenParsingUtils.getAccessToken;

/**
 * The type Presentation controller.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PresentationController {

    private final PresentationService presentationService;

    private final TractusXPresentationRequestReader presentationRequestReader;

    private final STSTokenValidationService validationService;

    /**
     * Create presentation response entity.
     *
     * @param data           the data
     * @param audience       the audience
     * @param asJwt          the as jwt
     * @param authentication the authentication
     * @return the response entity
     */
    @PostVerifiablePresentationApiDocs
    @PostMapping(path = RestURI.API_PRESENTATIONS, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createPresentation(@RequestBody Map<String, Object> data,
                                                                  @RequestParam(name = "audience", required = false) String audience,
                                                                  @RequestParam(name = "asJwt", required = false, defaultValue = "true") boolean asJwt, Authentication authentication
    ) {
        log.debug("Received request to create presentation. BPN: {}", TokenParsingUtils.getBPNFromToken(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(presentationService.createPresentation(data, asJwt, audience, TokenParsingUtils.getBPNFromToken(authentication)));
    }

    /**
     * Validate presentation response entity.
     *
     * @param data                     the data
     * @param audience                 the audience
     * @param asJwt                    the as jwt
     * @param withCredentialExpiryDate the with credential expiry date
     * @return the response entity
     */
    @PostVerifiablePresentationValidationApiDocs
    @PostMapping(path = RestURI.API_PRESENTATIONS_VALIDATION, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> validatePresentation(@RequestBody Map<String, Object> data,
                                                                    @Parameter(description = "Audience to validate in VP (Only supported in case of JWT formatted VP)") @RequestParam(name = "audience", required = false) String audience,
                                                                    @Parameter(description = "Pass true in case of VP is in JWT format") @RequestParam(name = "asJwt", required = false, defaultValue = "true") boolean asJwt,
                                                                    @Parameter(description = "Check expiry of VC(Only supported in case of JWT formatted VP)") @RequestParam(name = "withCredentialExpiryDate", required = false, defaultValue = "false") boolean withCredentialExpiryDate
    ) {
        log.debug("Received request to validate presentation");
        return ResponseEntity.status(HttpStatus.OK).body(presentationService.validatePresentation(data, asJwt, withCredentialExpiryDate, audience));
    }

    /**
     * Create presentation response entity for VC types provided in STS token.
     *
     * @param stsToken the STS token with required scopes
     * @param asJwt    as JWT VP response
     * @return the VP response entity
     */


    @PostMapping(path = { RestURI.API_PRESENTATIONS_IATP, RestURI.API_PRESENTATIONS_IATP_WORKAROUND }, produces = { MediaType.APPLICATION_JSON_VALUE })
    @GetVerifiablePresentationIATPApiDocs
    @SneakyThrows
    public ResponseEntity<PresentationResponseMessage> createPresentation(
            /* As filters are disabled for this endpoint set required to false and handle missing token manually */
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String stsToken,
            @RequestParam(name = "asJwt", required = false, defaultValue = "true") boolean asJwt,
            InputStream is) {
        try {

            if (stsToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (stsToken.startsWith("Bearer ")) {
                stsToken = stsToken.substring("Bearer ".length());
            }

            var validationResult = validationService.validateToken(stsToken);
            if (!validationResult.isValid()) {
                log.atDebug().log("Unauthorized request. Errors: '%s'".formatted(
                        StringUtil.join(validationResult.getErrors().stream()
                                        .map(Enum::name)
                                        .toList(),
                                ", ")));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // requested scopes are ignored for now
            final List<String> requestedScopes = presentationRequestReader.readVerifiableCredentialScopes(is);

            SignedJWT accessToken = getAccessToken(stsToken);

            if(asJwt) {
                Map<String, Object> map = presentationService.createVpWithRequiredScopes(accessToken, true);
                String verifiablePresentation = (String) map.get("vp");
                PresentationResponseMessage message = new PresentationResponseMessage(verifiablePresentation);
                return ResponseEntity.ok(message);
            } else {
                Map<String, Object> map = presentationService.createVpWithRequiredScopes(accessToken, false);
                VerifiablePresentation verifiablePresentation = new VerifiablePresentation((Map) map.get("vp"));
                PresentationResponseMessage message = new PresentationResponseMessage(verifiablePresentation);
                return ResponseEntity.ok(message);
            }

        } catch (TractusXPresentationRequestReader.InvalidPresentationQueryMessageResource e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
