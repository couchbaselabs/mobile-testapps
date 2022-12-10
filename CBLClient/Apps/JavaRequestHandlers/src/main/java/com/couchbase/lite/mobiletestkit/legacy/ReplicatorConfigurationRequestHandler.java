package com.couchbase.lite.mobiletestkit.legacy;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.couchbase.lite.Authenticator;
import com.couchbase.lite.Conflict;
import com.couchbase.lite.ConflictResolver;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseEndpoint;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentFlag;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.ReplicationFilter;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.ReplicatorType;
import com.couchbase.lite.URLEndpoint;
import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.FileUtils;
import com.couchbase.lite.mobiletestkit.util.Log;

import static java.lang.Thread.sleep;


public class ReplicatorConfigurationRequestHandler {
    private static final String TAG = "REPLCONFIGHANDLER";

    public ReplicatorConfiguration builderCreate(Args args) throws URISyntaxException {
        Database sourceDb = args.get("sourceDb", Database.class);
        Database targetDb = args.get("targetDb", Database.class);
        URI targetURI = null;
        if (args.getString("targetURI") != null) {
            targetURI = new URI(args.getString("targetURI"));
        }
        if (targetDb != null) {
            DatabaseEndpoint target = new DatabaseEndpoint(targetDb);
            return new ReplicatorConfiguration(sourceDb, target);
        }
        else if (targetURI != null) {
            URLEndpoint target = new URLEndpoint(targetURI);
            return new ReplicatorConfiguration(sourceDb, target);
        }
        else {
            throw new IllegalArgumentException("Incorrect configuration parameter provided");
        }
    }

