package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.io.File;
import java.io.IOException;

import com.couchbase.CouchbaseLiteServ.TestServerApp;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.CouchbaseLiteServ.util.Log;
import com.couchbase.mobiletestkit.javacommon.util.FileUtils;

import com.couchbase.lite.*;

public class LoggingRequestHandler {
    private static final String TAG = "LOGREQHANDLER";
    /* ----------- */
    /* - Logging - */
    /* ----------- */

    public LogFileConfiguration configure(Args args) {
        String log_level = args.getString("log_level");
        String directory = args.getString("directory");
        int maxRotateCount = args.getInt("max_rotate_count");
        long maxSize = args.getLong("max_size");
        boolean plainText = args.getBoolean("plain_text");

        if (directory.isEmpty()) {
            long ts = System.currentTimeMillis() / 1000;
            directory = TestServerApp.getApp().getFilesDir().getAbsolutePath() + File.separator + "logs_" + ts;
            Log.i(TAG, "File logging configured at: " + directory);
        }
        LogFileConfiguration config = new LogFileConfiguration(directory);
        if (maxRotateCount > 1) {
            config.setMaxRotateCount(maxRotateCount);
        }
        if (maxSize > 512000) {
            config.setMaxSize(maxSize);
        }
        config.setUsePlaintext(plainText);
        Database.log.getFile().setConfig(config);
        switch (log_level) {
            case "debug":
                Database.log.getFile().setLevel(LogLevel.DEBUG);
                break;
            case "verbose":
                Database.log.getFile().setLevel(LogLevel.VERBOSE);
                break;
            case "info":
                Database.log.getFile().setLevel(LogLevel.INFO);
                break;
            case "error":
                Database.log.getFile().setLevel(LogLevel.ERROR);
                break;
            case "warning":
                Database.log.getFile().setLevel(LogLevel.WARNING);
                break;
            default:
                Database.log.getFile().setLevel(LogLevel.NONE);
                break;
        }
        return config;
    }

    public boolean getPlainTextStatus() { return Database.log.getFile().getConfig().usesPlaintext(); }

    public int getMaxRotateCount() { return Database.log.getFile().getConfig().getMaxRotateCount(); }

    public long getMaxSize() { return Database.log.getFile().getConfig().getMaxSize(); }

    public String getDirectory() { return Database.log.getFile().getConfig().getDirectory(); }

    public LogFileConfiguration getConfig() { return Database.log.getFile().getConfig(); }

    public LogFileConfiguration setPlainTextStatus(Args args) {
        LogFileConfiguration config = args.get("config", LogFileConfiguration.class);
        Boolean plain_text = args.getBoolean("plain_text");
        config.setUsePlaintext(plain_text);
        return config;
    }

    public LogFileConfiguration setMaxRotateCount(Args args) {
        LogFileConfiguration config = args.get("config", LogFileConfiguration.class);
        int max_rotate_count = args.getInt("max_rotate_count");
        config.setMaxRotateCount(max_rotate_count);
        return config;
    }

    public LogFileConfiguration setMaxSize(Args args) {
        LogFileConfiguration config = args.get("config", LogFileConfiguration.class);
        long max_size = args.getLong("max_size");
        config.setMaxSize(max_size);
        return config;
    }

    public LogFileConfiguration setConfig(Args args) {
        String directory = args.getString("directory");
        if (directory.isEmpty()) {
            long ts = System.currentTimeMillis() / 1000;
            directory = TestServerApp.getApp().getFilesDir().getAbsolutePath() + File.separator + "logs_" + ts;

            Log.i(TAG, "File logging configured at: " + directory);
        }
        LogFileConfiguration config = new LogFileConfiguration(directory);
        Database.log.getFile().setConfig(config);
        return config;
    }

    public int getLogLevel() { return Database.log.getFile().getLevel().ordinal(); }

    public LogFileConfiguration setLogLevel(Args args) {
        LogFileConfiguration config = args.get("config", LogFileConfiguration.class);
        String log_level = args.getString("log_level");
        switch (log_level) {
            case "debug":
                Database.log.getFile().setLevel(LogLevel.DEBUG);
                break;
            case "verbose":
                Database.log.getFile().setLevel(LogLevel.VERBOSE);
                break;
            case "info":
                Database.log.getFile().setLevel(LogLevel.INFO);
                break;
            case "error":
                Database.log.getFile().setLevel(LogLevel.ERROR);
                break;
            case "warning":
                Database.log.getFile().setLevel(LogLevel.WARNING);
                break;
            default:
                Database.log.getFile().setLevel(LogLevel.NONE);
                break;
        }
        return config;
    }

    public byte[] getLogsInZip() throws IOException {
        LogFileConfiguration fileLoggerConfig = Database.log.getFile().getConfig();
        if (fileLoggerConfig == null) { return null; }

        FileUtils zipper = new FileUtils();

        File zipDir = TestServerApp.getApp().getExternalFilesDir("zip");
        try {
            File zipFile = new File(zipDir, "archive.zip");
            if (zipFile.exists()) { zipper.deleteRecursive(zipFile); }

            zipper.zipDirectory(fileLoggerConfig.getDirectory(), zipFile);

            return zipper.readFile(zipFile);
        }
        finally {
            zipper.deleteRecursive(zipDir);
        }
    }

}
