package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.io.File;

import com.couchbase.lite.logging.FileLogSink;
import com.couchbase.lite.logging.LogSinks;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher;
import com.couchbase.mobiletestkit.javacommon.RawData;
import com.couchbase.mobiletestkit.javacommon.util.Log;
import com.couchbase.mobiletestkit.javacommon.util.ZipUtils;

import com.couchbase.lite.*;

public class LoggingRequestHandler {
    private static final String TAG = "LOGREQHANDLER";
    /* ----------- */
    /* - Logging - */
    /* ----------- */

    public FileLogSink configure(Args args) {
        String log_level = args.get("log_level");
        String directory = args.get("directory");
        int maxRotateCount = args.get("max_rotate_count");
        long maxSize = args.get("max_size");
        boolean plainText = args.get("plain_text");

        if (directory.isEmpty()) {
            long ts = System.currentTimeMillis() / 1000;
            directory = RequestHandlerDispatcher.context.getFilesDir().getAbsolutePath() + File.separator + "logs_" + ts;
            Log.i(TAG, "File logging configured at: " + directory);
        }
        FileLogSink.Builder builder = new FileLogSink.Builder()
                .setDirectory(directory);
        if (maxRotateCount > 1) {
            builder.setMaxKeptFiles(maxRotateCount+1);
        }
        if (maxSize > 512000) {
            builder.setMaxFileSize(maxSize);
        }
        builder.setPlainText(plainText);

        LogLevel level = parseLogLevel(log_level);
        builder.setLevel(level);

        FileLogSink sink = builder.build();
        LogSinks.get().setFile(sink);
        return sink;
    }

    public boolean getPlainTextStatus(Args args) {
        FileLogSink sink = LogSinks.get().getFile();
        return sink != null && sink.isPlainText();
    }

    public int getMaxRotateCount(Args args) {
        FileLogSink sink = LogSinks.get().getFile();
        return sink != null ? sink.getMaxKeptFiles() - 1 : 0;
    }

    public long getMaxSize(Args args) {
        FileLogSink sink = LogSinks.get().getFile();
        return sink != null ? sink.getMaxFileSize(): 0;
    }

    public String getDirectory(Args args) {
        FileLogSink sink = LogSinks.get().getFile();
        return sink != null ? sink.getDirectory() : "";
    }

    public FileLogSink getConfig(Args args) {
        return LogSinks.get().getFile();
    }

    public FileLogSink setPlainTextStatus(Args args) {
        Boolean plain_text = args.get("plain_text");
        FileLogSink currentSink = LogSinks.get().getFile();

        if (currentSink == null) return null;

        FileLogSink newSink = new FileLogSink.Builder(currentSink)
                .setPlainText(plain_text)
                .build();

        LogSinks.get().setFile(newSink);
        return newSink;
    }

    public FileLogSink setMaxRotateCount(Args args) {
        int max_rotate_count = args.get("max_rotate_count");
        FileLogSink currentSink = LogSinks.get().getFile();

        if (currentSink == null) return null;

        FileLogSink newSink = new FileLogSink.Builder(currentSink)
                .setMaxKeptFiles(max_rotate_count+1)
                .build();

        LogSinks.get().setFile(newSink);
        return newSink;
    }


    public FileLogSink setMaxSize(Args args) {
        long max_size = args.get("max_size");
        FileLogSink currentSink = LogSinks.get().getFile();

        if (currentSink == null) return null;

        FileLogSink newSink = new FileLogSink.Builder(currentSink)
                .setMaxFileSize(max_size)
                .build();

        LogSinks.get().setFile(newSink);
        return newSink;
    }

    public FileLogSink setConfig(Args args) {
        String directory = args.get("directory");
        if (directory.isEmpty()) {
            long ts = System.currentTimeMillis() / 1000;
            directory = RequestHandlerDispatcher.context.getFilesDir().getAbsolutePath() + File.separator + "logs_" + ts;

            Log.i(TAG, "File logging configured at: " + directory);
        }
        FileLogSink newSink = new FileLogSink.Builder()
                .setDirectory(directory)
                .build();
        LogSinks.get().setFile(newSink);
        return newSink;
    }

    public int getLogLevel(Args args) {
        FileLogSink sink = LogSinks.get().getFile();
        return sink != null ? sink.getLevel().ordinal() : LogLevel.NONE.ordinal();
    }

    public FileLogSink setLogLevel(Args args) {
        String log_level = args.get("log_level");
        FileLogSink currentSink = LogSinks.get().getFile();

        if (currentSink == null) return null;

        LogLevel level = parseLogLevel(log_level);
        FileLogSink newSink = new FileLogSink.Builder(currentSink)
                .setLevel(level)
                .build();

        LogSinks.get().setFile(newSink);
        return newSink;
    }

    public RawData getLogsInZip(Args args) {
        FileLogSink fileLoggerConfig = LogSinks.get().getFile();
        if (fileLoggerConfig == null) { return null; }

        ZipUtils zipper = new ZipUtils();

        File zipDir = RequestHandlerDispatcher.context.getExternalFilesDir("zip");
        try {
            File zipFile = new File(zipDir, "archive.zip");
            if (zipFile.exists()) { zipper.deleteRecursive(zipFile); }

            zipper.zipDirectory(fileLoggerConfig.getDirectory(), zipFile);
            return new RawData("application/zip", zipper.readFile(zipFile));
        }
        finally {
            zipper.deleteRecursive(zipDir);
        }
    }

    private LogLevel parseLogLevel(String log_level) {
        switch (log_level) {
            case "debug": return LogLevel.DEBUG;
            case "verbose": return LogLevel.VERBOSE;
            case "info": return LogLevel.INFO;
            case "error": return LogLevel.ERROR;
            case "warning": return LogLevel.WARNING;
            default: return LogLevel.NONE;
        }
    }

}
