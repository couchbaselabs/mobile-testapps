package com.couchbase.mobiletestkit.javacommon.RequestHandler;


import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.lite.EncryptionKey;


public class EncryptionKeyRequestHandler {

    public EncryptionKey create(Args args) {
        byte[] key = args.getData("key");
        String password = args.getString("password");

        if (password != null) {
            return new EncryptionKey(password);
        }
        else if (key != null) {
            return new EncryptionKey(key);
        }
        else {
            throw new IllegalArgumentException("an Encryption parameter is null");
        }
    }
}
