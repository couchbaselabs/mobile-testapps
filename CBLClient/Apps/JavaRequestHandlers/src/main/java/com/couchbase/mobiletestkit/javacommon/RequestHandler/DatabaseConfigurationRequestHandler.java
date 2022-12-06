package com.couchbase.mobiletestkit.javacommon.RequestHandler;


import com.couchbase.CouchbaseLiteServ.TestServerApp;
import com.couchbase.CouchbaseLiteServ.util.Log;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.EncryptionKey;
import com.couchbase.mobiletestkit.javacommon.Args;


public class DatabaseConfigurationRequestHandler {
    private static final String TAG = "DATABASE_CONFIG";

    public DatabaseConfiguration configure(Args args) {
        String directory = args.getString("directory");
        if (directory == null) {
            directory = TestServerApp.getApp().getFilesDir().getAbsolutePath();
            Log.i(TAG, "No directory is set, now point to " + directory);
        }
        Log.i(TAG, "DatabaseConfiguration_configure directory=" + directory);
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory(directory);

        EncryptionKey encryptionKey;
        String password = args.getString("password");
        if (password != null) {
            encryptionKey = new EncryptionKey(password);
            config.setEncryptionKey(encryptionKey);
        }
        return config;
    }

    public String getDirectory(Args args) {
        DatabaseConfiguration config = args.get("config", DatabaseConfiguration.class);
        return config.getDirectory();
    }

    public EncryptionKey getEncryptionKey(Args args) {
        DatabaseConfiguration config = args.get("config", DatabaseConfiguration.class);
        return config.getEncryptionKey();
    }

    public DatabaseConfiguration setDirectory(Args args) {
        DatabaseConfiguration config = args.get("config", DatabaseConfiguration.class);
        String directory = args.getString("directory");
        return config.setDirectory(directory);
    }

    public DatabaseConfiguration setEncryptionKey(Args args) {
        DatabaseConfiguration config = args.get("config", DatabaseConfiguration.class);
        String password = args.getString("password");
        EncryptionKey encryptionKey = new EncryptionKey(password);
        return config.setEncryptionKey(encryptionKey);
    }
}
