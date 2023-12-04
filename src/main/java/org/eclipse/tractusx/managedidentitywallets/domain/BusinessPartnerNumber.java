package org.eclipse.tractusx.managedidentitywallets.domain;

public record BusinessPartnerNumber(String bpn) {
  
  public String toString() {
    return bpn;
  }

}
