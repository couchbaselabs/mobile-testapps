package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.BasicAuthenticator
import com.couchbase.mobiletestkit.javacommon.Args

class BasicAuthenticatorRequestHandler {
    /* ---------------------- */ /* - BasicAuthenticator - */ /* ---------------------- */
    fun create(args: Args): BasicAuthenticator {
        val username = args.get<String>("username")
        val password = args.get<String>("password")
        val passwordData = password.toCharArray()
        return BasicAuthenticator(username, passwordData)
    }

    fun getPassword(args: Args): CharArray {
        val authenticator = args.get<BasicAuthenticator>("authenticator")
        return authenticator.passwordChars
    }

    fun getUsername(args: Args): String {
        val authenticator = args.get<BasicAuthenticator>("authenticator")
        return authenticator.username
    }
}