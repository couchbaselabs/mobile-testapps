//
// Copyright (c) 2019 Couchbase, Inc All rights reserved.
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
package com.couchbase.CouchbaseLiteServ;

import android.app.Application;
import android.util.Base64;

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

import com.couchbase.CouchbaseLiteServ.util.Log;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.KeyStoreUtils;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.TLSIdentity;
import com.couchbase.mobiletestkit.javacommon.Dispatcher;


public class TestServerApp extends Application {
    private static final String TAG = "APP";

    private static TestServerApp app;

    public static TestServerApp getApp() { return app; }


    private Dispatcher dispatcher;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        CouchbaseLite.init(this, true);

        Database.log.getConsole().setLevel(LogLevel.DEBUG);
        Database.log.getConsole().setDomains(LogDomain.ALL_DOMAINS);

        // This is down here because it probably takes it a while to run.
        // Do it before showing the activity...
        dispatcher = new Dispatcher();
        dispatcher.init();
    }

    public String getPlatform() { return "android"; }

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

    public Dispatcher getDispatcher() { return dispatcher; }

    public String encodeBase64(byte[] hashBytes) { return Base64.encodeToString(hashBytes, Base64.NO_WRAP); }

    public byte[] decodeBase64(String encodedBytes) { return Base64.decode(encodedBytes, Base64.NO_WRAP); }

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
        InputStream serverCert = this.getCertFile("certs.p12");
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

        try (InputStream serverCert = this.getCertFile("client-ca.der")) {
            certsList.add(CertificateFactory.getInstance("X.509").generateCertificate(serverCert));
        }

        return certsList;
    }

    public TLSIdentity getClientCertsIdentity()
        throws IOException, UnrecoverableEntryException, CertificateException, KeyStoreException,
        NoSuchAlgorithmException, CouchbaseLiteException {
        try (InputStream clientCert = this.getCertFile("client.p12")) {
            KeyStoreUtils.importEntry("PKCS12",
                clientCert,
                "123456".toCharArray(),
                "testkit",
                "123456".toCharArray(), "ClientCertsSelfsigned");

            return TLSIdentity.getIdentity("ClientCertsSelfsigned");
        }
    }

    private InputStream getCertFile(String name) { return getAsset(name); }
}
