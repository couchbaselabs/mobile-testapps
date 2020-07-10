package com.couchbase.mobiletestkit.javacommon.RequestHandler;

/*
  Created by sridevi.saragadam on 7/9/18.
 */

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.couchbase.lite.Authenticator;
import com.couchbase.lite.ConflictResolver;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.ListenerAuthenticator;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.util.Log;
import com.couchbase.lite.Database;
import com.couchbase.lite.MessageEndpoint;
import com.couchbase.lite.MessageEndpointConnection;
import com.couchbase.lite.MessageEndpointDelegate;
import com.couchbase.lite.ProtocolType;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.MessageEndpointListener;
import com.couchbase.lite.MessageEndpointListenerConfiguration;
import com.couchbase.lite.URLEndpoint;
import com.couchbase.lite.URLEndpointListenerConfiguration;
import com.couchbase.lite.URLEndpointListener;

public class PeerToPeerRequestHandler implements MessageEndpointDelegate {
    private static final String TAG = "P2PHANDLER";

    final ReplicatorRequestHandler replicatorRequestHandlerObj = new ReplicatorRequestHandler();


    public void clientStart(Args args) {
        Replicator replicator = args.get("replicator");
        replicator.start(
                false
        );
        Log.i(TAG, "Replication started .... ");
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
//        Authenticator authenticator = args.get("basic_auth");

        ReplicatorConfiguration config;
        Replicator replicator;

        if (replicationType == null) {
            replicationType = "push_pull";
        }
        replicationType = replicationType.toLowerCase();
        ReplicatorConfiguration.ReplicatorType replType;
        if (replicationType.equals("push")) {
            replType = ReplicatorConfiguration.ReplicatorType.PUSH;
        }
        else if (replicationType.equals("pull")) {
            replType = ReplicatorConfiguration.ReplicatorType.PULL;
        }
        else {
            replType = ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL;
        }
        Log.i(TAG, "serverDBName is " + serverDBName);
        URI uri = new URI("ws://" + ipaddress + ":" + port + "/" + serverDBName);
        if (endPointType.equals("URLEndPoint")) {
            URLEndpoint urlEndPoint = new URLEndpoint(uri);
            config = new ReplicatorConfiguration(sourceDb, urlEndPoint);
        }
        else if (endPointType.equals("MessageEndPoint")) {
            System.out.println("MessageEndpoint");
            MessageEndpoint messageEndPoint = new MessageEndpoint("p2p", uri, ProtocolType.BYTE_STREAM, this);
            config = new ReplicatorConfiguration(sourceDb, messageEndPoint);
        }
        else {
            throw new IllegalArgumentException("Incorrect EndPoint type");
        }
        config.setReplicatorType(replType);
        if (continuous != null) {
            config.setContinuous(continuous);
        }
        else {
            config.setContinuous(false);
        }
        if (documentIds != null) {
            config.setDocumentIDs(documentIds);
        }
        if (push_filter) {
            switch (filter_callback_func) {
                case "boolean":
                    config.setPushFilter(new ReplicatorBooleanFilterCallback());
                    break;
                case "deleted":
                    config.setPushFilter(new ReplicatorDeletedFilterCallback());
                    break;
                case "access_revoked":
                    config.setPushFilter(new ReplicatorAccessRevokedFilterCallback());
                    break;
                default:
                    config.setPushFilter(new DefaultReplicatorFilterCallback());
                    break;
            }
        }
        if (pull_filter) {
            switch (filter_callback_func) {
                case "boolean":
                    config.setPullFilter(new ReplicatorBooleanFilterCallback());
                    break;
                case "deleted":
                    config.setPullFilter(new ReplicatorDeletedFilterCallback());
                    break;
                case "access_revoked":
                    config.setPullFilter(new ReplicatorAccessRevokedFilterCallback());
                    break;
                default:
                    config.setPullFilter(new DefaultReplicatorFilterCallback());
                    break;
            }
        }
        switch (conflict_resolver) {
            case "local_wins":
                config.setConflictResolver(new LocalWinsCustomConflictResolver());
                break;
            case "remote_wins":
                config.setConflictResolver(new RemoteWinsCustomConflictResolver());
                break;
            case "null":
                config.setConflictResolver(new NullCustomConflictResolver());
                break;
            case "merge":
                config.setConflictResolver(new MergeCustomConflictResolver());
                break;
            case "incorrect_doc_id":
                config.setConflictResolver(new IncorrectDocIdConflictResolver());
                break;
            case "delayed_local_win":
                config.setConflictResolver(new DelayedLocalWinConflictResolver());
                break;
            case "delete_not_win":
                config.setConflictResolver(new DeleteDocConflictResolver());
                break;
            case "exception_thrown":
                config.setConflictResolver(new ExceptionThrownConflictResolver());
                break;
            default:
                config.setConflictResolver(ConflictResolver.DEFAULT);
                break;
        }
//        if (authenticator != null) {
//            config.setAuthenticator(authenticator);
//        }

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
        int port;
        Database sourceDb = args.get("database");
        int securePort =  args.get("secure_port");
        ListenerAuthenticator listenerAuthenticator = args.get("basic_auth");
        URLEndpointListenerConfiguration.Builder configBuilder;
        configBuilder = new URLEndpointListenerConfiguration.Builder(sourceDb);
        if (args.get("port") != null) {
            port = args.get("port");
            configBuilder.setPort(port);
        }
        configBuilder.setTlsDisabled(true);
        System.out.println(listenerAuthenticator);
        configBuilder.setAuthenticator(listenerAuthenticator);
        URLEndpointListener p2ptcpListener = new URLEndpointListener(configBuilder.build(), false);
        p2ptcpListener.start();
        System.out.println("*******");
        System.out.println(p2ptcpListener.getPort());
        return p2ptcpListener;
    }

    public ReplicatorTcpListener messageEndpointListenerStart(Args args) throws IOException, CouchbaseLiteException {
        int port = args.get("port");
        Database sourceDb = args.get("database");
        System.out.println("*******start TCP*********");
        MessageEndpointListener messageEndpointListener = new MessageEndpointListener(new MessageEndpointListenerConfiguration(sourceDb, ProtocolType.BYTE_STREAM));
        ReplicatorTcpListener p2ptcpListener = new ReplicatorTcpListener(sourceDb, port);
        System.out.println(p2ptcpListener.getURL());
        return p2ptcpListener;
    }

    public void serverStop(Args args) {
        String endPointType = args.get("endPointType");
        if (endPointType.equals("MessageEndPoint")) {
            ReplicatorTcpListener p2ptcpListener = args.get("replicatorTcpListener");
            p2ptcpListener.stop();
        } else {
            URLEndpointListener p2ptcpListener = args.get("replicatorTcpListener");
            System.out.println("*******stop*********");
            System.out.println(p2ptcpListener);
            System.out.println(p2ptcpListener.getPort());
            p2ptcpListener.stop();
        }
    }

    public int getServerPort(Args args) {
        URLEndpointListener p2ptcpListener = args.get("replicatorTcpListener");
        System.out.println("*******getPORT*********");
        System.out.println();
        return p2ptcpListener.getPort();
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
