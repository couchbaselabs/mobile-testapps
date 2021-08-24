package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.Database
import com.couchbase.lite.LogFileConfiguration
import com.couchbase.lite.LogLevel
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.RawData
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher
import com.couchbase.mobiletestkit.javacommon.util.Log
import com.couchbase.mobiletestkit.javacommon.util.ZipUtils
import java.io.File

class LoggingRequestHandler {
    /* ----------- */ /* - Logging - */ /* ----------- */
    fun configure(args: Args): LogFileConfiguration {
        val log_level = args.get<String>("log_level")
        var directory = args.get<String>("directory")
        val maxRotateCount = args.get<Int>("max_rotate_count")
        val maxSize = args.get<Long>("max_size")
        val plainText = args.get<Boolean>("plain_text")
        if (directory.isEmpty()) {
            val ts = System.currentTimeMillis() / 1000
            directory =
                RequestHandlerDispatcher.context.filesDir.absolutePath + File.separator + "logs_" + ts
            Log.i(
                TAG,
                "File logging configured at: $directory"
            )
        }
        val config = LogFileConfiguration(directory)
        if (maxRotateCount > 1) {
            config.maxRotateCount = maxRotateCount
        }
        if (maxSize > 512000) {
            config.maxSize = maxSize
        }
        config.setUsePlaintext(plainText)
        Database.log.file.config = config
        when (log_level) {
            "debug" -> Database.log.file.level = LogLevel.DEBUG
            "verbose" -> Database.log.file.level = LogLevel.VERBOSE
            "info" -> Database.log.file.level = LogLevel.INFO
            "error" -> Database.log.file.level = LogLevel.ERROR
            "warning" -> Database.log.file.level = LogLevel.WARNING
            else -> Database.log.file.level = LogLevel.NONE
        }
        return config
    }

    fun getPlainTextStatus(args: Args?): Boolean {
        return Database.log.file.config!!.usesPlaintext()
    }

    fun getMaxRotateCount(args: Args?): Int {
        return Database.log.file.config!!.maxRotateCount
    }

    fun getMaxSize(args: Args?): Long {
        return Database.log.file.config!!.maxSize
    }

    fun getDirectory(args: Args?): String {
        return Database.log.file.config!!.directory
    }

    fun getConfig(args: Args?): LogFileConfiguration? {
        return Database.log.file.config
    }

    fun setPlainTextStatus(args: Args): LogFileConfiguration {
        val config = args.get<LogFileConfiguration>("config")
        val plain_text = args.get<Boolean>("plain_text")
        config.setUsePlaintext(plain_text)
        return config
    }

    fun setMaxRotateCount(args: Args): LogFileConfiguration {
        val config = args.get<LogFileConfiguration>("config")
        val max_rotate_count = args.get<Int>("max_rotate_count")
        config.maxRotateCount = max_rotate_count
        return config
    }

    fun setMaxSize(args: Args): LogFileConfiguration {
        val config = args.get<LogFileConfiguration>("config")
        val max_size = args.get<Long>("max_size")
        config.maxSize = max_size
        return config
    }

    fun setConfig(args: Args): LogFileConfiguration {
        var directory = args.get<String>("directory")
        if (directory.isEmpty()) {
            val ts = System.currentTimeMillis() / 1000
            directory =
                RequestHandlerDispatcher.context.filesDir.absolutePath + File.separator + "logs_" + ts
            Log.i(
                TAG,
                "File logging configured at: $directory"
            )
        }
        val config = LogFileConfiguration(directory)
        Database.log.file.config = config
        return config
    }

    fun getLogLevel(args: Args?): Int {
        return Database.log.file.level.ordinal
    }

    fun setLogLevel(args: Args): LogFileConfiguration {
        val config = args.get<LogFileConfiguration>("config")
        val log_level = args.get<String>("log_level")
        when (log_level) {
            "debug" -> Database.log.file.level = LogLevel.DEBUG
            "verbose" -> Database.log.file.level = LogLevel.VERBOSE
            "info" -> Database.log.file.level = LogLevel.INFO
            "error" -> Database.log.file.level = LogLevel.ERROR
            "warning" -> Database.log.file.level = LogLevel.WARNING
            else -> Database.log.file.level = LogLevel.NONE
        }
        return config
    }

    fun getLogsInZip(args: Args?): RawData? {
        val fileLoggerConfig = Database.log.file.config ?: return null
        val zipper = ZipUtils()
        val zipDir = RequestHandlerDispatcher.context.getExternalFilesDir("zip")
        return try {
            val zipFile = File(zipDir, "archive.zip")
            if (zipFile.exists()) {
                zipper.deleteRecursive(zipFile)
            }
            zipper.zipDirectory(fileLoggerConfig.directory, zipFile)
            RawData("application/zip", zipper.readFile(zipFile))
        } finally {
            zipper.deleteRecursive(zipDir)
        }
    }

    companion object {
        private const val TAG = "LOGREQHANDLER"
    }
}