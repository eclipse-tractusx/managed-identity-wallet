package org.eclipse.tractusx.managedidentitywallets.service.impl;

import java.util.Optional;
import java.util.Set;

import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.SecureTokenIssuer;
import org.eclipse.tractusx.managedidentitywallets.domain.Wallet;
import org.eclipse.tractusx.managedidentitywallets.domain.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenService;

import com.nimbusds.jwt.JWT;

public class SecureTokenServiceImpl implements SecureTokenService {

  private final WalletRepository repo;
  private final SecureTokenIssuer tokenIssuer;

  public SecureTokenServiceImpl(final WalletRepository repo, final SecureTokenIssuer tokenIssuer) {
    this.repo = repo;
    this.tokenIssuer = tokenIssuer;
  }

  @Override
  public JWT issueToken(final DID self, final DID partner, Set<String> scopes) {
    Optional<Wallet> wallet = repo.findWallet(self);
    KeyPair keyPair = wallet.map(w -> w.getKeys())
    .flatMap(k -> k.stream().findFirst())
    .orElseThrow();
  
    return this.tokenIssuer.issueIdToken(self, partner, keyPair, scopes);
  }

  @Override
  public JWT issueToken(DID self, DID partner, JWT accessToken) {
    Optional<Wallet> wallet = repo.findWallet(self);
    KeyPair keyPair = wallet.map(w -> w.getKeys())
    .flatMap(k -> k.stream().findFirst())
    .orElseThrow();

    return this.tokenIssuer.issueIdToken(self, partner, keyPair, accessToken);
  }

}
