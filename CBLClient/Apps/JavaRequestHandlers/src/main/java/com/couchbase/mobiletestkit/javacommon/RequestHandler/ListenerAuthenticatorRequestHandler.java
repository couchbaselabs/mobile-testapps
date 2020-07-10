package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import android.support.annotation.NonNull;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher;
import com.couchbase.lite.ListenerPasswordAuthenticator;



public class ListenerAuthenticatorRequestHandler {

    /* ---------------------- */
    /* - ListenerPasswordAuthenticator - */
    /* ---------------------- */

    public ListenerPasswordAuthenticator create(Args args) {
        String username = args.get("username");
        String pass = args.get("password");
        char[] password = pass.toCharArray();

        PasswordAuthenticator passwordAuthenticator = new PasswordAuthenticator();
        ListenerPasswordAuthenticator listenerPasswordAuthenticator = ListenerPasswordAuthenticator.create(
                passwordAuthenticator);
        listenerPasswordAuthenticator.authenticate(username, password);
        System.out.println(listenerPasswordAuthenticator);
        System.out.println("PASSSSSSS");
        return listenerPasswordAuthenticator;
    }


}



