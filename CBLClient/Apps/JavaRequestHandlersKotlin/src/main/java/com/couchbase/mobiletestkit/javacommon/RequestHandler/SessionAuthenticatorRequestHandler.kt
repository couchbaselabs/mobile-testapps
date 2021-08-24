package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.SessionAuthenticator
import com.couchbase.mobiletestkit.javacommon.Args

class SessionAuthenticatorRequestHandler {
    /* ------------------------ */ /* - SessionAuthenticator - */ /* ------------------------ */
    fun create(args: Args): SessionAuthenticator {
        val sessionId = args.get<String>("sessionId")
        val cookieName = args.get<String>("cookieName") ?: return SessionAuthenticator(sessionId)
        return SessionAuthenticator(sessionId, cookieName)
    }

    fun getSessionId(args: Args): String {
        val session = args.get<SessionAuthenticator>("session")
        return session.sessionID
    }

    fun getCookieName(args: Args): String? {
        val session = args.get<SessionAuthenticator>("session")
        return session.cookieName
    }
}