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
package com.couchbase.lite.android.mobiletestkit;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;

import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.KeyStoreUtils;
import com.couchbase.lite.TLSIdentity;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.Log;


public class AndroidTestKitApp extends TestKitApp {

    private final Context context;

    public AndroidTestKitApp(Context context) { this.context = context.getApplicationContext(); }

    @Override
    protected void initCBL() { CouchbaseLite.init(context, true); }

    @Override
    public String getPlatform() { return "android"; }

    @Override
    public File getFilesDir() { return context.getFilesDir(); }

    @Override
    public String encodeBase64(byte[] hashBytes) { return Base64.encodeToString(hashBytes, Base64.NO_WRAP); }

    @Override
    public byte[] decodeBase64(String encodedBytes) { return Base64.decode(encodedBytes, Base64.NO_WRAP); }

    @Override
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

    @Override
    public TLSIdentity getCreateIdentity() throws CouchbaseLiteException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 2);

        return TLSIdentity.createIdentity(
            true,
            getX509Attributes(),
            getExpirationTime(),
            UUID.randomUUID().toString());
    }

    @Override
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

    @Override
    public TLSIdentity getClientCertsIdentity()
        throws IOException, UnrecoverableEntryException, CertificateException, KeyStoreException,
        NoSuchAlgorithmException, CouchbaseLiteException {
        char[] pass = "123456".toCharArray();
        try (InputStream clientCert = getAsset("client.p12")) {
            KeyStoreUtils.importEntry(
                "PKCS12",
                clientCert,
                pass,
                "testkit",
                pass,
                "ClientCertsSelfsigned");
        }
        return TLSIdentity.getIdentity("ClientCertsSelfsigned");
    }
}
