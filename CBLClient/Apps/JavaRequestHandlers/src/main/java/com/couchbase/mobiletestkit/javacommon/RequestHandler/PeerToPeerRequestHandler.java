package com.couchbase.mobiletestkit.javacommon.RequestHandler;

/*
  Created by sridevi.saragadam on 7/9/18.
 */

import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.couchbase.lite.ClientCertificateAuthenticator;
import com.couchbase.lite.ConflictResolver;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.ListenerAuthenticator;
import com.couchbase.lite.ListenerCertificateAuthenticator;
import com.couchbase.lite.TLSIdentity;
import com.couchbase.lite.URLEndpointListener;
import com.couchbase.lite.URLEndpointListenerConfiguration;
import com.couchbase.lite.*;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher;
import com.couchbase.mobiletestkit.javacommon.util.Log;


public class PeerToPeerRequestHandler implements MessageEndpointDelegate {
    private static final String TAG = "P2PHANDLER";

    final ReplicatorRequestHandler replicatorRequestHandlerObj = new ReplicatorRequestHandler();

    public void clientStart(Args args) {
        Replicator replicator = args.get("replicator");
        replicator.start();
        Log.i(TAG, "Replication started .... ");
    }
    public Replicator configureCollection(Args args) throws Exception {
        String ipaddress = args.get("host");
        int port = args.get("port");
        String serverDBName = args.get("serverDBName");
        ArrayList<Collection> collections = args.get("collections");
        ArrayList<CollectionConfiguration> configuration = args.get("configuration");
        String replicationType = args.get("replicationType");
        Boolean continuous = args.get("continuous");
        String endPointType = args.get("endPointType");
        Boolean disableTls = args.get("tls_disable");
        String tlsAuthType = args.get("tls_auth_type");
        Boolean serverVerificationMode = args.get("server_verification_mode");
        Boolean tlsAuthenticator = args.get("tls_authenticator");
        String heartbeat = args.get("heartbeat");
        String maxRetries = args.get("max_retries");
        String maxTimeout = args.get("max_timeout");

        ReplicatorConfiguration config;
        Replicator replicator;
        URI uri;
        if (replicationType == null) {
            replicationType = "push_pull";
        }
        replicationType = replicationType.toLowerCase();
        ReplicatorType replType;
        if (replicationType.equals("push")) {
            replType = ReplicatorType.PUSH;
        } else if (replicationType.equals("pull")) {
            replType = ReplicatorType.PULL;
        } else {
            replType = ReplicatorType.PUSH_AND_PULL;
        }
        Log.i(TAG, "serverDBName is " + serverDBName);
        if (disableTls) {
            uri = new URI("ws://" + ipaddress + ":" + port + "/" + serverDBName);
        } else {
            uri = new URI("wss://" + ipaddress + ":" + port + "/" + serverDBName);
        }
        Set<CollectionConfiguration> collectionConfigs = new HashSet<>();
        if (collections != null) {
            if (configuration != null && configuration.size() > 1 && configuration.size() == collections.size()) {
                for (int i = 0; i < collections.size(); i++) {
                    CollectionConfiguration collConfig = (i < configuration.size())
                            ? configuration.get(i)
                            : new CollectionConfiguration(collections.get(i));
                    collectionConfigs.add(collConfig);
                }
            } else if (configuration != null && configuration.size() == 1) {
                CollectionConfiguration sharedConfig = configuration.get(0);
                for (Collection collection : collections) {
                    CollectionConfiguration collConfig = new CollectionConfiguration(collection);
                    if (sharedConfig.getChannels() != null) collConfig.setChannels(sharedConfig.getChannels());
                    if (sharedConfig.getDocumentIDs() != null) collConfig.setDocumentIDs(sharedConfig.getDocumentIDs());
                    if (sharedConfig.getPushFilter() != null) collConfig.setPushFilter(sharedConfig.getPushFilter());
                    if (sharedConfig.getPullFilter() != null) collConfig.setPullFilter(sharedConfig.getPullFilter());
                    if (sharedConfig.getConflictResolver() != null) collConfig.setConflictResolver(sharedConfig.getConflictResolver());
                    collectionConfigs.add(collConfig);
                }
            } else if (configuration == null) {
                for (Collection collection : collections) {
                    collectionConfigs.add(new CollectionConfiguration(collection));
                }
            } else {
                throw new Exception("Mismatch in number of collections and configurations");
            }
        }

        if (endPointType.equals("URLEndPoint")) {
            URLEndpoint urlEndPoint = new URLEndpoint(uri);
            config = new ReplicatorConfiguration(collectionConfigs, urlEndPoint);
        } else if (endPointType.equals("MessageEndPoint")) {
            MessageEndpoint messageEndPoint = new MessageEndpoint("p2p", uri, ProtocolType.BYTE_STREAM, this);
            config = new ReplicatorConfiguration(collectionConfigs, messageEndPoint);
        } else {
            throw new IllegalArgumentException("Incorrect EndPoint type");
        }
        config.setType(replType);
        if (continuous != null) {
            config.setContinuous(continuous);
        } else {
            config.setContinuous(false);
        }
        if (heartbeat != null && !heartbeat.trim().isEmpty()){
            config.setHeartbeat(Integer.parseInt(heartbeat));
        }

        if (maxRetries != null && !maxRetries.trim().isEmpty()) {
            config.setMaxAttempts(Integer.parseInt(maxRetries));
        }

        if (maxTimeout != null && !maxTimeout.trim().isEmpty()) {
            config.setMaxAttemptWaitTime(Integer.parseInt(maxTimeout));
        }
        if (args.get("basic_auth") != null) {
            config.setAuthenticator(args.get("basic_auth"));
        }

        if (tlsAuthType.equals("self_signed")) {
            TLSIdentity tlsIdentity = RequestHandlerDispatcher.context.getSelfSignedIdentity();
            if (tlsIdentity != null) {
                List<Certificate> certs = tlsIdentity.getCerts();
                X509Certificate cert = (X509Certificate) certs.get(0);
                config.setPinnedServerCertificate(cert.getEncoded());
                Log.i(TAG, "Pinned the certs ... .... ");
            }
        }

        if (tlsAuthenticator) {
            TLSIdentity identity = RequestHandlerDispatcher.context.getClientCertsIdentity();
            ClientCertificateAuthenticator clientCertificateAuthenticator = new ClientCertificateAuthenticator(identity);
            config.setAuthenticator(clientCertificateAuthenticator);
        }

        if (serverVerificationMode) {
            config.setAcceptOnlySelfSignedServerCertificate(true);
        }
        replicator = new Replicator(config);
        return replicator;
    }
    public Replicator configure(Args args) throws Exception {
        String ipaddress = args.get("host");
        int port = args.get("port");
        Database sourceDb = args.get("database");
        String serverDBName = args.get("serverDBName");
        String replicationType = args.get("replicationType");
        Boolean continuous = args.get("continuous");
        String endPointType = args.get("endPointType");
        List<String> documentIds = args.get("documentIDs");
        Boolean push_filter = args.get("push_filter");
        Boolean pull_filter = args.get("pull_filter");
        String filter_callback_func = args.get("filter_callback_func");
        String conflict_resolver = args.get("conflict_resolver");
        Boolean disableTls = args.get("tls_disable");
        String tlsAuthType = args.get("tls_auth_type");
        Boolean serverVerificationMode = args.get("server_verification_mode");
        Boolean tlsAuthenticator = args.get("tls_authenticator");
        String heartbeat = args.get("heartbeat");
        String maxRetries = args.get("max_retries");
        String maxTimeout = args.get("max_timeout");

        ReplicatorConfiguration config;
        Replicator replicator;
        URI uri;

        if (replicationType == null) {
            replicationType = "push_pull";
        }
        replicationType = replicationType.toLowerCase();
        ReplicatorType replType;
        if (replicationType.equals("push")) {
            replType = ReplicatorType.PUSH;
        } else if (replicationType.equals("pull")) {
            replType = ReplicatorType.PULL;
        } else {
            replType = ReplicatorType.PUSH_AND_PULL;
        }
        Log.i(TAG, "serverDBName is " + serverDBName);
        if (disableTls) {
            uri = new URI("ws://" + ipaddress + ":" + port + "/" + serverDBName);
        } else {
            uri = new URI("wss://" + ipaddress + ":" + port + "/" + serverDBName);
        }
        CollectionConfiguration collectionConfig = new CollectionConfiguration(sourceDb.getDefaultCollection());

        if (documentIds != null) {
            collectionConfig.setDocumentIDs(documentIds);
        }

        if (push_filter) {
            switch (filter_callback_func) {
                case "boolean":
                    collectionConfig.setPushFilter(new ReplicatorBooleanFilterCallback());
                    break;
                case "deleted":
                    collectionConfig.setPushFilter(new ReplicatorDeletedFilterCallback());
                    break;
                case "access_revoked":
                    collectionConfig.setPushFilter(new ReplicatorAccessRevokedFilterCallback());
                    break;
                default:
                    collectionConfig.setPushFilter(new DefaultReplicatorFilterCallback());
                    break;
            }
        }

        if (pull_filter) {
            switch (filter_callback_func) {
                case "boolean":
                    collectionConfig.setPullFilter(new ReplicatorBooleanFilterCallback());
                    break;
                case "deleted":
                    collectionConfig.setPullFilter(new ReplicatorDeletedFilterCallback());
                    break;
                case "access_revoked":
                    collectionConfig.setPullFilter(new ReplicatorAccessRevokedFilterCallback());
                    break;
                default:
                    collectionConfig.setPullFilter(new DefaultReplicatorFilterCallback());
                    break;
            }
        }

        switch (conflict_resolver) {
            case "local_wins":
                collectionConfig.setConflictResolver(new LocalWinsCustomConflictResolver());
                break;
            case "remote_wins":
                collectionConfig.setConflictResolver(new RemoteWinsCustomConflictResolver());
                break;
            case "null":
                collectionConfig.setConflictResolver(new NullCustomConflictResolver());
                break;
            case "merge":
                collectionConfig.setConflictResolver(new MergeCustomConflictResolver());
                break;
            case "incorrect_doc_id":
                collectionConfig.setConflictResolver(new IncorrectDocIdConflictResolver());
                break;
            case "delayed_local_win":
                collectionConfig.setConflictResolver(new DelayedLocalWinConflictResolver());
                break;
            case "delete_not_win":
                collectionConfig.setConflictResolver(new DeleteDocConflictResolver());
                break;
            case "exception_thrown":
                collectionConfig.setConflictResolver(new ExceptionThrownConflictResolver());
                break;
            default:
                collectionConfig.setConflictResolver(ConflictResolver.DEFAULT);
                break;
        }


        if (endPointType.equals("URLEndPoint")) {
            URLEndpoint urlEndPoint = new URLEndpoint(uri);
            config = new ReplicatorConfiguration(Set.of(collectionConfig), urlEndPoint);
        } else if (endPointType.equals("MessageEndPoint")) {
            MessageEndpoint messageEndPoint = new MessageEndpoint("p2p", uri, ProtocolType.BYTE_STREAM, this);
            config = new ReplicatorConfiguration(Set.of(collectionConfig), messageEndPoint);
        } else {
            throw new IllegalArgumentException("Incorrect EndPoint type");
        }
        config.setType(replType);
        if (continuous != null) {
            config.setContinuous(continuous);
        } else {
            config.setContinuous(false);
        }
        if (heartbeat != null && !heartbeat.trim().isEmpty()){
            config.setHeartbeat(Integer.parseInt(heartbeat));
        }

        if (maxRetries != null && !maxRetries.trim().isEmpty()) {
            config.setMaxAttempts(Integer.parseInt(maxRetries));
        }

        if (maxTimeout != null && !maxTimeout.trim().isEmpty()) {
            config.setMaxAttemptWaitTime(Integer.parseInt(maxTimeout));
        }

        if (args.get("basic_auth") != null) {
            config.setAuthenticator(args.get("basic_auth"));
        }

        if (tlsAuthType.equals("self_signed")) {
            TLSIdentity tlsIdentity = RequestHandlerDispatcher.context.getSelfSignedIdentity();
            if (tlsIdentity != null) {
                List<Certificate> certs = tlsIdentity.getCerts();
                X509Certificate cert = (X509Certificate) certs.get(0);
                config.setPinnedServerCertificate(cert.getEncoded());
                Log.i(TAG, "Pinned the certs ... .... ");
            }
        }

        if (tlsAuthenticator) {
            TLSIdentity identity = RequestHandlerDispatcher.context.getClientCertsIdentity();
            ClientCertificateAuthenticator clientCertificateAuthenticator = new ClientCertificateAuthenticator(identity);
            config.setAuthenticator(clientCertificateAuthenticator);
        }

        if (serverVerificationMode) {
            config.setAcceptOnlySelfSignedServerCertificate(true);
        }
        replicator = new Replicator(config);
        return replicator;
    }

