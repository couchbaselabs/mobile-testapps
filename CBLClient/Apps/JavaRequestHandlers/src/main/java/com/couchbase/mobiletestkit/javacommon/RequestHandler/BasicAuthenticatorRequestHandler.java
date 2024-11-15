package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.lite.BasicAuthenticator;


public class BasicAuthenticatorRequestHandler {
    /* ---------------------- */
    /* - BasicAuthenticator - */
    /* ---------------------- */

    public BasicAuthenticator create(Args args) {
        String username = args.get("username");
        String password = args.get("password");
        char[] passwordData = password.toCharArray();
        return new BasicAuthenticator(username, passwordData);
    }

    public char[] getPassword(Args args) {
        BasicAuthenticator authenticator = args.get("authenticator");
        return authenticator.getPasswordChars();
    }

    public String getUsername(Args args) {
        BasicAuthenticator authenticator = args.get("authenticator");
        return authenticator.getUsername();
    }
}