    public ReplicatorConfiguration configure(Args args) throws URISyntaxException, IOException {
        Database sourceDb = args.get("source_db", Database.class);
        String url = args.getString("target_url");
        URI targetURL = (url == null) ? null : new URI(args.getString("target_url"));
        Database targetDb = args.get("target_db", Database.class);
        String replicatorType = args.getString("replication_type");
        Boolean continuous = args.getBoolean("continuous");
        List<String> channels = args.getList("channels");
        List<String> documentIds = args.getList("documentIDs");
        String pinnedservercert = args.getString("pinnedservercert");
        Authenticator authenticator = args.get("authenticator", Authenticator.class);
        Boolean push_filter = args.getBoolean("push_filter");
        Boolean pull_filter = args.getBoolean("pull_filter");
        String filter_callback_func = args.getString("filter_callback_func");
        String conflict_resolver = args.getString("conflict_resolver");
        Map<String, String> headers = args.getMap("headers");
        String heartbeat = args.getString("heartbeat");
        String maxRetries = args.getString("max_retries");
        String maxRetryWaitTime = args.getString("max_timeout");
        String auto_purge = args.getString("auto_purge");

        if (replicatorType == null) {
            replicatorType = "push_pull";
        }
        replicatorType = replicatorType.toLowerCase();
        ReplicatorType replType;
        if (replicatorType.equals("push")) {
            replType = ReplicatorType.PUSH;
        }
        else if (replicatorType.equals("pull")) {
            replType = ReplicatorType.PULL;
        }
        else {
            replType = ReplicatorType.PUSH_AND_PULL;
        }
        ReplicatorConfiguration config;
        if (sourceDb != null && targetURL != null) {
            URLEndpoint target = new URLEndpoint(targetURL);
            config = new ReplicatorConfiguration(sourceDb, target);
        }
        else if (sourceDb != null && targetDb != null) {
            DatabaseEndpoint target = new DatabaseEndpoint(targetDb);
            config = new ReplicatorConfiguration(sourceDb, target);
        }
        else {
            throw new IllegalArgumentException("\"No source db provided or target url provided\"");
        }
        if (continuous != null) {
            config.setContinuous(continuous);
        }
        else {
            config.setContinuous(false);
        }
        if (headers != null) {
            config.setHeaders(headers);
        }
        if (authenticator != null) {
            config.setAuthenticator(authenticator);
        }
        config.setType(replType);
        /*if (conflictResolver != null) {
            config.setConflictResolver(conflictResolver);
        }*/
        if (channels != null) {
            config.setChannels(channels);
        }
        if (documentIds != null) {
            config.setDocumentIDs(documentIds);
        }
        if (heartbeat != null && !heartbeat.trim().isEmpty()) {
            config.setHeartbeat(Integer.parseInt(heartbeat));
        }
        if (maxRetries != null && !maxRetries.trim().isEmpty()) {
            config.setMaxAttempts(Integer.parseInt(maxRetries));
        }
        if (maxRetryWaitTime != null && !maxRetryWaitTime.trim().isEmpty()) {
            config.setMaxAttemptWaitTime(Integer.parseInt(maxRetryWaitTime));
        }
        if (auto_purge != null) {
            if (auto_purge.equalsIgnoreCase("enabled")) {
                Log.i(TAG, "auto purge is enabled explicitly");
                config.setAutoPurgeEnabled(true);
            }
            else if (auto_purge.equalsIgnoreCase("disabled")) {
                Log.i(TAG, "auto purge is disabled");
                config.setAutoPurgeEnabled(false);
            }
        }
        else {
            Log.i(TAG, "auto purge not specified, use the default setting.");
        }

        Log.d(TAG, "Args: " + args);
        if (pinnedservercert != null) {
            byte[] ServerCert = this.getPinnedCertFile();
            // Set pinned certificate.
            config.setPinnedServerCertificate(ServerCert);
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
        return config;
    }

    public ReplicatorConfiguration create(Args args) {
        return args.get("variable", ReplicatorConfiguration.class);
    }

    public Authenticator getAuthenticator(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getAuthenticator();
    }

    public List<String> getChannels(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getChannels();
    }

    /*public ConflictResolver getConflictResolver(Args args){
        ReplicatorConfiguration replicatorConfiguration = args.get("configuration");
        return replicatorConfiguration.getConflictResolver();
    }*/

    public Database getDatabase(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getDatabase();
    }

    public List<String> getDocumentIDs(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getDocumentIDs();
    }

    public byte[] getPinnedServerCertificate(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getPinnedServerCertificate();
    }

    public String getReplicatorType(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getType().toString();
    }

    public String getTarget(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.getTarget().toString();
    }

    public Boolean isContinuous(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        return replicatorConfiguration.isContinuous();
    }

    public void setAuthenticator(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        Authenticator authenticator = args.get("authenticator", Authenticator.class);
        replicatorConfiguration.setAuthenticator(authenticator);
    }

    public void setChannels(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        List<String> channels = args.getList("documentIds");
        replicatorConfiguration.setChannels(channels);
    }

    /*public void setConflictResolver(Args args){
        ReplicatorConfiguration replicatorConfiguration = args.get("configuration");
        ConflictResolver conflictResolver = args.get("conflictResolver");
        replicatorConfiguration.setConflictResolver(conflictResolver);
    }*/

    public void setContinuous(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        Boolean continuous = args.getBoolean("continuous");
        replicatorConfiguration.setContinuous(continuous);
    }

    public void setDocumentIDs(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        List<String> documentIds = args.getList("documentIds");
        replicatorConfiguration.setDocumentIDs(documentIds);
    }

    public void setPinnedServerCertificate(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        byte[] cert = args.getData("cert");
        replicatorConfiguration.setPinnedServerCertificate(cert);
    }

    public void setReplicatorType(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        String type = args.getString("replType");
        ReplicatorType replicatorType;
        switch (type) {
            case "PUSH":
                replicatorType = ReplicatorType.PUSH;
                break;
            case "PULL":
                replicatorType = ReplicatorType.PULL;
                break;
            default:
                replicatorType = ReplicatorType.PUSH_AND_PULL;
        }
        replicatorConfiguration.setType(replicatorType);
    }

    public void setAutoPurge(Args args) {
        ReplicatorConfiguration replicatorConfiguration = args.get("variable", ReplicatorConfiguration.class);
        Boolean auto_purge = args.getBoolean("auto_purge");
        replicatorConfiguration.setAutoPurgeEnabled(auto_purge);
    }

    private byte[] getPinnedCertFile() throws IOException {
        try (InputStream is = TestKitApp.getApp().getAsset("sg_cert.cer")) {
            return new FileUtils().readToByteArray(is);
        }
    }

    public String addCollection(Args args) {
        throw new UnsupportedOperationException("replicatorConfigurartionRequestHandler_addCollection");
    }
}

class ReplicatorBooleanFilterCallback implements ReplicationFilter {
    @Override
    public boolean filtered(Document document, EnumSet<DocumentFlag> flags) {
        String key = "new_field_1";
        if (document.contains(key)) {
            return document.getBoolean(key);
        }
        return true;
    }
}

class DefaultReplicatorFilterCallback implements ReplicationFilter {
    @Override
    public boolean filtered(Document document, EnumSet<DocumentFlag> flags) {
        return true;
    }
}

class ReplicatorDeletedFilterCallback implements ReplicationFilter {
    @Override
    public boolean filtered(Document document, EnumSet<DocumentFlag> flags) {
        return !(flags.contains(DocumentFlag.DELETED));
    }
}

class ReplicatorAccessRevokedFilterCallback implements ReplicationFilter {
    @Override
    public boolean filtered(Document document, EnumSet<DocumentFlag> flags) {
        return !(flags.contains(DocumentFlag.ACCESS_REMOVED));
    }
}

class LocalWinsCustomConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        return localDoc;
    }
}

class RemoteWinsCustomConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        return remoteDoc;
    }
}

class NullCustomConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        return null;
    }
}

/**
 * Migrate the conflicted doc.
 * Algorithm creates a new doc with copying local doc and then adding any additional key
 * from remote doc. Conflicting keys will have value from local doc.
 */
class MergeCustomConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        MutableDocument newDoc = localDoc.toMutable();
        Map<String, Object> remoteDocMap = remoteDoc.toMap();
        for (Map.Entry<String, Object> entry: remoteDocMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (!newDoc.contains(key)) {
                newDoc.setValue(key, value);
            }
        }
        return newDoc;
    }
}

class IncorrectDocIdConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        String newId = "changed" + docId;
        MutableDocument newDoc = new MutableDocument(newId, localDoc.toMap());
        newDoc.setValue("new_value", "couchbase");
        return newDoc;
    }
}

class DelayedLocalWinConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        try { sleep(1000 * 10); } // !!!! WTF???
        catch (InterruptedException ignore) { }
        return localDoc;
    }
}

class DeleteDocConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);

        return localDoc;
    }
}

class ExceptionThrownConflictResolver implements ConflictResolver {
    private static final String TAG = "CCRREPLCONFIGHANDLER";

    @Override
    public Document resolve(Conflict conflict) {
        Document localDoc = conflict.getLocalDocument();
        Document remoteDoc = conflict.getRemoteDocument();
        if (localDoc == null || remoteDoc == null) {
            throw new IllegalStateException("Either local doc or remote is/are null");
        }
        String docId = conflict.getDocumentId();
        Utility util_obj = new Utility();
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId);
        throw new IllegalStateException("Throwing an exception");
    }
}

class Utility {
    public void checkMismatchDocId(Document localDoc, Document remoteDoc, String docId) {
        String localId = localDoc.getId();
        String remoteId = remoteDoc.getId();
        if (!(localId.equals(docId) && remoteId.equals(docId))) {
            throw new IllegalStateException("Mismatch " + docId + ": local = " + localId + ", remote = " + remoteId);
        }
    }
}
