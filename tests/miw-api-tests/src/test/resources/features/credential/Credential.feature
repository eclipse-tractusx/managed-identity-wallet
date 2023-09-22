Feature: Verifiable credential and Verifiable presentation creation and validation

  @TEST_CXGP-595
  Scenario: TC_CX_Core_MIW_Credential_CX type of credentials to user's wallet, verify credentials, create presentation of Summary VC as JWT and verify JWT
    Given keycloak client_id and client_secret of base wallet, MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet using a random BPN
    Then Issue a membership verifiable credential(VC) to that wallet
    Then Issue dismantler VC to that wallet
    Then Issue BehaviorTwinCredential VC to that Wallet
    Then Issue PcfCredential VC to that Wallet
    Then Issue SustainabilityCredential VC to that Wallet
    Then Issue QualityCredential VC to that Wallet
    Then Issue TraceabilityCredential VC to that Wallet
    Then Issue ResiliencyCredential VC to that Wallet
    Then Verify the Summary VC of that wallet also check the items, it should contain all required values
    Then Validate Summary VC with VC expiry date check
    Then Create a Verifiable presentation(VP) of summary VC as JWT
    Then Validate VP

  @TEST_CXGP-753
  Scenario: TC_CX_Core_MIW_Credential issue custom credential to user's wallet
    Given keycloak client_id and client_secret of base wallet, Keycloak client_id and client_secret of user's wallet  MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet with the user's BPN if not created
    Then Get did of user's wallet
    Then Issue any custom type VC to wallet
    Then Verify API response code, it should be 201
    Then Verify API response body , it should contain VC and type of VC as same as issued  type
    Then Check that credential is issued using issuer API
    Then Create access_token using the client_id and client_secret of the user's wallet
    Then Get issued type credential using holder API

  @TEST_CXGP-754
  Scenario: TC_CX_Core_MIW_Credential_Issue membership VC to wallet, get issued membership VC, validate VC, create VP as JWT and validate VP
    Given keycloak client_id and client_secret of base wallet, MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet using a random BPN
    Then Issue a membership verifiable credential(VC) to that wallet
    Then Try to issue membership VC again
    Then It should give duplicate error with status code 409
    Then Validate membership VC
    Then Create VP as JWT of membership VC
    Then Validate membership VP

  @TEST_CXGP-755
  Scenario: TC_CX_Core_MIW_Credential_Issue dismentaler VC to wallet, get issued dismentaler VC, validate VC, create VP as JWT and validate VP
    Given keycloak client_id and client_secret of base wallet, MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet using a random BPN
    Then Issue dismantler VC to that wallet
    Then Try to issue dismentaler VC again
    Then It should give duplicate error with status code 409
    Then Validate dismentaler VC
    Then Create VP as JWT of dismentaler VC
    #Then Validate dismentaler VP    Currently this is not working task is open in catena-x ref: https://github.com/eclipse-tractusx/SSI-agent-lib/issues/4

  @TEST_CXGP-757
  Scenario: TC_CX_Core_MIW_Credential_Issue framework VC to wallet, get issued VC validate VC, create VP as JWT and validate VP
    Given keycloak client_id and client_secret of base wallet, MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet using a random BPN
    Then Issue a framework verifiable credential(VC) to that wallet
    Then Validate framework VC
    Then Create VP as JWT of framework VC
    #Then Validate framework VP    Currently this is not working task is open in catena-x ref: https://github.com/eclipse-tractusx/SSI-agent-lib/issues/4

  @TEST_CXGP-758
  Scenario: TC_CX_Core_MIW_Credential_Create VP of multiple VCs
    Given keycloak client_id and client_secret of base wallet, MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet using a random BPN
    Then Get BPN credential
    Then Get Summary credential
    Then Create VP as JWT of BPN credential and Summary credential
    Then Validate create VP(JWT)

  @TEST_CXGP-759
  Scenario: TC_CX_Core_MIW_Credential_Issue custom credentials to self wallet
    Given keycloak client_id and client_secret of base wallet, Keycloak client_id and client_secret of user's wallet  MIW host application host
    Then Create access_token using client_id and client_secret
    Then Create a wallet with the user's BPN if not created
    Then Get did of user's wallet
    Then Create access_token using the client_id and client_secret of the user's wallet
    Then Issue any random type of credential to self wallet using holder API
    Then Get this type of credential using holder API
    Then Validate this VC
    Then Create VP as JWT of issued VC
    Then Validate custom type of  VP