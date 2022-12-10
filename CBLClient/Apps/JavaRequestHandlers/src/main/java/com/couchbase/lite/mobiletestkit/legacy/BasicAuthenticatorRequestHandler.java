package com.couchbase.lite.mobiletestkit.legacy;

import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.BasicAuthenticator;


public class BasicAuthenticatorRequestHandler {
    /* ---------------------- */
    /* - BasicAuthenticator - */
    /* ---------------------- */

    public BasicAuthenticator create(Args args) {
        String username = args.getString("username");
        String password = args.getString("password");
        char[] passwordData = password.toCharArray();
        return new BasicAuthenticator(username, passwordData);
    }

    public char[] getPassword(Args args) {
        BasicAuthenticator authenticator = args.get("authenticator", BasicAuthenticator.class);
        return authenticator.getPasswordChars();
    }

    public String getUsername(Args args) {
        BasicAuthenticator authenticator = args.get("authenticator", BasicAuthenticator.class);
        return authenticator.getUsername();
    }
}
