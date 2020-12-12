package com.couchbase.mobiletestkit.javatestserver;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.mobiletestkit.javacommon.Context;
import com.couchbase.mobiletestkit.javacommon.util.Log;

import java.io.File;
import java.io.InputStream;
import java.net.*;
import java.security.UnrecoverableEntryException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.io.*;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;


public class TestServerContext implements Context {
    private final String TAG = "JavaContext";
    private File directory;

    public TestServerContext(File directory) {
        this.directory = directory;
    }

    @Override
    public File getFilesDir() {
        return this.directory;
    }

    @Override
    public File getExternalFilesDir(String filetype) {
        File externalFilesDir = new File(this.directory.getAbsolutePath(), filetype);
        externalFilesDir.mkdir();

        return externalFilesDir;
    }


    @Override
    public InputStream getAsset(String name) {
        return TestServerMain.class.getResourceAsStream("/" + name);
    }

    @Override
    public String getPlatform() {
        return "java";
    }

    @Override
    public String getLocalIpAddress() {
        String ip = null;

        NetworkInterface en0 = null;
        NetworkInterface eth1 = null;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if (en0 == null && netint.getName().equals("en0")) {
                    en0 = netint;
                    continue;
                }
                if (eth1 == null && netint.getName().equals("eth1")) {
                    eth1 = netint;
                    continue;
                }
            }

            if (eth1 != null) {
                ip = getIpAddressByInterface(eth1);
                if (!ip.isEmpty()) {
                    return ip;
                }
            }

            if (en0 != null) {
                ip = getIpAddressByInterface(en0);
                if (!ip.isEmpty()) {
                    return ip;
                }
            }

            if (ip == null || ip.isEmpty()) {
                ip = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (SocketException socketEx) {
            Log.e(TAG, "SocketException: ", socketEx);
        } catch (UnknownHostException ex) {
            Log.e(TAG, "UnknownHostException: ", ex);
        }

        return ip;
    }

    @Override
    public String encodeBase64(byte[] hashBytes) {
        // load java.util.Base64 in java standalone app
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    @Override
    public List<Certificate> getAuthenticatorCertsList() {
        List<Certificate> certsList = new ArrayList<>();
        try (InputStream ClientCert = this.getCertFile("client-ca.der")) {
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate cert;
                cert = cf.generateCertificate(new BufferedInputStream(ClientCert));
                certsList.add(cert);
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return certsList;
    }

    private InputStream getCertFile(String fileName) {
        InputStream is = null;
        try {
            is = getAsset(fileName);
            return is;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }

    private String getIpAddressByInterface(NetworkInterface networkInterface) {
        String ip = "";

        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        for (InetAddress address : Collections.list(inetAddresses)) {
            if (address instanceof Inet4Address) {
                // currently support ipv4 address
                ip = address.getHostAddress();
                return ip;
            } else if (address instanceof Inet6Address) {
                // do nothing for ipv6 address now, may need for future
            }
        }
        return ip;
    }
}