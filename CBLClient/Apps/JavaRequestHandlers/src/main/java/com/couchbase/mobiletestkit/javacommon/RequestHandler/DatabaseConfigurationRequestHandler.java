package com.couchbase.mobiletestkit.javacommon.RequestHandler;


import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher;
import com.couchbase.mobiletestkit.javacommon.util.Log;
import com.couchbase.lite.DatabaseConfiguration;

public class DatabaseConfigurationRequestHandler {
    private static final String TAG = "DATABASE_CONFIG";
    public DatabaseConfiguration configure(Args args) {
        String directory = args.get("directory");
        if (directory == null) {
            directory = RequestHandlerDispatcher.context.getFilesDir().getAbsolutePath();
            Log.i(TAG, "No directory is set, now point to " + directory);
        }
        Log.i(TAG, "DatabaseConfiguration_configure directory=" + directory);
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory(directory);

        String password = args.get("password");
        return config;
    }

    /*public ConflictResolver getConflictResolver(Args args){
        DatabaseConfiguration config = args.get("config");
        return config.getConflictResolver();
    }*/

    public String getDirectory(Args args) {
        DatabaseConfiguration config = args.get("config");
        return config.getDirectory();
    }

    /*public DatabaseConfiguration setConflictResolver(Args args){
        DatabaseConfiguration config = args.get("config");
        ConflictResolver conflictResolver = args.get("conflictResolver");
        return config.setConflictResolver(conflictResolver);
    }*/

    public DatabaseConfiguration setDirectory(Args args) {
        DatabaseConfiguration config = args.get("config");
        String directory = args.get("directory");
        return config.setDirectory(directory);
    }

}
