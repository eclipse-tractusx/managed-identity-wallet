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

package org.eclipse.tractusx.managedidentitywallets.apidocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class SecureTokenControllerApiDoc {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SecurityRequirements
    @RequestBody(content = {
            @Content(examples = {
                    @ExampleObject(name = "Request Secure Token using Scopes", value = """
                             {
                               "audience": "BPNL000000000009",
                               "client_id": "your_client_id",
                               "client_secret": "your_client_secret",
                               "grant_type": "client_credentials",
                               "bearer_access_scope": "org.eclipse.tractusx.vc.type:ValidCredentialType:read"
                             }
                            """
                    ),
                    @ExampleObject(name = "Request Secure Token using Access Token", value = """
                             {
                               "audience": "BPNL000000000009",
                               "client_id": "your_client_id",
                               "client_secret": "your_client_secret",
                               "grant_type": "client_credentials",
                               "access_token": "a_jwt_token"
                             }
                            """
                    )
            })
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Success response", value = """
                                     {
                                        "token": "a_jwt_token",
                                        "expiresAt": 1706888709315
                                     }
                                    """
                            )
                    })
            }),

            @ApiResponse(responseCode = "400", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Unknown BPN", value = """
                                    {
                                      "error": "UnknownBusinessPartnerNumber",
                                      "errorDescription": "The provided BPN 'BPNL000000000001' is unknown"
                                    }
                                    """
                            ),

                            @ExampleObject(name = "Wrong Grant Type", value = """
                                    {
                                      "error": "UnsupportedGrantTypeException",
                                      "errorDescription": "The provided 'grant_type' is not valid. Use 'client_credentials'."
                                    }
                                    """
                            ),

                            @ExampleObject(name = "Invalid idp Token Response", value = """
                                    {
                                      "error": "InvalidIdpTokenResponse",
                                      "errorDescription": "The idp response cannot be null. Possible causes for this are: the 'clientId' is invalid, or the 'client' is not enabled."
                                    }
                                    """
                            ),

                            @ExampleObject(name = "Invalid Secure Token Request", value = """
                                    {
                                      "error": "InvalidSecureTokenRequest",
                                      "errorDescription": "The provided data could not be used to create and sign a token."
                                    }
                                    """
                            )
                    })
            }),

            @ApiResponse(responseCode = "500", description = "Any other internal server error", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Internal server error", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Error Title",
                                      "status": 500,
                                      "detail": "Error Details",
                                      "instance": "API endpoint",
                                      "properties": {
                                        "timestamp": 1689762476720
                                      }
                                    }
                                    """
                            )
                    })
            })
    })
    @Operation(summary = "Create and Sign Access Tokens", description = "The endpoint for creating and signing access tokens which are to be used during a verifiable presentation flow.")
    public @interface PostSecureTokenDoc {
    }
}
