/*
  Created by sridevi.saragadam on 11/14/18.
 */
package com.couchbase.lite.mobiletestkit.util;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class FileUtils {
    private static final String TAG = "FILE_UTIL";

    private final byte[] buffer = new byte[1024];

    public byte[] readToByteArray(InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try { copyStream(is, bos); }
        catch (IOException err) { Log.w(TAG, "Ignoring exception while copying stream to memory", err); }
        return bos.toByteArray();
    }

    public byte[] readToByteArray(@NonNull File srcFile) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(srcFile)) { copyStream(in, out); }
        return out.toByteArray();
    }

    public void copyStream(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        int read;
        while ((read = in.read(buffer)) != -1) { out.write(buffer, 0, read); }
    }

    public void moveFileOrDir(@NonNull File src, @NonNull File dst) throws IOException {
        if (!src.exists()) { return; }

        if (dst.exists()) { deleteRecursive(dst); }

        copyFileOrDir(src, dst);

        deleteRecursive(src);
    }

    public boolean deleteRecursive(@NonNull File fileOrDirectory) {
        return (!fileOrDirectory.exists()) || (deleteContents(fileOrDirectory) && fileOrDirectory.delete());
    }

    public void zipDirectory(@NonNull File srcDir, @NonNull File zipFile) throws IOException {
        final List<String> zipFiles = new ArrayList<>();
        getFilesList(srcDir, zipFiles);

        final int prefixLen = srcDir.getAbsolutePath().length() + 1;
        final String srcName = srcDir.getName();
        try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String path: zipFiles) {
                try (FileInputStream fis = new FileInputStream(path)) {
                    zos.putNextEntry(new ZipEntry(srcName + "/" + path.substring(prefixLen)));
                    copyStream(fis, zos);
                }
                finally {
                    try { zos.closeEntry(); } catch (IOException ignore) { }
                }
                Log.d(TAG, "Zipped file " + path);
            }
        }
    }

    public void unzip(@NonNull InputStream in, @NonNull File dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                File newFile = new File(dest, ze.getName());
                if (ze.isDirectory()) { newFile.mkdirs(); }
                else {
                    final String parent = newFile.getParent();
                    if (parent != null) { new File(parent).mkdirs(); }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) { copyStream(zis, fos); }
                }
                Log.d(TAG, "Unzipped file " + newFile);
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        finally {
            try { in.close(); }
            catch (IOException ignore) { }
        }
    }

    private void getFilesList(@NonNull File dir, @NonNull List<String> files) {
        File[] contents = dir.listFiles();
        if (contents == null) { return; }
        for (File file: contents) {
            if (file.isDirectory()) { getFilesList(file, files); }
            else { files.add(file.getAbsolutePath()); }
        }
    }

    private boolean deleteContents(File fileOrDirectory) {
        if ((fileOrDirectory == null) || (!fileOrDirectory.isDirectory())) { return true; }

        final File[] contents = fileOrDirectory.listFiles();
        if (contents == null) { return true; }

        boolean succeeded = true;
        for (File file: contents) {
            if (!deleteRecursive(file)) {
                Log.d(TAG, "Failed deleting file: " + file);
                succeeded = false;
            }
        }

        return succeeded;
    }

    private void copyFileOrDir(@NonNull File src, @NonNull File dst) throws IOException {
        if (!src.isDirectory()) {
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                copyStream(in, out);
            }
            Log.d(TAG, "File copied from " + src + " to " + dst);
            return;
        }

        if (!dst.mkdir()) { throw new IOException("Failed creating directory: " + dst); }

        String[] files = src.list();
        if (files != null) {
            for (String file: files) { copyFileOrDir(new File(src, file), new File(dst, file)); }
        }
        Log.d(TAG, "Directory copied from " + src + "  to " + dst);
    }
}

