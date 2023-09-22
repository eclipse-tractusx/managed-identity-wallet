Feature: Create wallet with new/random BPN

  @TEST_CXGP-573
  Scenario: TC_CX_Core_MIW_Wallet_Create wallet using random BPN and verify wallet, did document, BPN and Summery credentials
    Given keycloak client_id, client_secret of base wallet and MIW application host
    Then Create access_token using client_id and client_secret
    Then Create random BPN and create wallet using this BPN number
    Then Verify API response code, it should be 201
    Then Verify API response body, it should contains wallet information
    Then Verify did document is resolvable
    Then Verify BPN credential should be issued
    Then Verify Summary credential should be issued with BPN credential entry

  @TEST_CXGP-577
  Scenario: TC_CX_Core_MIW_Wallet_Create wallet with duplicate BPN number
    Given keycloak client_id, client_secret of base wallet and MIW application host
    Then Create access_token using client_id and client_secret
    Then Get any one wallet information and take BPN of this wallet
    Then Create wallet with this BPN
    Then Verify wallet creation should be failed with http status code 409

  @TEST_CXGP-588
  Scenario: TC_CX_Core_MIW_Wallet_Get wallet using users client_id and client_secret and check BPN and Summary VC using users token
    Given Keycloak client_id and client_secret of base wallet, client_id and client_secret of user wallet, MIW host and users BPN
    Then Create access_token using client_id and client_secret of the base wallet
    Then Create a wallet with the user's BPN if not created
    Then Create access_token using the client_id and client_secret of the user's wallet
    Then Get user wallet with credentials
    Then Verify that the user must have DID Document
    Then Verify that the user must have BPN and Summary credentials

  @TEST_CXGP-735
  Scenario: TC_CX_Core_MIW_Wallet_Store credential to wallet
    Given Keycloak client_id and client_secret of user wallet, MIW host and users BPN
    Then Create access_token using the client_id and client_secret of the user's wallet
    Then Store any custom VC in wallet
    Then verify API response, status should be 201
    Then Get this stored VC using holder API, it should return stored VC