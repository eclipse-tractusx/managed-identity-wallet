package org.eclipse.tractusx.managedidentitywallets.adapter.sts;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.eclipse.tractusx.managedidentitywallets.domain.DID;
import org.eclipse.tractusx.managedidentitywallets.domain.KeyPair;
import org.eclipse.tractusx.managedidentitywallets.domain.SecureTokenIssuer;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureTokenIssuerImpl implements SecureTokenIssuer {

  private static final String ACCESS_TOKEN = "access_token";

  private final SecureTokenConfigurationProperties properties;

  @Override
  public JWT issueIdToken(DID self, DID partner, KeyPair keyPair) {
    log.info("Requested ID token for us: '{}' and partner '{}'", self, partner);
    Instant expirationTime = Instant.now().plus(properties.tokenDuration());
    JWT accessToken = createAccessToken(keyPair, self, partner, expirationTime);
    log.info("Access token created for us: '{}' expiring at '{}'", self, expirationTime);
    return createIdToken(keyPair, self, partner, expirationTime, accessToken);
  }

  @Override
  public JWT issueIdToken(DID self, DID partner, KeyPair keyPair, JWT accessToken) {
    log.info("Requested ID token for us: '{}' and partner '{}' with existing access token.", self, partner);
    Instant expirationTime = Instant.now().plus(properties.tokenDuration());
    return createIdToken(keyPair, self, partner, expirationTime, accessToken);
  }

  private JWT createIdToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime, JWT accessToken) {
    return createToken(keyPair, new JWTClaimsSet.Builder()
        .issuer(self.toString())
        .audience(partner.toString())
        .subject(self.toString())
        .expirationTime(Date.from(expirationTime))
        .claim(ACCESS_TOKEN, accessToken));
  }

  private JWT createAccessToken(KeyPair keyPair, DID self, DID partner, Instant expirationTime) {
    return createToken(keyPair, new JWTClaimsSet.Builder()
        .issuer(self.toString())
        .audience(partner.toString())
        .subject(partner.toString())
        .expirationTime(Date.from(expirationTime)));
  }

  @SneakyThrows
  private JWT createToken(KeyPair keyPair, JWTClaimsSet.Builder builder) {
    log.debug("Creating JWS header for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
        builder.getClaims().get("sub"));
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
        .type(JOSEObjectType.JWT)
        .keyID(UUID.fromString(keyPair.publicKey()).toString())
        .build();

    log.debug("Creating JWS body for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
        builder.getClaims().get("sub"));
    JWTClaimsSet body = builder
        .issueTime(Date.from(Instant.now()))
        .jwtID(UUID.randomUUID().toString())
        .build();

    log.debug("Creating JWS signature for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
        builder.getClaims().get("sub"));
    SignedJWT signedJWT = new SignedJWT(header, body);
    OctetKeyPair jwk = new OctetKeyPair.Builder(Curve.Ed25519, new Base64URL(keyPair.publicKey()))
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.fromString(keyPair.publicKey()).toString())
        .d(new Base64URL(keyPair.privateKey()))
        .build();

    signedJWT.sign(new Ed25519Signer(jwk));
    log.debug("JWT signed for issuer '{}' and holder '{}'", builder.getClaims().get("iss"),
        builder.getClaims().get("sub"));

    return signedJWT;
  }

}
