package com.couchbase.lite.mobiletestkit.legacy;


import com.couchbase.lite.mobiletestkit.Args;
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
