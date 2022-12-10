package com.couchbase.lite.mobiletestkit.legacy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.couchbase.lite.Blob;
import com.couchbase.lite.ConcurrencyControl;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseChange;
import com.couchbase.lite.DatabaseChangeListener;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.EncryptionKey;
import com.couchbase.lite.Expression;
import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.MaintenanceType;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.ConcurrentExecutor;
import com.couchbase.lite.mobiletestkit.util.FileUtils;
import com.couchbase.lite.mobiletestkit.util.Log;


public class DatabaseRequestHandler {
    private static final String TAG = "DATABASE";
    /* ------------ */
    /* - Database - */
    /* ------------ */

    public Database create(Args args) throws CouchbaseLiteException {
        String name = args.getString("name");
        Log.i(TAG, "database_create name=" + name);
        DatabaseConfiguration config = args.get("config", DatabaseConfiguration.class);
        if (config != null) {
            String dbDir = config.getDirectory();
             /*
                dbDir is obtained from cblite database configuration
                1. dbDir shouldn't be null unless a bad situation happen.
                2. while TestServer app running as a daemon service,
                cblite core sets dbDir "/", which will cause due permission issues.
                set dbDir to wherever the application context points to
                */
            if (dbDir == null || dbDir.equals("/")) {
                config.setDirectory(TestKitApp.getApp().getFilesDir().getAbsolutePath());
                Log.i(TAG, "database_create directory=" + config.getDirectory());
            }
            return new Database(name, config);
        }
        else {
            return new Database(name);
        }
    }


    public long getCount(Args args) {
        Database database = args.get("database", Database.class);
        return database.getCount();
    }

    public void close(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        database.close();
    }

    public void compact(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        database.performMaintenance(MaintenanceType.COMPACT);
    }

    public String getPath(Args args) {
        Database database = args.get("database", Database.class);
        return database.getPath();
    }

    public String getName(Args args) {
        Database database = args.get("database", Database.class);
        return database.getName();
    }

    public Document getDocument(Args args) {
        Database database = args.get("database", Database.class);
        String id = args.getString("id");
        return database.getDocument(id);
    }

    public List<String> getIndexes(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        return database.getIndexes();
    }

    public Map<String, Map<String, Object>> getDocuments(Args args) {
        Database database = args.get("database", Database.class);
        List<String> ids = args.getList("ids");
        Map<String, Map<String, Object>> documents = new HashMap<>();
        for (String id: ids) {
            Document document = database.getDocument(id);
            if (document != null) {
                Map<String, Object> doc = document.toMap();
                // looping through the document, replace the Blob with its properties
                for (Map.Entry<String, Object> entry: doc.entrySet()) {
                    if (entry.getValue() != null && entry.getValue() instanceof Map<?, ?>) {
                        if (((Map<?, ?>) entry.getValue()).size() == 0) { continue; }
                        boolean isBlob = false;
                        Map<?, ?> value = (Map<?, ?>) entry.getValue();
                        Map<String, Object> newVal = new HashMap<>();
                        for (Map.Entry<?, ?> item: value.entrySet()) {
                            if (item.getValue() != null && item.getValue() instanceof Blob) {
                                isBlob = true;
                                Blob b = (Blob) item.getValue();
                                newVal.put(item.getKey().toString(), b.getProperties());
                            }
                        }
                        if (isBlob) { doc.put(entry.getKey(), newVal); }
                    }
                }
                documents.put(id, doc);
            }
        }
        return documents;
    }

    public void updateDocument(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        String id = args.getString("id");
        Map<String, Object> data = args.getMap("data");
        MutableDocument updatedDoc = database.getDocument(id).toMutable();
        Map<String, Object> new_data = this.setDataBlob(data);
        updatedDoc.setData(new_data);
        database.save(updatedDoc);
    }

