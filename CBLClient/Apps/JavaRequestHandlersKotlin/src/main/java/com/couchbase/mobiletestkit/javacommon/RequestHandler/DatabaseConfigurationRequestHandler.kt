package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.DatabaseConfiguration
import com.couchbase.lite.EncryptionKey
import com.couchbase.lite.create
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher
import com.couchbase.mobiletestkit.javacommon.util.Log

class DatabaseConfigurationRequestHandler {
    fun configure(args: Args): DatabaseConfiguration {
        var directory = args.get<String>("directory")
        if (directory == null) {
            directory = RequestHandlerDispatcher.context.filesDir.absolutePath
            Log.i(
                TAG,
                "No directory is set, now point to $directory"
            )
        }
        Log.i(
            TAG,
            "DatabaseConfiguration_configure directory=$directory"
        )
        val config = DatabaseConfiguration()
        config.create(directory!!)
        val encryptionKey: EncryptionKey
        val password = args.get<String>("password")
        if (password != null) {
            encryptionKey = EncryptionKey(password)
            config.setEncryptionKey(encryptionKey)
        }
        return config
    }

    /*public ConflictResolver getConflictResolver(Args args){
        DatabaseConfiguration config = args.get("config");
        return config.getConflictResolver();
    }*/
    fun getDirectory(args: Args): String {
        val config = args.get<DatabaseConfiguration>("config")
        return config.directory
    }

    fun getEncryptionKey(args: Args): EncryptionKey? {
        val config = args.get<DatabaseConfiguration>("config")
        return config.encryptionKey
    }

    /*public DatabaseConfiguration setConflictResolver(Args args){
        DatabaseConfiguration config = args.get("config");
        ConflictResolver conflictResolver = args.get("conflictResolver");
        return config.setConflictResolver(conflictResolver);
    }*/
    fun setDirectory(args: Args): DatabaseConfiguration {
        val config = args.get<DatabaseConfiguration>("config")
        val directory = args.get<String>("directory")
        return config.setDirectory(directory)
    }

    fun setEncryptionKey(args: Args): DatabaseConfiguration {
        val config = args.get<DatabaseConfiguration>("config")
        val password = args.get<String>("password")
        val encryptionKey = EncryptionKey(password)
        return config.setEncryptionKey(encryptionKey)
    }

    companion object {
        private const val TAG = "DATABASE_CONFIG"
    }
}