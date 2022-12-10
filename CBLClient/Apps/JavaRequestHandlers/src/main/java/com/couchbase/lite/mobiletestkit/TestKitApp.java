//
// Copyright (c) 2022 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.lite.mobiletestkit;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.KeyStoreUtils;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.TLSIdentity;
import com.couchbase.lite.mobiletestkit.util.Log;


public abstract class TestKitApp {
    private static final String TAG = "TESTKIT";

    private static final AtomicReference<TestKitApp> APP = new AtomicReference<>();

    public static void init(@NonNull TestKitApp app) {
        if (!APP.compareAndSet(null, app)) { throw new IllegalStateException("Attempt to re-initialize the Test App"); }
        app.init();
    }

    @NonNull
    public static TestKitApp getApp() {
        final TestKitApp app = APP.get();
        if (app == null) { throw new IllegalStateException("Test App has not been initialized"); }
        return app;
    }


    private final Dispatcher dispatcher = new Dispatcher();

    protected abstract void initCBL();

    public abstract String getPlatform();

    public abstract File getFilesDir();

    public abstract String encodeBase64(byte[] hashBytes);

    public abstract byte[] decodeBase64(String encodedBytes);

    public Dispatcher getDispatcher() { return dispatcher; }

    public String getLocalIpAddress() {
        try {
            for (NetworkInterface intf: Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String intf_name = intf.getName();
                Log.d(TAG, "intf_name: " + intf_name);
                for (InetAddress inetAddress: Collections.list(intf.getInetAddresses())) {
                    if (!inetAddress.isLoopbackAddress()
                        && (inetAddress instanceof Inet4Address)
                        && (intf_name.equals("eth1") || intf_name.equals("wlan0"))) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }
        catch (java.net.SocketException e) { Log.w(TAG, "Failed getting device IP address", e); }
        return "unknown";
    }

    public InputStream getAsset(String name) { return getClass().getResourceAsStream("/" + name); }

    public TLSIdentity getCreateIdentity() throws CouchbaseLiteException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 2);

        HashMap<String, String> X509Attributes = new HashMap<>();
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_COMMON_NAME, "CBL Test");
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION, "Couchbase");
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION_UNIT, "Mobile");
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_EMAIL_ADDRESS, "lite@couchbase.com");
        return TLSIdentity.createIdentity(
            true,
            X509Attributes,
            calendar.getTime(),
            UUID.randomUUID().toString());
    }

    public TLSIdentity getSelfSignedIdentity()
        throws UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException,
        IOException, CouchbaseLiteException {
        InputStream serverCert = getAsset("certs.p12");
        KeyStoreUtils.importEntry(
            "PKCS12",
            serverCert,
            "123456".toCharArray(),
            "testkit",
            "123456".toCharArray(),
            "Servercerts");
        return TLSIdentity.getIdentity("Servercerts");
    }

    public List<Certificate> getAuthenticatorCertsList() throws CertificateException, IOException {
        List<Certificate> certsList = new ArrayList<>();

        try (InputStream serverCert = getAsset("client-ca.der")) {
            certsList.add(CertificateFactory.getInstance("X.509").generateCertificate(serverCert));
        }

        return certsList;
    }

    public TLSIdentity getClientCertsIdentity()
        throws IOException, UnrecoverableEntryException, CertificateException, KeyStoreException,
        NoSuchAlgorithmException, CouchbaseLiteException {
        try (InputStream clientCert = getAsset("client.p12")) {
            KeyStoreUtils.importEntry("PKCS12",
                clientCert,
                "123456".toCharArray(),
                "testkit",
                "123456".toCharArray(), "ClientCertsSelfsigned");

            return TLSIdentity.getIdentity("ClientCertsSelfsigned");
        }
    }

    private void init() {
        initCBL();

        Database.log.getConsole().setLevel(LogLevel.DEBUG);
        Database.log.getConsole().setDomains(LogDomain.ALL_DOMAINS);

        // The dispatcher is down here because it probably takes it a while to initialize.
        // Do it early, before showing the UI...
        dispatcher.init();
    }
}