    public void updateDocuments(Args args) throws CouchbaseLiteException {
        final Database database = args.get("database", Database.class);
        final Map<String, Map<String, Object>> documents = args.getMap("documents");
        database.inBatch(() -> {
            for (Map.Entry<String, Map<String, Object>> entry: documents.entrySet()) {
                String id = entry.getKey();
                Map<String, Object> data = entry.getValue();
                MutableDocument updatedDoc = database.getDocument(id).toMutable();
                Map<String, Object> new_data = this.setDataBlob(data);
                updatedDoc.setData(new_data);
                database.save(updatedDoc);
            }
        });
    }

    public void purge(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        MutableDocument document = args.get("document", MutableDocument.class);
        database.purge(document);
    }

    public void saveDocuments(Args args) throws CouchbaseLiteException {
        final Database database = args.get("database", Database.class);
        final Map<String, Map<String, Object>> documents = args.getMap("documents");

        database.inBatch(() -> {
            for (Map.Entry<String, Map<String, Object>> entry: documents.entrySet()) {
                String id = entry.getKey();
                Map<String, Object> data = entry.getValue();
                Map<String, Object> new_data = this.setDataBlob(data);
                MutableDocument document = new MutableDocument(id, new_data);
                database.save(document);
            }
        });
    }

    public void save(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        MutableDocument document = args.get("document", MutableDocument.class);
        database.save(document);
    }

    public void saveWithConcurrency(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        MutableDocument document = args.get("document", MutableDocument.class);
        String concurrencyControlType = args.getString("concurrencyControlType");
        ConcurrencyControl concurrencyType
            = ((concurrencyControlType != null) && (concurrencyControlType.equals("failOnConflict")))
            ? ConcurrencyControl.FAIL_ON_CONFLICT
            : ConcurrencyControl.LAST_WRITE_WINS;
        database.save(document, concurrencyType);
    }

    public void delete(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        Document document = args.get("document", Document.class);
        database.delete(document);
    }

    public void deleteWithConcurrency(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        Document document = args.get("document", Document.class);
        String concurrencyControlType = args.getString("concurrencyControlType");
        ConcurrencyControl concurrencyType
            = ((concurrencyControlType != null) && (concurrencyControlType.equals("failOnConflict")))
            ? ConcurrencyControl.FAIL_ON_CONFLICT
            : ConcurrencyControl.LAST_WRITE_WINS;

        database.delete(document, concurrencyType);
    }

    public void deleteDB(Args args) {
        Database database = args.get("database", Database.class);
        String dbName = database.getName();
        try {
            database.delete();
            Log.i(TAG, "database deleted: " + dbName);
        }
        catch (CouchbaseLiteException ex) { Log.w(TAG, "Failed deleting db: " + dbName, ex); }
    }

    public void changeEncryptionKey(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        String password = args.getString("password");
        EncryptionKey encryptionKey;
        if (password.equals("nil")) { encryptionKey = null; }
        else { encryptionKey = new EncryptionKey(password); }
        database.changeEncryptionKey(encryptionKey);
    }

    public void deleteDbByName(Args args) throws CouchbaseLiteException {
        String dbName = args.getString("dbName");
        File directory = args.get("directory", File.class);
        Database.delete(dbName, directory.getParentFile());
    }

    public boolean exists(Args args) {
        String name = args.getString("name");
        File directory = new File(args.getString("directory"));
        return Database.exists(name, directory);
    }

    public void deleteBulkDocs(Args args) throws CouchbaseLiteException {
        final Database db = args.get("database", Database.class);
        final List<String> docIds = args.getList("doc_ids");
        db.inBatch(() -> {
            for (String id: docIds) {
                Document document = db.getDocument(id);
                try { db.delete(document); }
                catch (CouchbaseLiteException e) { Log.w(TAG, "Failed deleting document: " + id, e); }
            }
        });
    }

