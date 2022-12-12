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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.couchbase.lite.Database;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.TLSIdentity;


public abstract class TestKitApp {
    protected static final String TAG = "TESTKIT";

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

    public abstract String getLocalIpAddress();

    public abstract TLSIdentity getCreateIdentity() throws Exception;

    public abstract TLSIdentity getSelfSignedIdentity() throws Exception;

    public abstract TLSIdentity getClientCertsIdentity() throws Exception;

    public InputStream getAsset(String name) { return TestKitApp.class.getResourceAsStream("/" + name); }

    public Dispatcher getDispatcher() { return dispatcher; }

    public List<Certificate> getAuthenticatorCertsList() throws CertificateException, IOException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

        List<Certificate> certsList = new ArrayList<>();
        try (InputStream cert = getAsset("client-ca.der")) {
            certsList.add(certFactory.generateCertificate(cert));
        }

        return certsList;
    }

    protected final Date getExpirationTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 2);
        return calendar.getTime();
    }

    protected final HashMap<String, String> getX509Attributes() {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put(TLSIdentity.CERT_ATTRIBUTE_COMMON_NAME, "CBL Test");
        attributes.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION, "Couchbase");
        attributes.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION_UNIT, "Mobile");
        attributes.put(TLSIdentity.CERT_ATTRIBUTE_EMAIL_ADDRESS, "lite@couchbase.com");
        return attributes;
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
