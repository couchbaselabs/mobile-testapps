package com.couchbase.lite.mobiletestkit.legacy;


import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.SessionAuthenticator;


public class SessionAuthenticatorRequestHandler {
    /* ------------------------ */
    /* - SessionAuthenticator - */
    /* ------------------------ */

    public SessionAuthenticator create(Args args) {
        String sessionId = args.getString("sessionId");
        String cookieName = args.getString("cookieName");
        if (cookieName == null) {
            return new SessionAuthenticator(sessionId);
        }
        return new SessionAuthenticator(sessionId, cookieName);
    }

    public String getSessionId(Args args) {
        SessionAuthenticator session = args.get("session", SessionAuthenticator.class);
        return session.getSessionID();
    }

    public String getCookieName(Args args) {
        SessionAuthenticator session = args.get("session", SessionAuthenticator.class);
        return session.getCookieName();
    }
}
