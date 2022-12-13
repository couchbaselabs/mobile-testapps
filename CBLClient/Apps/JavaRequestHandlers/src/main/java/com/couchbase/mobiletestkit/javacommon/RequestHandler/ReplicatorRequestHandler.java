package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.util.ArrayList;
import java.util.List;

import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DocumentReplication;
import com.couchbase.lite.DocumentReplicationListener;
import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.ReplicatedDocument;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.ReplicatorStatus;
import com.couchbase.mobiletestkit.javacommon.util.ConcurrentExecutor;
import com.couchbase.CouchbaseLiteServ.util.Log;


public class ReplicatorRequestHandler {
    private static final String TAG = "Replicator";
    /* -------------- */
    /* - Replicator - */
    /* -------------- */

    public Replicator create(Args args) {
        ReplicatorConfiguration config = args.get("config", ReplicatorConfiguration.class);
        return new Replicator(config);
    }

    public String status(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        return replicator.getStatus().toString();
    }

    public String getActivityLevel(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        return replicator.getStatus().getActivityLevel().toString().toLowerCase();
    }

    public ReplicatorChangeListener addChangeListener(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        MyReplicatorListener changeListener = new MyReplicatorListener();
        ListenerToken token = replicator.addChangeListener(ConcurrentExecutor.EXECUTOR, changeListener);
        changeListener.setToken(token);
        return changeListener;
    }

    public void removeChangeListener(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        MyReplicatorListener changeListener = args.get("changeListener", MyReplicatorListener.class);
        replicator.removeChangeListener(changeListener.getToken());
    }

    public MyDocumentReplicatorListener addReplicatorEventChangeListener(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        MyDocumentReplicatorListener changeListener = new MyDocumentReplicatorListener();
        ListenerToken token = replicator.addDocumentReplicationListener(ConcurrentExecutor.EXECUTOR, changeListener);
        changeListener.setToken(token);
        return changeListener;
    }

    public void removeReplicatorEventListener(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        MyDocumentReplicatorListener changeListener = args.get("changeListener", MyDocumentReplicatorListener.class);
        replicator.removeChangeListener(changeListener.getToken());
    }

    public int changeListenerChangesCount(Args args) {
        MyReplicatorListener changeListener = args.get("changeListener", MyReplicatorListener.class);
        return changeListener.getChanges().size();
    }

    public List<String> replicatorEventGetChanges(Args args) {
        MyDocumentReplicatorListener changeListener = args.get("changeListener", MyDocumentReplicatorListener.class);
        List<DocumentReplication> changes = changeListener.getChanges();
        List<String> event_list = new ArrayList<>();

        for (DocumentReplication change : changes) {
            for (ReplicatedDocument document : change.getDocuments()) {
                String doc_id = "doc_id: " + document.getID();
                String error = ", error_code: ";
                String error_domain = "0";
                int error_code = 0;

                if (document.getError() != null) {
                    error_code = document.getError().getCode();
                    error_domain = document.getError().getDomain();
                }
                error = error + error_code + ", error_domain: " + error_domain;
                String flags = ", flags: " + document.getFlags();
                String push = ", push: " + change.isPush();
                event_list.add(doc_id + error + push + flags);
            }
        }
        return event_list;
    }

    public String toString(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        return replicator.toString();
    }

    public void start(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        replicator.start();
    }

    public void stop(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        replicator.stop();
    }

    public Replicator changeGetReplicator(Args args) {
        ReplicatorChange change = args.get("change", ReplicatorChange.class);
        return change.getReplicator();
    }

    public ReplicatorStatus changeGetStatus(Args args) {
        ReplicatorChange change = args.get("change", ReplicatorChange.class);
        return change.getStatus();
    }

    public int replicatorEventChangesCount(Args args) {
        MyDocumentReplicatorListener changeListener = args.get("changeListener", MyDocumentReplicatorListener.class);
        return changeListener.getChanges().size();
    }

    public List<ReplicatorChange> changeListenerGetChanges(Args args) {
        MyReplicatorListener changeListener = args.get("changeListener", MyReplicatorListener.class);
        return changeListener.getChanges();
    }

    public CouchbaseLiteException replicatorGetError(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        Log.i(TAG, "Replication Error... ");
        Log.i(TAG, "replicator.getStatus().getError()");
        return replicator.getStatus().getError();
    }

    public ReplicatorConfiguration config(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        return replicator.getConfig();
    }

    public long getCompleted(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        return replicator.getStatus().getProgress().getCompleted();
    }

    public long getTotal(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        return replicator.getStatus().getProgress().getTotal();
    }

    public String getError(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        CouchbaseLiteException error = replicator.getStatus().getError();
        if (error != null) {
            return error.toString();
        }
        return null;
    }

    public Boolean isContinuous(Args args) {
        ReplicatorConfiguration config = args.get("config", ReplicatorConfiguration.class);
        return config.isContinuous();
    }

    public void resetCheckpoint(Args args) {
        Replicator replicator = args.get("replicator", Replicator.class);
        replicator.start(true);
    }

}

class MyReplicatorListener implements ReplicatorChangeListener {
    private final List<ReplicatorChange> changes = new ArrayList<>();
    private ListenerToken token;

    public List<ReplicatorChange> getChanges() {
        return changes;
    }

    public void setToken(ListenerToken token) {
        this.token = token;
    }

    public ListenerToken getToken() {
        return token;
    }

    @Override
    public void changed(ReplicatorChange change) {
        changes.add(change);
    }
}

class MyDocumentReplicatorListener implements DocumentReplicationListener {
    private final List<DocumentReplication> changes = new ArrayList<>();
    private ListenerToken token;

    public List<DocumentReplication> getChanges() {
        return changes;
    }

    public void setToken(ListenerToken token) {
        this.token = token;
    }

    public ListenerToken getToken() {
        return token;
    }

    @Override
    public void replication(DocumentReplication replication) {
        changes.add(replication);
    }
}


