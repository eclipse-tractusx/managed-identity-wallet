package org.eclipse.tractusx.managedidentitywallets.adapter.controller;

import java.util.Optional;
import java.util.Set;

import org.eclipse.tractusx.managedidentitywallets.adapter.controller.dto.StsTokenErrorResponse;
import org.eclipse.tractusx.managedidentitywallets.adapter.controller.dto.StsTokenResponse;
import org.eclipse.tractusx.managedidentitywallets.adapter.controller.exception.UnsupportedGrantTypeException;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RestController
@RequiredArgsConstructor
public class SecureTokenController implements TokenApi {

  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String TOKEN_TYPE_BEARER = "Bearer";

  private final SecureTokenService tokenService;

  @Override
  @SneakyThrows
  public ResponseEntity<StsTokenResponse> token(
      @Valid String audience, @Valid String clientId, @Valid String clientSecret, @Valid String grantType,
      @Valid String accessToken, @Valid String bearerAccessAlias, @Valid String bearerAccessScope) {
    if (!grantType.equals(CLIENT_CREDENTIALS)) {
      throw new UnsupportedGrantTypeException("Selected GrantType is not supported.");
    }

    // Authentication is handled in {@link
    // org.eclipse.tractusx.managedidentitywallets.adapter.controller.filter.ClientCredentialsFilter}
    // and {@link
    // org.eclipse.tractusx.managedidentitywallets.adapter.controller.filter.FilterConfig}

    JWT jwt;
    if (accessToken != null && !accessToken.isBlank()) {
      jwt = tokenService.issueToken(new DID(clientId), new DID(audience), JWTParser.parse(accessToken));
    } else {
      jwt = tokenService.issueToken(new DID(clientId), new DID(audience),
          Optional.of(bearerAccessScope).map(scopes -> Set.of(scopes.split(" "))).orElse(Set.of()));
    }

    StsTokenResponse response = new StsTokenResponse();
    response.setAccessToken(jwt.toString());
    response.setExpiresIn(jwt.getJWTClaimsSet().getExpirationTime().getTime());
    response.setTokenType(TOKEN_TYPE_BEARER);
    return ResponseEntity.ok().build();
  }

  @ExceptionHandler(UnsupportedGrantTypeException.class)
  public ResponseEntity<StsTokenErrorResponse> getErrorResponse(RuntimeException e) {
    StsTokenErrorResponse response = new StsTokenErrorResponse();
    response.setError("client_metadata_value_not_supported");
    response.setErrorDescription(e.getMessage());
    return ResponseEntity.badRequest().body(response);
  }

}
