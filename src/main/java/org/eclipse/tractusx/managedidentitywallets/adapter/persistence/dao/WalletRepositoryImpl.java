package org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.entity.WalletKey;
import org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.BusinessPartnerNumber;
import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.Wallet;
import org.eclipse.tractusx.managedidentitywallets.domain.WalletRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

  private final org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.repository.WalletRepository walletRepo;
  private final WalletKeyRepository keyRepo;

  @Override
  public Optional<Wallet> findWallet(DID did) {
    return Optional.ofNullable(walletRepo.getByDid(did.toString()))
        .map(this::buildWallet);
  }

  @Override
  public Optional<Wallet> findWallet(BusinessPartnerNumber bpn) {
    return Optional.ofNullable(walletRepo.getByBpn(bpn.toString()))
        .map(this::buildWallet);
  }

  private Wallet buildWallet(org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.entity.Wallet wallet) {
    WalletKey key = keyRepo.getByWalletId(wallet.getId());
    return Wallet.builder()
        .businessPartnerNumber(new BusinessPartnerNumber(wallet.getBpn()))
        .did(new DID(wallet.getDid()))
        .keys(List.of(
            new KeyPair(key.getPrivateKey(), key.getPublicKey())))
        .build();
  }
}
