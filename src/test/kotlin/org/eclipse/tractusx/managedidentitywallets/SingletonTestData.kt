package org.eclipse.tractusx.managedidentitywallets

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

    fun cleanSingletonTestData() {
        this.baseWalletDID = ""
        this.baseWalletVerKey = ""
        this.signCredentialResponse = ""
        this.isValidVerifiableCredential = true
        this.isValidVerifiablePresentation = true
        this.revocationListName = ""
        this.credentialIndex = 0
        this.encodedList = EnvironmentTestSetup.NONE_REVOKED_ENCODED_LIST
        this.connectionId = ""
        this.threadId = ""
    }

    fun getDidMethodPrefixWithNetworkIdentifier(): String {
        //TODO replace implementation when indy method is supported by AcaPy
        //return "did:indy:${EnvironmentTestSetup.NETWORK_ID}:"
        return "did:sov:"
    }
}
