package org.eclipse.tractusx.managedidentitywallets.sanityTests

import org.eclipse.tractusx.managedidentitywallets.sanityTests.utils.EnvironmentTestSetup

object SingletonTestData {
    lateinit var baseWalletDID: String
    lateinit var baseWalletVerKey: String
    lateinit var signCredentialResponse: String
    var isValidVerifiableCredential: Boolean = true
    var isValidVerifiablePresentation: Boolean = true
    lateinit var revocationListName: String
    lateinit var credentialIndex: Number
    lateinit var encodedList: String
    lateinit var connectionId: String
    lateinit var threadId: String
    var didDocWithoutService: Boolean = false

    fun cleanSingletonTestData() {
        baseWalletDID = ""
        baseWalletVerKey = ""
        signCredentialResponse = ""
        isValidVerifiableCredential = true
        isValidVerifiablePresentation = true
        revocationListName = ""
        credentialIndex = 0
        encodedList = EnvironmentTestSetup.NONE_REVOKED_ENCODED_LIST
        connectionId = ""
        threadId = ""
        didDocWithoutService = false
    }

    fun getDidMethodPrefixWithNetworkIdentifier(): String {
        //TODO replace implementation when indy method is supported by AcaPy
        //return "did:indy:${EnvironmentTestSetup.NETWORK_ID}:"
        return "did:sov:"
    }
}
