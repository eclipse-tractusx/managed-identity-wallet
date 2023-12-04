package org.eclipse.tractusx.managedidentitywallets.domain;

import java.util.List;

import org.intellij.lang.annotations.JdkConstants.BoxLayoutAxis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Wallet {
  
  private BusinessPartnerNumber businessPartnerNumber;
  private DID did;
  private List<KeyPair> keys;

}
