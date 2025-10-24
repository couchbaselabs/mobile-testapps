package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.couchbase.lite.MaintenanceType;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.mobiletestkit.javacommon.Context;
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher;
import com.couchbase.mobiletestkit.javacommon.util.Log;
import com.couchbase.mobiletestkit.javacommon.util.ZipUtils;
import com.couchbase.lite.Blob;
import com.couchbase.lite.ConcurrencyControl;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.EncryptionKey;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;

public class DatabaseRequestHandler {
    private static final String TAG = "DATABASE";
    /* ------------ */
    /* - Database - */
    /* ------------ */

    public Database create(Args args) throws CouchbaseLiteException {
        String name = args.get("name");
        Log.i(TAG, "database_create name=" + name);
        DatabaseConfiguration config = args.get("config");
        if (config != null) {
            String dbDir = config.getDirectory();
             /*
             * dbDir is obtained from cblite database configuration
             * 1. dbDir shouldn't be null unless a bad situation happen.
             * 2. while TestServer app running as a daemon service,
             * cblite core sets dbDir "/", which will cause due permission issues.
             * set dbDir to wherever the application context points to
             */
            if (dbDir == null || dbDir.equals("/")) {
                config.setDirectory(RequestHandlerDispatcher.context.getFilesDir().getAbsolutePath());
                Log.i(TAG, "database_create directory=" + config.getDirectory());
            }
            return new Database(name, config);
        } else {
            return new Database(name);
        }
    }


    public long getCount(Args args) {
        Database database = args.get("database");
        try {
            return  database.getDefaultCollection().getCount();
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException(e);
        }
    }

    public void close(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        database.close();
    }

    public void compact(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        database.performMaintenance(MaintenanceType.COMPACT);
    }

    public String getPath(Args args) {
        Database database = args.get("database");
        return database.getPath();
    }

    public String getName(Args args) {
        Database database = args.get("database");
        return database.getName();
    }

    public Document getDocument(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        String id = args.get("id");
        return database.getDefaultCollection().getDocument(id);
    }

    public List<String> getIndexes(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        return new ArrayList<>(database.getDefaultCollection().getIndexes());
    }

    public Map<String, Map<String, Object>> getDocuments(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        List<String> ids = args.get("ids");
        Map<String, Map<String, Object>> documents = new HashMap<>();
        for (String id : ids) {
            Document document = database.getDefaultCollection().getDocument(id);
            if (document != null) {
                Map<String, Object> doc = document.toMap();
                // looping through the document, replace the Blob with its properties
                for (Map.Entry<String, Object> entry : doc.entrySet()) {
                    if (entry.getValue() != null && entry.getValue() instanceof Map<?, ?>) {
                        if (((Map) entry.getValue()).size() == 0) {
                            continue;
                        }
                        boolean isBlob = false;
                        Map<?, ?> value = (Map<?, ?>) entry.getValue();
                        Map<String, Object> newVal = new HashMap<>();
                        for (Map.Entry<?, ?> item : value.entrySet()) {
                            if (item.getValue() != null && item.getValue() instanceof Blob) {
                                isBlob = true;
                                Blob b = (Blob) item.getValue();
                                newVal.put(item.getKey().toString(), b.getProperties());
                            }
                        }
                        if (isBlob) {
                            doc.put(entry.getKey(), newVal); 
                        }
                    }
                }
                documents.put(id, doc);
            }
        }
        return documents;
    }

    public void updateDocument(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        String id = args.get("id");
        Map<String, Object> data = args.get("data");
        MutableDocument updatedDoc = database.getDefaultCollection().getDocument(id).toMutable();
        Map<String, Object> new_data = this.setDataBlob(data);
        updatedDoc.setData(new_data);
        database.getDefaultCollection().save(updatedDoc);
    }