    public List<String> getDocIds(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);
        int limit = args.getInt("limit");
        int offset = args.getInt("offset");
        Query query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .limit(Expression.intValue(limit), Expression.intValue(offset));
        List<String> result = new ArrayList<>();
        ResultSet results = query.execute();
        for (Result row: results) {

            result.add(row.getString("id"));
        }
        return result;
    }

    public ListenerToken addChangeListener(Args args) {
        Database database = args.get("database", Database.class);
        ListenerToken token;
        if (args.contains("docId")) {
            String docId = args.getString("docId");
            MyDocumentChangeListener changeListener = new MyDocumentChangeListener();
            token = database.addDocumentChangeListener(docId, ConcurrentExecutor.EXECUTOR, changeListener);
        }
        else {
            MyDatabaseChangeListener changeListener = new MyDatabaseChangeListener();
            token = database.addChangeListener(ConcurrentExecutor.EXECUTOR, changeListener);
        }
        return token;
    }

    public void removeChangeListener(Args args) {
        Database database = args.get("database", Database.class);
        ListenerToken token = args.get("changeListenerToken", ListenerToken.class);
        database.removeChangeListener(token);
    }

    public int databaseChangeListenerChangesCount(Args args) {
        MyDatabaseChangeListener changeListener = args.get("changeListenerToken", MyDatabaseChangeListener.class);
        return changeListener.getChanges().size();
    }

    public DatabaseChange databaseChangeListenerGetChange(Args args) {
        MyDatabaseChangeListener changeListener = args.get("changeListener", MyDatabaseChangeListener.class);
        int index = args.getInt("index");
        return changeListener.getChanges().get(index);
    }

    public Database changeGetDatabase(Args args) {
        DatabaseChange change = args.get("change", DatabaseChange.class);
        return change.getDatabase();
    }

    public List<String> changeGetDocumentId(Args args) {
        DatabaseChange change = args.get("change", DatabaseChange.class);
        return change.getDocumentIDs();
    }

    public void copy(Args args) throws CouchbaseLiteException {
        String dbName = args.getString("dbName");
        String dbPath = args.getString("dbPath");

        DatabaseConfiguration dbConfig = args.get("dbConfig", DatabaseConfiguration.class);
        File oldDbPath = new File(dbPath);
        Database.copy(oldDbPath, dbName, dbConfig);
    }

    public String getPreBuiltDb(Args args) throws IOException {
        String dbPath = args.getString("dbPath");
        String dbFileName = new File(dbPath).getName();
        dbFileName = dbFileName.substring(0, dbFileName.lastIndexOf("."));

        TestKitApp app = TestKitApp.getApp();

        FileUtils zipper = new FileUtils();
        zipper.unzip(app.getAsset(dbPath), app.getFilesDir());
        return app.getFilesDir().getAbsolutePath() + "/" + dbFileName;
    }

    private Map<String, Object> setDataBlob(Map<String, Object> data) {
        if (!data.containsKey("_attachments")) {
            return data;
        }

        Map<String, Object> attachment_items = (Map<String, Object>) data.get("_attachments");
        Map<String, Object> existingBlobItems = new HashMap<>();

        for (Map.Entry<String, Object> attItem: attachment_items.entrySet()) {
            String attItemKey = attItem.getKey();
            HashMap<String, String> attItemValue = (HashMap<String, String>) attItem.getValue();
            if (attItemValue.get("data") != null) {
                String contentType = attItemKey.endsWith(".png") ? "image/jpeg" : "text/plain";
                Blob blob = new Blob(contentType, TestKitApp.getApp().decodeBase64(attItemValue.get("data")));
                data.put(attItemKey, blob);
            }
            else if (attItemValue.containsKey("digest")) {
                existingBlobItems.put(attItemKey, attItemValue);
            }
        }
        data.remove("_attachments");
        // deal with partial blob situation,
        // remove all elements then add back blob type only items to _attachments key
        if (existingBlobItems.size() > 0 && existingBlobItems.size() < attachment_items.size()) {
            data.remove("_attachments");
            data.put("_attachments", existingBlobItems);
        }

        return data;
    }
}

class MyDatabaseChangeListener implements DatabaseChangeListener {
    private final List<DatabaseChange> changes = new ArrayList<>();

    public List<DatabaseChange> getChanges() { return changes; }

    @Override
    public void changed(DatabaseChange change) { changes.add(change); }
}