    class MyReplicatorListener implements ReplicatorChangeListener {
        private final List<ReplicatorChange> changes = new ArrayList<>();

        public List<ReplicatorChange> getChanges() {
            return changes;
        }

        @Override
        public void changed(ReplicatorChange change) {
            changes.add(change);
        }
    }

    public URLEndpointListener serverStart(Args args) throws IOException, CouchbaseLiteException {
        int port = args.get("port");
        URLEndpointListenerConfiguration config;
        Database sourceDb = args.get("database");
        ArrayList<Collection> collectionsList = args.get("collections");
        Set<Collection> collections = new HashSet<>(collectionsList);
        if (collections.size() != 0) {
            config = new URLEndpointListenerConfiguration(collections);
        }
        else if (sourceDb != null) {
            config = new URLEndpointListenerConfiguration(sourceDb.getCollections());
        }
        else {
            throw new IllegalArgumentException("Provide collections array or database");
        }
        Boolean disableTls = args.get("tls_disable");
        Boolean tlsAuthenticator = args.get("tls_authenticator");
        String tlsAuthType = args.get("tls_auth_type");
        System.out.println(tlsAuthType);

        if (port > 0) {
            port = args.get("port");
            config.setPort(port);
        }
        config.setDisableTls(disableTls);

        if (args.get("basic_auth") != null) {
            ListenerAuthenticator listenerAuthenticator = args.get("basic_auth");
            config.setAuthenticator(listenerAuthenticator);
        }

        if (tlsAuthType.equals("self_signed_create")) {
            TLSIdentity identity = RequestHandlerDispatcher.context.getCreateIdentity();
            config.setTlsIdentity(identity);
        }

        if (tlsAuthType.equals("self_signed")) {
            TLSIdentity identity = RequestHandlerDispatcher.context.getSelfSignedIdentity();
            config.setTlsIdentity(identity);
            Log.e(TAG,"ServerSide setting the identity");
        }
        if (tlsAuthenticator) {
            List<Certificate> certsList = RequestHandlerDispatcher.context.getAuthenticatorCertsList();
            ListenerCertificateAuthenticator listenerCertificateAuthenticator = new ListenerCertificateAuthenticator(certsList);
            config.setAuthenticator(listenerCertificateAuthenticator);
        }

        URLEndpointListener p2ptcpListener = new URLEndpointListener(config);
        p2ptcpListener.start();
        System.out.println(p2ptcpListener.getPort());
        return p2ptcpListener;
    }

