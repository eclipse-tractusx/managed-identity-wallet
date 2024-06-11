package org.eclipse.tractusx.managedidentitywallets.apidocs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

public class DidDocumentControllerApiDocs {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
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
                                    """)
                    }) }),
            @ApiResponse(responseCode = "404", description = "Wallet not found with provided bpn", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Wallet not found with provided bpn", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Wallet not found for identifier BPNL00000000000",
                                      "status": 404,
                                      "detail": "Wallet not found for identifier BPNL00000000000",
                                      "instance": "/BPNL00000000000/did.json",
                                      "properties": {
                                        "timestamp": 1689767698010
                                      }
                                    }
                                    """)
                    }) }),
            @ApiResponse(responseCode = "200", description = "DID document", content = {
                    @Content(examples = {
                            @ExampleObject(name = " DID document", value = """
                                     {
                                        "@context": [
                                         "https://www.w3.org/ns/did/v1",
                                         "https://w3c.github.io/vc-jws-2020/contexts/v1"
                                       ],
                                       "id": "did:web:localhost:BPNL000000000000",
                                       "verificationMethod": [
                                         {
                                           "controller": "did:web:localhost:BPNL000000000000",
                                           "id": "did:web:localhost:BPNL000000000000",
                                           "publicKeyJwk": {
                                             "crv": "Ed25519",
                                             "kty": "OKP",
                                             "x": "wAOQvr92L1m7RwrpeOrgWByVYvWmhRr4fJbiMwHEIdY"
                                           },
                                           "type": "JsonWebKey2020"
                                         }
                                       ]
                                     }
                                    """)
                    })
            }) })
    @Operation(description = "Resolve the DID document for a given DID or BPN", summary = "Resolve DID Document", security = { @SecurityRequirement(name = "Authenticate using access_token") })
    public @interface GetDidDocumentApiDocs {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses(value = {
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
                                    """)
                    }) }),
            @ApiResponse(responseCode = "404", description = "Wallet not found with provided bpn", content = {
                    @Content(examples = {
                            @ExampleObject(name = "Wallet not found with provided bpn", value = """
                                    {
                                      "type": "about:blank",
                                      "title": "Wallet not found for identifier BPNL00000000000",
                                      "status": 404,
                                      "detail": "Wallet not found for identifier BPNL00000000000",
                                      "instance": "/BPNL00000000000/did.json",
                                      "properties": {
                                        "timestamp": 1689767698010
                                      }
                                    }
                                    """)
                    }) }),
            @ApiResponse(responseCode = "200", description = "DID document", content = {
                    @Content(examples = {
                            @ExampleObject(name = " DID document", value = """
                                     {
                                       "@context": [
                                         "https://www.w3.org/ns/did/v1",
                                         "https://w3c.github.io/vc-jws-2020/contexts/v1"
                                       ],
                                       "id": "did:web:localhost:BPNL000000000000",
                                       "verificationMethod": [
                                         {
                                           "controller": "did:web:localhost:BPNL000000000000",
                                           "id": "did:web:localhost:BPNL000000000000",
                                           "publicKeyJwk": {
                                             "crv": "Ed25519",
                                             "kty": "OKP",
                                             "x": "wAOQvr92L1m7RwrpeOrgWByVYvWmhRr4fJbiMwHEIdY"
                                           },
                                           "type": "JsonWebKey2020"
                                         }
                                       ]
                                     }
                                    """)
                    })
            })
    })
    @Operation(description = "Resolve the DID document for a given BPN", summary = "Resolve DID Document", security = { @SecurityRequirement(name = "Authenticate using access_token") })
    public @interface GetDidResolveApiDocs {
    }

    @Parameter(description = "Did or BPN", examples = {
            @ExampleObject(name = "bpn", value = "BPNL000000000000", description = "bpn"),
            @ExampleObject(description = "did", name = "did", value = "did:web:localhost:BPNL000000000000") })
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DidOrBpnParameterDoc {
    }

    @Parameter(description = "BPN", examples = {
            @ExampleObject(name = "bpn", value = "BPNL000000000000", description = "bpn") })
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BpnParameterDoc {
    }

}
