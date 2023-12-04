package org.eclipse.tractusx.managedidentitywallets.adapter.controller;

import org.eclipse.tractusx.managedidentitywallets.service.SecureTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jwt.JWT;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SecureTokenControllerImpl implements SecureTokenController {
  
  private final SecureTokenService tokenService;

  public ResponseEntity<JWT> createToken(){
    tokenService.issueToken(null, null);
    return ResponseEntity.of(null);
  }

}
