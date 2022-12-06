/*
  Created by sridevi.saragadam on 11/14/18.
 */
package com.couchbase.mobiletestkit.javacommon.util;

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

import com.couchbase.CouchbaseLiteServ.util.Log;


public class FileUtils {
    private static final String TAG = "ZIP";

    private final byte[] buffer = new byte[1024];

    public void unzip(@NonNull InputStream in, @NonNull File dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                File newFile = new File(dest, ze.getName());
                if (ze.isDirectory()) { newFile.mkdirs(); }
                else {
                    final String parent = newFile.getParent();
                    if (parent != null) { new File(parent).mkdirs(); }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) { copyFile(zis, fos); }
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        finally {
            try { in.close(); }
            catch (IOException ignore) { }
        }
    }

    public void zipDirectory(@NonNull String srcDirPath, @NonNull File zipFile) throws IOException {
        File srcDir = new File(srcDirPath);
        String srcName = srcDir.getName();

        List<String> zipFiles = new ArrayList<>();
        getFilesList(srcDir, zipFiles);

        int prefixLen = srcDir.getAbsolutePath().length() + 1;
        try (FileOutputStream fos = new FileOutputStream(zipFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String path: zipFiles) {
                try (FileInputStream fis = new FileInputStream(path)) {
                    zos.putNextEntry(new ZipEntry(srcName + "/" + path.substring(prefixLen)));
                    copyFile(fis, zos);
                }
                finally {
                    try { zos.closeEntry(); } catch (IOException ignore) { }
                }
            }
        }
    }

    public byte[] toByteArray(InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            int bytesRead = is.read(buffer);
            while (bytesRead != -1) {
                bos.write(buffer, 0, bytesRead);
                bytesRead = is.read(buffer);
            }
        }
        catch (IOException err) {
            Log.w(TAG, "Ignoring exception while copying stream to memory", err);
        }

        return bos.toByteArray();
    }

    public byte[] readFile(@NonNull File srcFile) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (FileInputStream in = new FileInputStream(srcFile)) { copyFile(in, out); }
        return out.toByteArray();
    }

    public void copyFolder(@NonNull File src, @NonNull File dest) throws IOException {
        if (!src.isDirectory()) {
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
                copyFile(in, out);
            }
            Log.d(TAG, "File copied from " + src + " to " + dest);
        }
        else {
            if (dest.exists()) { deleteRecursive(dest); }
            dest.mkdir();

            String[] files = src.list();
            if (files != null) {
                for (String file: files) { copyFolder(new File(src, file), new File(dest, file)); }
            }

            Log.d(TAG, "Directory copied from " + src + "  to " + dest);
        }

        deleteRecursive(src);
    }

    public boolean deleteRecursive(@NonNull File fileOrDirectory) {
        return (!fileOrDirectory.exists()) || (deleteContents(fileOrDirectory) && fileOrDirectory.delete());
    }

    public void copyFile(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
        int read;
        while ((read = in.read(buffer)) != -1) { out.write(buffer, 0, read); }
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
                Log.d("FILE UTILS", "Failed deleting file: " + file);
                succeeded = false;
            }
        }

        return succeeded;
    }
}
