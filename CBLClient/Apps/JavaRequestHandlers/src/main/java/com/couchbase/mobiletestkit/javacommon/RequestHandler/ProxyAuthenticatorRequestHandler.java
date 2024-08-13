package com.couchbase.mobiletestkit.javacommon.RequestHandler;
import com.couchbase.lite.ProxyAuthenticator;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.util.PasswordAuthenticator;

import java.util.Arrays;


public class ProxyAuthenticatorRequestHandler {
    /* ---------------------- */
    /* - ProxyAuthenticator - */
    /* ---------------------- */

    public ProxyAuthenticator create(Args args) {
        String username = args.get("username");
        String password = args.get("password");
        char[] passwordData = password.toCharArray();
        return new ProxyAuthenticator(username, passwordData);
    }

    public char[] getPassword(Args args) {
        ProxyAuthenticator authenticator = args.get("authenticator");
        return authenticator.getPassword();
    }

    public String getUsername(Args args) {
        ProxyAuthenticator authenticator = args.get("authenticator");
        return authenticator.getUsername();
    }
}