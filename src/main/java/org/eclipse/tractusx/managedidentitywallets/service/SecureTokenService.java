package org.eclipse.tractusx.managedidentitywallets.service;

import java.util.Set;

import org.eclipse.tractusx.managedidentitywallets.domain.DID;

import com.nimbusds.jwt.JWT;

public interface SecureTokenService {
  JWT issueToken(DID self, DID partner, Set<String> scopes);
  JWT issueToken(DID self, DID partner, JWT accessToken);
}
