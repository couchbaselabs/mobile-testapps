package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.Blob
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher
import com.couchbase.mobiletestkit.javacommon.util.ZipUtils
import java.io.*
import java.net.MalformedURLException
import java.net.URL

class BlobRequestHandler {
    @Throws(IOException::class)
    fun create(args: Args): Blob {
        val contentType = args.get<String>("contentType")
        val content = args.get<ByteArray>("content")
        if (content != null) {
            return Blob(contentType, content)
        }
        val stream = args.get<InputStream>("stream")
        if (stream != null) {
            return Blob(contentType, stream)
        }
        val fileURL = args.get<URL>("fileURL")
        if (fileURL != null) {
            return Blob(contentType, fileURL)
        }
        throw IOException("Incorrect parameters provided")
    }

    @Throws(IOException::class)
    fun createImageStream(args: Args): InputStream {
        val filePath = args.get<String>("image")
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Image content file path cannot be null")
        }
        return RequestHandlerDispatcher.context.getAsset(filePath)
    }

    @Throws(IOException::class)
    fun createImageContent(args: Args): ByteArray {
        val filePath = args.get<String>("image")
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Image content file path cannot be null")
        }
        val stream = RequestHandlerDispatcher.context.getAsset(filePath)
        val targetArray = ByteArray(stream.available())
        stream.read(targetArray)
        return targetArray
    }

    @Throws(IOException::class, MalformedURLException::class)
    fun createImageFileUrl(args: Args): URL {
        val filePath = args.get<String>("image")
        if (filePath == null || filePath.isEmpty()) {
            throw IOException("Image content file path cannot be null")
        }
        val stream = RequestHandlerDispatcher.context.getAsset(filePath)
        val directory = RequestHandlerDispatcher.context.filesDir.absolutePath
        val targetFile = File(directory, filePath)
        val outStream: OutputStream = FileOutputStream(targetFile)
        val utils = ZipUtils()
        utils.copyFile(stream, outStream)
        return targetFile.toURI().toURL()
    }

    fun digest(args: Args): String? {
        return (args.get<Any>("blob") as Blob).digest()
    }

    fun encodeTo(args: Args) {
        (args.get<Any>("blob") as Blob).encodeTo(args.get("encoder"))
    }

    fun equals(args: Args): Boolean {
        return args.get<Any>("blob") == args.get("obj")
    }

    fun hashCode(args: Args): Int {
        return (args.get<Any>("blob") as Blob).hashCode()
    }

    fun getContent(args: Args): ByteArray? {
        return (args.get<Any>("blob") as Blob).content
    }

    fun getProperties(args: Args): Map<String, Any> {
        return (args.get<Any>("blob") as Blob).properties
    }

    fun getContentStream(args: Args): InputStream? {
        return (args.get<Any>("blob") as Blob).contentStream
    }

    fun getContentType(args: Args): String {
        return (args.get<Any>("blob") as Blob).contentType
    }

    fun length(args: Args): Long {
        return (args.get<Any>("blob") as Blob).length()
    }

    fun toString(args: Args): String {
        return args.get<Any>("blob").toString()
    }
}