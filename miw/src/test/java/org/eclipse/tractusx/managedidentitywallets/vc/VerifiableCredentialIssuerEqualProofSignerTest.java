package org.eclipse.tractusx.managedidentitywallets.vc;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.eclipse.tractusx.managedidentitywallets.ManagedIdentityWalletsApplication;
import org.eclipse.tractusx.managedidentitywallets.config.MIWSettings;
import org.eclipse.tractusx.managedidentitywallets.config.TestContextInitializer;
import org.eclipse.tractusx.managedidentitywallets.constant.StringPool;
import org.eclipse.tractusx.managedidentitywallets.dao.entity.Wallet;
import org.eclipse.tractusx.managedidentitywallets.service.CommonService;
import org.eclipse.tractusx.managedidentitywallets.service.PresentationService;
import org.eclipse.tractusx.managedidentitywallets.service.WalletKeyService;
import org.eclipse.tractusx.managedidentitywallets.utils.TestUtils;
import org.eclipse.tractusx.ssi.lib.crypt.x21559.x21559PrivateKey;
import org.eclipse.tractusx.ssi.lib.model.proof.jws.JWSSignature2020;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredential;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialBuilder;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialSubject;
import org.eclipse.tractusx.ssi.lib.model.verifiable.credential.VerifiableCredentialType;
import org.eclipse.tractusx.ssi.lib.proof.LinkedDataProofGenerator;
import org.eclipse.tractusx.ssi.lib.proof.SignatureType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.tractusx.managedidentitywallets.constant.StringPool.COLON_SEPARATOR;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = { ManagedIdentityWalletsApplication.class })
@ContextConfiguration(initializers = { TestContextInitializer.class })
public class VerifiableCredentialIssuerEqualProofSignerTest {

    @Autowired
    private MIWSettings miwSettings;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WalletKeyService walletKeyService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private PresentationService presentationService;

    @SneakyThrows
    @Test
    public void test() {
        var bpn1 = "BPNL000000000FOO";
        String defaultLocation = miwSettings.host() + COLON_SEPARATOR + bpn1;
        var response1 = TestUtils.createWallet(bpn1, "did:web:localhost%3A8080:BPNL000000000FOO", restTemplate, miwSettings.authorityWalletBpn(), defaultLocation);
        Assertions.assertTrue(response1.getStatusCode().is2xxSuccessful(), "Wallet 1 creation failed. " + response1.getBody());
        var wallet1 = commonService.getWalletByIdentifier(bpn1);

        var bpn2 = "BPNL000000000BAR";
        String defaultLocation2 = miwSettings.host() + COLON_SEPARATOR + bpn2;
        var response2 = TestUtils.createWallet(bpn2, "did:web:localhost%3A8080:BPNL000000000BAR", restTemplate, miwSettings.authorityWalletBpn(), defaultLocation2);
        Assertions.assertTrue(response2.getStatusCode().is2xxSuccessful(), "Wallet 2 creation failed. " + response2.getBody());
        var wallet2 = commonService.getWalletByIdentifier(bpn2);

        // create vc where issuer and proof-signer are different
        var verifiableCredentialWithSignerDifferentIssuer = issueVC(wallet1.getDid(), wallet2);
        var verifiableCredentialWithSignerEqualIssuer = issueVC(wallet1.getDid(), wallet1);

        var presentationWithSignerDifferentIssuer = presentationService.createPresentation(
                Map.of(StringPool.VERIFIABLE_CREDENTIALS, List.of(verifiableCredentialWithSignerDifferentIssuer)),
                true, "audience", miwSettings.authorityWalletBpn());
        var presentationCredentialWithSignerEqualIssuer = presentationService.createPresentation(
                Map.of(StringPool.VERIFIABLE_CREDENTIALS, List.of(verifiableCredentialWithSignerEqualIssuer)),
                true, "audience", miwSettings.authorityWalletBpn());

        var resultSignerEqualIssuer = presentationService.validatePresentation(presentationCredentialWithSignerEqualIssuer, true, false, "audience");
        var resultSignerDifferentIssuer = presentationService.validatePresentation(presentationWithSignerDifferentIssuer, true, false, "audience");

        Assertions.assertFalse((boolean) resultSignerDifferentIssuer.get(StringPool.VALID), "Presentation should not be valid. Issuer different than proof-signer. Verifiable Credential:\n" + verifiableCredentialWithSignerEqualIssuer.toPrettyJson());
        Assertions.assertTrue((boolean) resultSignerEqualIssuer.get(StringPool.VALID), "Presentation should be valid. Issuer equal than proof-signer. Verifiable Credential:\n" + verifiableCredentialWithSignerEqualIssuer.toPrettyJson());
    }

    @SneakyThrows
    private VerifiableCredential issueVC(String issuerDid, Wallet signerWallet) throws JsonProcessingException {
        List<URI> contexts = new ArrayList();
        contexts.add(URI.create("https://www.w3.org/2018/credentials/v1"));
        // if the credential does not contain the JWS proof-context add it
        URI jwsUri = URI.create("https://w3id.org/security/suites/jws-2020/v1");
        if (!contexts.contains(jwsUri)) {
            contexts.add(jwsUri);
        }

        URI id = URI.create(UUID.randomUUID().toString());
        VerifiableCredentialBuilder builder =
                new VerifiableCredentialBuilder()
                        .context(contexts)
                        .id(URI.create(issuerDid + "#" + id))
                        .type(List.of(VerifiableCredentialType.VERIFIABLE_CREDENTIAL))
                        .issuer(URI.create(issuerDid))
                        .issuanceDate(Instant.now())
                        .expirationDate(Instant.now().plusSeconds(60 * 60 * 24 * 365))
                        .credentialSubject(new VerifiableCredentialSubject(Map.of("id", "foo")));

        LinkedDataProofGenerator generator = LinkedDataProofGenerator.newInstance(SignatureType.JWS);
        URI verificationMethod = signerWallet.getDidDocument().getVerificationMethods().get(0).getId();

        byte[] privateKeyBytes = walletKeyService.getPrivateKeyByWalletIdentifierAsBytes(signerWallet.getId(), signerWallet.getAlgorithm());

        JWSSignature2020 proof =
                (JWSSignature2020) generator.createProof(builder.build(), verificationMethod, new x21559PrivateKey(privateKeyBytes));

        //Adding Proof to VC
        builder.proof(proof);

        //Create Credential
        return builder.build();
    }
}
