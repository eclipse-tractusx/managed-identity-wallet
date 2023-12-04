package org.eclipse.tractusx.managedidentitywallets.domain;

import java.util.Optional;

public interface WalletRepository {
  
  Optional<Wallet> findWallet(DID did);
  Optional<Wallet> findWallet(BusinessPartnerNumber bpn);
  
}
