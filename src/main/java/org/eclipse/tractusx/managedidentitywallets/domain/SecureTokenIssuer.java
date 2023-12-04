package org.eclipse.tractusx.managedidentitywallets.domain;

import com.nimbusds.jwt.JWT;

public interface SecureTokenIssuer {
  JWT issueIdToken(DID self, DID partner, KeyPair keyPair);
  JWT issueIdToken(DID self, DID partner, KeyPair keyPair, JWT accessToken);
}
