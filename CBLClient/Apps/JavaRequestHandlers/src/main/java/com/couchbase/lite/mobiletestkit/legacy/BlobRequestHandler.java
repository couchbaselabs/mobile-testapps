package com.couchbase.lite.mobiletestkit.legacy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.couchbase.lite.Blob;
import com.couchbase.lite.internal.fleece.FLEncoder;
import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.FileUtils;


public class BlobRequestHandler {

    public Blob create(Args args) throws IOException {
        String contentType = args.getString("contentType");

        byte[] content = args.getData("content");
        if (content != null) { return new Blob(contentType, content); }

        InputStream stream = args.get("stream", InputStream.class);
        if (stream != null) { return new Blob(contentType, stream); }

        URL fileURL = args.get("fileURL", URL.class);
        if (fileURL != null) { return new Blob(contentType, fileURL); }

        throw new IOException("Incorrect parameters provided");
    }

    public byte[] createUTFBytesContent(Args args) {
        String content = args.getString("content");
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public InputStream createImageStream(Args args) throws IOException {
        String filePath = args.getString("image");
        if (filePath == null || filePath.isEmpty()) {
            throw new IOException("Image content file path cannot be null");
        }

        return TestKitApp.getApp().getAsset(filePath);
    }

    public byte[] createImageContent(Args args) throws IOException {
        String filePath = args.getString("image");
        if (filePath == null || filePath.isEmpty()) {
            throw new IOException("Image content file path cannot be null");
        }

        InputStream stream = TestKitApp.getApp().getAsset(filePath);
        byte[] targetArray = new byte[stream.available()];
        stream.read(targetArray);

        return targetArray;
    }

    public URL createImageFileUrl(Args args) throws IOException {
        String filePath = args.getString("image");
        if (filePath == null || filePath.isEmpty()) {
            throw new IOException("Image content file path cannot be null");
        }

        InputStream stream = TestKitApp.getApp().getAsset(filePath);

        String directory = TestKitApp.getApp().getFilesDir().getAbsolutePath();
        File targetFile = new File(directory, filePath);
        OutputStream outStream = new FileOutputStream(targetFile);
        FileUtils utils = new FileUtils();
        utils.copyStream(stream, outStream);

        return targetFile.toURI().toURL();
    }

    public String digest(Args args) {
        return args.get("blob", Blob.class).digest();
    }

    public void encodeTo(Args args) {
        args.get("blob", Blob.class).encodeTo(args.get("encoder", FLEncoder.class));
    }

    public Boolean equals(Args args) {
        return args.get("blob", Blob.class).equals(args.get("obj", Blob.class));
    }

    public int hashCode(Args args) {
        return args.get("blob", Blob.class).hashCode();
    }

    public byte[] getContent(Args args) {
        return args.get("blob", Blob.class).getContent();
    }

    public Map<String, Object> getProperties(Args args) {
        return args.get("blob", Blob.class).getProperties();
    }

    public InputStream getContentStream(Args args) {
        return args.get("blob", Blob.class).getContentStream();
    }

    public String getContentType(Args args) {
        return args.get("blob", Blob.class).getContentType();
    }

    public long length(Args args) {
        return args.get("blob", Blob.class).length();
    }

    public String toString(Args args) {
        return args.getString("blob");
    }
}