    public void updateDocuments(Args args) throws CouchbaseLiteException {
        final Database database = args.get("database");
        final Map<String, Map<String, Object>> documents = args.get("documents");
        database.inBatch(() -> {
            for (Map.Entry<String, Map<String, Object>> entry : documents.entrySet()) {
                String id = entry.getKey();
                Map<String, Object> data = entry.getValue();
                MutableDocument updatedDoc = database.getDefaultCollection().getDocument(id).toMutable();
                Map<String, Object> new_data = this.setDataBlob(data);
                updatedDoc.setData(new_data);
                try {
                    database.getDefaultCollection().save(updatedDoc);
                }
                catch (CouchbaseLiteException e) {
                    Log.e(TAG, "DB Save failed", e);
                }
            }
        });
    }

    public void purge(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        MutableDocument document = args.get("document");
        database.getDefaultCollection().purge(document);
    }

    public void saveDocuments(Args args) throws CouchbaseLiteException {
        final Database database = args.get("database");
        final Map<String, Map<String, Object>> documents = args.get("documents");

        database.inBatch(() -> {
            for (Map.Entry<String, Map<String, Object>> entry : documents.entrySet()) {
                String id = entry.getKey();
                Map<String, Object> data = entry.getValue();
                Map<String, Object> new_data = this.setDataBlob(data);
                MutableDocument document = new MutableDocument(id, new_data);
                try {
                    database.getDefaultCollection().save(document);
                }
                catch (CouchbaseLiteException e) {
                    Log.e(TAG, "DB Save failed", e);
                }
            }
        });
    }

    public void save(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        MutableDocument document = args.get("document");
        database.getDefaultCollection().save(document);
    }

    public void saveWithConcurrency(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        MutableDocument document = args.get("document");
        String concurrencyControlType = args.get("concurrencyControlType");
        ConcurrencyControl concurrencyType = ((concurrencyControlType != null) 
            && (concurrencyControlType.equals("failOnConflict")))
                ? ConcurrencyControl.FAIL_ON_CONFLICT
                : ConcurrencyControl.LAST_WRITE_WINS;
        database.getDefaultCollection().save(document, concurrencyType);
    }

    public void delete(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        Document document = args.get("document");
        database.getDefaultCollection().delete(document);
    }

    public void deleteWithConcurrency(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        Document document = args.get("document");
        String concurrencyControlType = args.get("concurrencyControlType");
        ConcurrencyControl concurrencyType = ((concurrencyControlType != null) 
            && (concurrencyControlType.equals("failOnConflict")))
                ? ConcurrencyControl.FAIL_ON_CONFLICT
                : ConcurrencyControl.LAST_WRITE_WINS;

        database.getDefaultCollection().delete(document, concurrencyType);
    }

    public void deleteDB(Args args) {
        Database database = args.get("database");
        try {
            database.delete();
            Log.i(TAG, "database deleted");
        }
        catch (CouchbaseLiteException ex) {
            Log.e(TAG, "deleteDB() ERROR !!!!!!", ex);
        }
    }

    public void changeEncryptionKey(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        String password = args.get("password");
        EncryptionKey encryptionKey;
        if (password.equals("nil")) { 
            encryptionKey = null; 
        }
        else { 
            encryptionKey = new EncryptionKey(password); 
        }
        database.changeEncryptionKey(encryptionKey);
    }

    public void deleteDbByName(Args args) throws CouchbaseLiteException {
        String dbName = args.get("dbName");
        File directory = args.get("directory");
        Database.delete(dbName, directory.getParentFile());
    }

    public boolean exists(Args args) {
        String name = args.get("name");
        File directory = new File(args.get("directory").toString());
        return Database.exists(name, directory);
    }

    public void deleteBulkDocs(Args args) throws CouchbaseLiteException {
        final Database db = args.get("database");
        final List<String> docIds = args.get("doc_ids");
        db.inBatch(() -> {
            for (String id : docIds) {
                Document document = db.getDefaultCollection().getDocument(id);
                try {
                    db.getDefaultCollection().delete(document);
                }
                catch (CouchbaseLiteException e) {
                    Log.e(TAG, "DB Delete failed", e);
                }
            }
        });

    }

