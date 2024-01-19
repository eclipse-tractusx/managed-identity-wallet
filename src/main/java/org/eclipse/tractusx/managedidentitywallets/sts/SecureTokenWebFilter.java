package org.eclipse.tractusx.managedidentitywallets.adapter.sts;

import java.io.IOException;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter to perform custom authentication based on the request body paramters.
 * (since we want to perform authentication before dispatching the request to the controller)
 * NOTE: this filter will be skipped for all endpoints that are not /token.
 */
public class SecureTokenWebFilter extends OncePerRequestFilter {

  private static final String GRANT_TYPE = "grant_type";
  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String SCOPE = "scope";
  private static final String OPENID = "openid";
  private static final String CLIENT_ID = "client_id";
  private static final String CLIENT_SECRET = "client_secret";

  private final RequestMatcher matcher;
  private final JwtDecoder decoder;
  private final RestTemplate rest;

  public SecureTokenWebFilter(OAuth2ResourceServerProperties properties, RestTemplateBuilder restTemplateBuilder) {
    matcher = new NegatedRequestMatcher(new AntPathRequestMatcher("/token"));
    decoder = JwtDecoders.fromIssuerLocation(properties.getJwt().getIssuerUri());
    rest = restTemplateBuilder
        .rootUri(properties.getJwt().getIssuerUri())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .build();
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<String, String>();
    tokenRequest.add(GRANT_TYPE, CLIENT_CREDENTIALS);
    tokenRequest.add(SCOPE, OPENID);
    tokenRequest.add(CLIENT_ID, request.getParameter(CLIENT_ID));
    tokenRequest.add(CLIENT_SECRET, request.getParameter(CLIENT_SECRET));

    ResponseEntity<OAuth2AccessTokenResponse> idpResponse = rest.postForEntity("/protocol/openid-connect/token",
        request, OAuth2AccessTokenResponse.class);
    decoder.decode(idpResponse.getBody().getAccessToken().getTokenValue());
    // decode throws an exception, if token can not be validated? 
    // TODO need to check that, otherwise we need to provide additional validation code to check whether the token received
    // from the IdP is actually valid, in all other cases run the next filter in the chain
    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    return matcher.matches(request);
  }

}
