/*
 * *******************************************************************************
 *  Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ******************************************************************************
 */

package org.eclipse.tractusx.managedidentitywallets.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ssl.KeyStoreSettings;
import com.github.tomakehurst.wiremock.http.JvmProxyConfigurer;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Slf4j
public class TestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String GITHUB_IO = "catenax-ng.github.io";
    public static WireMockServer wireMockServer;

    private static final int port = findFreePort();
    private static final KeycloakContainer KEYCLOAK_CONTAINER = new KeycloakContainer().withRealmImportFile("miw-test-realm.json");

    public static String CA_PATH = null;

    private static final String W3_2018_CREDENTIALS_V1;
    private static final String W3_NS_DID_V1;
    private static final String W3ID;
    private static final String PARTNER;
    private static final String SUMMARY;
    private static final String USE_CASE;

    static{
        Path path = Path.of("src/test/resources/wiremock/wiremock-truststore.jks");
        System.setProperty("javax.net.ssl.trustStore", path.toAbsolutePath().toString());
        Path caPath = Path.of("src/test/resources/wiremock/ca.p12");
        CA_PATH = caPath.toAbsolutePath().toString();
        
        try (InputStream w3ResponseStream = TestContextInitializer.class.getResourceAsStream("/wiremock/w3.org_2018_credentials_v1.json")) {
            W3_2018_CREDENTIALS_V1 = new String(Objects.requireNonNull(w3ResponseStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream w3ResponseStream = TestContextInitializer.class.getResourceAsStream("/wiremock/w3.org_ns_did_v1.json")) {
            W3_NS_DID_V1 = new String(Objects.requireNonNull(w3ResponseStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream w3ResponseStream = TestContextInitializer.class.getResourceAsStream("/wiremock/w3c.github.io.json")) {
            W3ID = new String(Objects.requireNonNull(w3ResponseStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream cofxStream = TestContextInitializer.class.getResourceAsStream("/wiremock/github-businesspartner.json")) {
            PARTNER = new String(Objects.requireNonNull(cofxStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream cofxStream = TestContextInitializer.class.getResourceAsStream("/wiremock/github-summary.json")) {
            SUMMARY = new String(Objects.requireNonNull(cofxStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream cofxStream = TestContextInitializer.class.getResourceAsStream("/wiremock/github-usecase.json")) {
            USE_CASE = new String(Objects.requireNonNull(cofxStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @SneakyThrows
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        wireMockServer = new WireMockServer(wireMockConfig()
                .caKeystorePassword("changeit")
                .caKeystorePath(CA_PATH)
                .enableBrowserProxying(true)
                .caKeystoreSettings(new KeyStoreSettings(CA_PATH, "changeit", "pkcs12")));

        wireMockServer.stubFor(
                get("/2018/credentials/v1")
                        .withHost(equalTo("www.w3.org"))
                        .willReturn(
                                ok(W3_2018_CREDENTIALS_V1).withHeader("Content-Type", "application/json")
                        )
        );

        wireMockServer.stubFor(
                get("/ns/did/v1")
                        .withHost(equalTo("www.w3.org"))
                        .willReturn(
                                ok(W3_NS_DID_V1).withHeader("Content-Type", "application/json")
                        )
        );

        wireMockServer.stubFor(
                get("/security/suites/jws-2020/v1")
                        .withHost(equalTo("www.w3.org"))
                        .willReturn(
                                ok(W3ID).withHeader("Content-Type", "application/json")
                        )
        );

        wireMockServer.stubFor(
                get("/security/suites/jws-2020/v1")
                        .withHost(equalTo("www.w3id.org"))
                        .willReturn(
                                ok(W3ID).withHeader("Content-Type", "application/json")
                        )
        );

        //cofinity-x schema URLs
        wireMockServer.stubFor(
                get("/product-core-schemas/businessPartnerData.json")
                        .withHost(equalTo(GITHUB_IO))
                        .willReturn(
                                ok(PARTNER).withHeader("Content-Type", "application/json")
                        )
        );
        wireMockServer.stubFor(
                get("/product-core-schemas/SummaryVC.json")
                        .withHost(equalTo(GITHUB_IO))
                        .willReturn(
                                ok(SUMMARY).withHeader("Content-Type", "application/json")
                        )
        );

        wireMockServer.stubFor(
                get("/product-core-schemas/UseCaseVC.json")
                        .withHost(equalTo(GITHUB_IO))
                        .willReturn(
                                ok(USE_CASE).withHeader("Content-Type", "application/json")
                        )
        );



        wireMockServer.start();
        JvmProxyConfigurer.configureFor(wireMockServer);

        KEYCLOAK_CONTAINER.start();
        String authServerUrl = KEYCLOAK_CONTAINER.getAuthServerUrl();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        // use explicit initialization as the platform default might fail
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        TestPropertyValues.of(
                "server.port=" + port,
                "miw.host: localhost:${server.port}",
                "miw.enforceHttps=false",
                "miw.encryptionKey="+ Base64.getEncoder().encodeToString(secretKey.getEncoded()),
                "miw.authorityWalletBpn: BPNL000000000000",
                "miw.authorityWalletName: Test-X",
                "miw.authorityWalletDid: did:web:localhost%3A${server.port}:BPNL000000000000",
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driverClassName=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.datasource.username=sa",
                "spring.datasource.password=password",
                "miw.security.auth-server-url=" + authServerUrl,
                "miw.security.clientId=miw_private_client  ",
                "miw.security.auth-url=${miw.security.auth-server-url}realms/${miw.security.realm}/protocol/openid-connect/auth",
                "miw.security.token-url=${miw.security.auth-server-url}realms/${miw.security.realm}/protocol/openid-connect/token",
                "miw.security.refresh-token-url=${miw.security.token-url}",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=${miw.security.auth-server-url}realms/${miw.security.realm}",
                "spring.security.oauth2.resourceserver.jwk-set-uri=${miw.security.auth-server-url}realms/${miw.security.realm}/protocol/openid-connect/certs"
        ).applyTo(applicationContext.getEnvironment());
    }

    public static String getAuthServerUrl() {
        return KEYCLOAK_CONTAINER.getAuthServerUrl();
    }

    @SneakyThrows
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
