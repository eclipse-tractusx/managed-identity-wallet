package org.eclipse.tractusx.managedidentitywallets.adapter.sts;

import org.eclipse.tractusx.managedidentitywallets.domain.SecureTokenIssuer;
import org.eclipse.tractusx.managedidentitywallets.domain.WalletRepository;
import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenService;
import org.eclipse.tractusx.managedidentitywallets.service.impl.SecureTokenServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecureTokenServiceBeanConfig {

  @Bean
  public  SecureTokenService secureTokenService(WalletRepository walletRepo, SecureTokenIssuer issuer) {
    return new SecureTokenServiceImpl(walletRepo, issuer);
  }

}