    public List<String> getDocIds(Args args) throws CouchbaseLiteException {
        Database database = args.get("database");
        int limit = args.get("limit");
        int offset = args.get("offset");
        Query query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.collection(database.getDefaultCollection()))
            .limit(Expression.intValue(limit), Expression.intValue(offset));
        List<String> result = new ArrayList<>();
        ResultSet results = query.execute();
        for (Result row : results) {

            result.add(row.getString("id"));
        }
        return result;

    }

    public void copy(Args args) throws CouchbaseLiteException {
        String dbName = args.get("dbName");
        String dbPath = args.get("dbPath");

        DatabaseConfiguration dbConfig = args.get("dbConfig");
        File oldDbPath = new File(dbPath);
        Database.copy(oldDbPath, dbName, dbConfig);
    }

    public String getPreBuiltDb(Args args) throws IOException {
        String dbPath = args.get("dbPath");
        String dbFileName = new File(dbPath).getName();
        dbFileName = dbFileName.substring(0, dbFileName.lastIndexOf("."));
        Context context = RequestHandlerDispatcher.context;
        ZipUtils zipper = new ZipUtils();
        zipper.unzip(context.getAsset(dbPath), context.getFilesDir());
        return context.getFilesDir().getAbsolutePath() + "/" + dbFileName;
    }

    public String getVectorSearchDb(Args args) throws IOException {
        String dbPath = args.get("dbPath");
        String dbFileName = new File(dbPath).getName();
        dbFileName = dbFileName.substring(0, dbFileName.lastIndexOf("."));
        Context context = RequestHandlerDispatcher.context;
        // The zip utility erorrs when unziping the archive, as a workaround we copy the files to the location
        String filesFolder = context.getFilesDir().getAbsolutePath() + "/vsTestDatabase.cblite2";
        InputStream dbsqlite = context.getAsset("vsTestDatabase.cblite2/db.sqlite3");
        InputStream dbsqliteshm = context.getAsset("vsTestDatabase.cblite2/db.sqlite3-shm");
        InputStream dbsqlwal = context.getAsset("vsTestDatabase.cblite2/db.sqlite3-wal");
        new File(filesFolder).mkdirs();
        copyDbFile(dbsqlite, new FileOutputStream(new File(filesFolder + "/db.sqlite3")));
        copyDbFile(dbsqliteshm, new FileOutputStream(new File(filesFolder + "/db.sqlite3-shm")));
        copyDbFile(dbsqlwal, new FileOutputStream(new File(filesFolder + "/db.sqlite3-wal")));
        return filesFolder;
    }

    private void copyDbFile(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) != -1) {
            target.write(buf, 0, length);
        }
    }

    private Map<String, Object> setDataBlob(Map<String, Object> data) {
        if (!data.containsKey("_attachments")) {
            return data;
        }

        Map<String, Object> attachment_items = (Map<String, Object>) data.get("_attachments");
        Map<String, Object> existingBlobItems = new HashMap<>();

        for (Map.Entry<String, Object> attItem : attachment_items.entrySet()) {
            String attItemKey = attItem.getKey();
            HashMap<String, String> attItemValue = (HashMap<String, String>) attItem.getValue();
            if (attItemValue.get("data") != null) {
                String contentType = attItemKey.endsWith(".png") ? "image/jpeg": "text/plain";
                Blob blob = new Blob(contentType,
                        RequestHandlerDispatcher.context.decodeBase64(attItemValue.get("data")));
                data.put(attItemKey, blob);

            }
            else if (attItemValue.containsKey("digest")){
                existingBlobItems.put(attItemKey, attItemValue);
            }
        }
        data.remove("_attachments");
        // deal with partial blob situation,
        // remove all elements then add back blob type only items to _attachments key
        if (existingBlobItems.size() > 0 && existingBlobItems.size() < attachment_items.size()){
            data.remove("_attachments");
            data.put("_attachments", existingBlobItems);
        }

        return data;
    }
}

