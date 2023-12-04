package org.eclipse.tractusx.managedidentitywallets.domain;

public record DID(String did) {
  
  public String toString() {
    return did;
  }

}
