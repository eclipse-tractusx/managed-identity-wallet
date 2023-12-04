package org.eclipse.tractusx.managedidentitywallets.config;

import org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.WalletRepositoryImpl;
import org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.repository.WalletKeyRepository;
import org.eclipse.tractusx.managedidentitywallets.domain.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletBeanConfig {

  @Bean
  public WalletRepository walletRepository(
      org.eclipse.tractusx.managedidentitywallets.adapter.persistence.dao.repository.WalletRepository walletRepository,
      WalletKeyRepository walletKeyRepository) {
    return new WalletRepositoryImpl(walletRepository, walletKeyRepository);
  }
}
