package org.eclipse.tractusx.managedidentitywallets.domain;

import java.util.Set;

import com.nimbusds.jwt.JWT;

public interface SecureTokenIssuer {
  /**
   * Issues an ID Token with an embedded access token, as long as scopes are
   * provided.
   * If no scopes are provided the ID Token will contain no embedded access token.
   * 
   * @param self    The DID for the issuer of the token.
   * @param partner the DID for the receiver of the token.
   * @param keyPair the KeyPair with which the JWT will be signed.
   * @param scopes  the scopes which will be embedded in the access token.
   * @return the ID Token
   */
  JWT issueIdToken(DID self, DID partner, KeyPair keyPair, Set<String> scopes);

  /**
   * Issues an ID Token without creating a new embedded access token. 
   * This method is used to issue a new ID Token with a received access token.
   * @param self        The DID for the issuer of the token.
   * @param partner     the DID for the receiver of the token.
   * @param keyPair     the KeyPair with which the JWT will be signed.
   * @param accessToken an access token that should be embedded.
   * @return the ID Token
   */
  JWT issueIdToken(DID self, DID partner, KeyPair keyPair, JWT accessToken);
}
