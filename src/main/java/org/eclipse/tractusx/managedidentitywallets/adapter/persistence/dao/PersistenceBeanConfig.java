package org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao;

import org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceBeanConfig {
  
  @Bean
  public WalletRepository walletRepository(org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.repository.WalletRepository walletRepo, WalletKeyRepository walletKeyRepo) {
    return new WalletRepositoryImpl(walletRepo, walletKeyRepo);
  }

}