    public int getListenerPort(Args args) {
        URLEndpointListener p2ptcpListener = args.get("listener");
        return p2ptcpListener.getPort();
    }

    public ReplicatorTcpListener messageEndpointListenerStart(Args args) throws IOException, CouchbaseLiteException {
        Database sourceDb = args.get("database");
        int port = args.get("port");
        MessageEndpointListener messageEndpointListener =
                new MessageEndpointListener(new MessageEndpointListenerConfiguration(
                        sourceDb.getCollections(),
                        ProtocolType.BYTE_STREAM));
        ReplicatorTcpListener p2ptcpListener = new ReplicatorTcpListener(sourceDb, port);
        p2ptcpListener.start();
        return p2ptcpListener;
    }

    public void serverStop(Args args) {
        String endPointType = args.get("endPointType");
        if (endPointType.equals("MessageEndPoint")) {
            ReplicatorTcpListener p2ptcpListener = args.get("listener");
            p2ptcpListener.stop();
        } else {
            URLEndpointListener p2ptcpListener = args.get("listener");
            p2ptcpListener.stop();
        }
    }

    public MessageEndpointConnection createConnection(MessageEndpoint endpoint) {
        URI url = (URI) endpoint.getTarget();
        return new ReplicatorTcpClientConnection(url);
    }

    public MyDocumentReplicatorListener addReplicatorEventChangeListener(Args args) {
        return replicatorRequestHandlerObj.addReplicatorEventChangeListener(args);
    }

    public void removeReplicatorEventListener(Args args) {
        replicatorRequestHandlerObj.removeReplicatorEventListener(args);
    }

    public int changeListenerChangesCount(Args args) {
        return replicatorRequestHandlerObj.changeListenerChangesCount(args);
    }

    public List<String> replicatorEventGetChanges(Args args) {
        return replicatorRequestHandlerObj.replicatorEventGetChanges(args);
    }
}
