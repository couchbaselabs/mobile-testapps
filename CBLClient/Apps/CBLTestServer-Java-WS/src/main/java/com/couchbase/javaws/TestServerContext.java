package com.couchbase.javaws;

import com.couchbase.mobiletestkit.javacommon.Context;

import javax.servlet.ServletRequest;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.security.cert.X509Certificate;

public class TestServerContext implements Context {
    private ServletRequest servletRequest;

    public static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    public TestServerContext(ServletRequest request){
        this.servletRequest = request;
    }

    @Override
    public InputStream getAsset(String name) {
        return getClass().getResourceAsStream("/" + name);
    }

    @Override
    public String getPlatform() {
        return "java-ws";
    }

    @Override
    public File getFilesDir() {
        /*
        this function to provide temp directory location
        where CouchbaseLite database files and log files will be stored
        in Tomcat structure, this points to %CATALINE_HOME%/temp/
         */
        File temp_dir = new File(TMP_DIR, "TestServerTemp");
        if(!temp_dir.exists()){
            try {
                temp_dir.mkdir();
            }catch(SecurityException se){
                throw se;
            }
        }

        return temp_dir;
    }

    @Override
    public File getExternalFilesDir(String filetype) {
        /*
        this function to provide a location
        that zipped log archive file will temporarily be stored
        in Tomcat structure, this points to %CATALINE_HOME%/temp/TestServerTemp/
        and a subdirectory with the value of filetype will be created under
         */
        File externalFilesDir = new File(getFilesDir().getAbsoluteFile(), filetype);
        if(!externalFilesDir.exists()){
            try {
                externalFilesDir.mkdir();
            }catch(SecurityException se){
                throw se;
            }
        }
        return externalFilesDir;
    }

    @Override
    public String getLocalIpAddress() {
        return servletRequest.getServerName();
    }

    @Override
    public String encodeBase64(byte[] hashBytes){
        // load java.util.Base64 in java webservice app
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    @Override
    public TLSIdentity getCreateIdentity() {
        final String EXTERNAL_KEY_STORE_TYPE = "PKCS12";
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        Date certTime = calendar.getTime();
        HashMap<String, String> X509Attributes = new HashMap<String, String>();
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_COMMON_NAME, "CBL Test");
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION, "Couchbase");
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION_UNIT, "Mobile");
        X509Attributes.put(TLSIdentity.CERT_ATTRIBUTE_EMAIL_ADDRESS, "lite@couchbase.com");
        KeyStore externalStore = KeyStore.getInstance(EXTERNAL_KEY_STORE_TYPE);
        externalStore.load(null, null);
        String alias = UUID.randomUUID().toString();
        TLSIdentity identity = TLSIdentity.createIdentity(true,
                    X509Attributes,
                    certTime,
                    externalStore,
                    alias,
                    "pass".toCharArray()
                    );
        return identity
    }

    @Override
    public TLSIdentity getSelfSignedIdentity() {
        try (InputStream ServerCert = this.getCertFile("certs.p12")) {
            char[] pass = "123456".toCharArray();
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null,null);
            trustStore.load(ServerCert, pass);
                KeyStore.ProtectionParameter protParam =
                        new KeyStore.PasswordProtection(pass);
                KeyStore.Entry newEntry = trustStore.getEntry("testkit", protParam);
                trustStore.setEntry("Servercerts", newEntry, protParam);
                TLSIdentity identity = TLSIdentity.getIdentity(trustStore, "Servercerts", pass);
        return identity
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
        }
        return certsList
    }

    @Override
    public TLSIdentity getClientCertsIdentity() {
        try (InputStream ClientCert = this.getCertFile("client.p12")) {
            char[] pass = "123456".toCharArray();
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null,null);
            trustStore.load(ClientCert, pass);
            KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pass);
            KeyStore.Entry newEntry = trustStore.getEntry("testkit", protParam);
            trustStore.setEntry("Clientcerts", newEntry, protParam);
            TLSIdentity identity = TLSIdentity.getIdentity(trustStore, "Clientcerts", pass);

            } catch (KeyStoreException e) {
                    e.printStackTrace();
            } catch (CertificateException e) {
                    e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
            }
        return identity
    }

    private InputStream getCertFile(String fileName) {
        InputStream is = null;
        try {
            is = RequestHandlerDispatcher.context.getAsset(fileName);
            return is;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is;
    }


}
