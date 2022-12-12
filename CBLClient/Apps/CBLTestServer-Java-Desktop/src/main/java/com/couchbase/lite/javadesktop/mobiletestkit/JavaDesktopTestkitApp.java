package com.couchbase.lite.javadesktop.mobiletestkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.TLSIdentity;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.Log;


public class JavaDesktopTestkitApp extends TestKitApp {
    private final File directory;

    public JavaDesktopTestkitApp() {
        directory = new File(System.getProperty("java.io.tmpdir"), "TestServerTemp");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalStateException("Cannot create tmp directory: " + directory);
            }
        }
    }

    @Override
    protected void initCBL() { CouchbaseLite.init(true); }

    @Override
    public String getPlatform() { return "java"; }

    @Override
    public File getFilesDir() { return this.directory; }

    @Override
    public String encodeBase64(byte[] hashBytes) { return Base64.getEncoder().encodeToString(hashBytes); }

    @Override
    public byte[] decodeBase64(String encodedBytes) { return Base64.getDecoder().decode(encodedBytes); }

    @Override
    public String getAppId() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface: Collections.list(nets)) {
                if (iface.getName().equals("en0")) {
                    String ip = getIpAddressByInterface(iface);
                    if (ip != null) { return ip; }
                }
                if (iface.getName().equals("en1")) {
                    String ip = getIpAddressByInterface(iface);
                    if (ip != null) { return ip; }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (SocketException | UnknownHostException e) { Log.w(TAG, "Failed getting device IP address", e); }
        return "unknown";
    }

    @Override
    public TLSIdentity getCreateIdentity()
        throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, CouchbaseLiteException {
        KeyStore externalStore = KeyStore.getInstance("PKCS12");
        externalStore.load(null, null);

        return TLSIdentity.createIdentity(
            true,
            getX509Attributes(),
            getExpirationTime(),
            externalStore,
            UUID.randomUUID().toString(),
            "pass".toCharArray()
        );
    }

    @Override
    public TLSIdentity getSelfSignedIdentity()
        throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException,
        UnrecoverableEntryException, CouchbaseLiteException {
        char[] pass = "123456".toCharArray();

        try (InputStream ServerCert = getAsset("certs.p12")) {
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);
            trustStore.load(ServerCert, pass);

            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pass);
            KeyStore.Entry newEntry = trustStore.getEntry("testkit", protParam);
            trustStore.setEntry("Servercerts", newEntry, protParam);

            return TLSIdentity.getIdentity(trustStore, "Servercerts", pass);
        }
    }

    @Override
    public TLSIdentity getClientCertsIdentity()
        throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException,
        UnrecoverableEntryException, CouchbaseLiteException {
        char[] pass = "123456".toCharArray();

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);

        try (InputStream cert = getAsset("client.p12")) { trustStore.load(cert, pass); }

        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pass);
        trustStore.setEntry("Clientcerts", trustStore.getEntry("testkit", protParam), protParam);

        return TLSIdentity.getIdentity(trustStore, "Clientcerts", pass);
    }

    private String getIpAddressByInterface(NetworkInterface networkInterface) {
        for (InetAddress address: Collections.list(networkInterface.getInetAddresses())) {
            if (address instanceof Inet4Address) { return address.getHostAddress(); }
        }
        return null;
    }
}