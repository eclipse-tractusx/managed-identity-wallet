package org.eclipse.tractusx.managedidentitywallets

import java.util.*

object SingletonTestData {
    lateinit var baseWalletDID: String
    lateinit var baseWalletVerKey: String
    lateinit var signCredentialResponse: String
    var isValidVerifiableCredential: Boolean = true
    var isValidVerifiablePresentation: Boolean = true
    lateinit var revocationListName: String
    lateinit var credentialIndex: Number
    lateinit var encodedList: String
}
