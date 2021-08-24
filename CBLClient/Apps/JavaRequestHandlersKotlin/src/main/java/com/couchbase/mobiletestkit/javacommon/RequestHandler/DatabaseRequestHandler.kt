package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.*
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher
import com.couchbase.mobiletestkit.javacommon.util.ConcurrentExecutor
import com.couchbase.mobiletestkit.javacommon.util.Log
import com.couchbase.mobiletestkit.javacommon.util.ZipUtils
import java.io.File
import java.io.IOException
import java.util.*

class DatabaseRequestHandler {
    /* ------------ */ /* - Database - */ /* ------------ */
    @Throws(CouchbaseLiteException::class)
    fun create(args: Args): Database {
        val name = args.get<String>("name")
        Log.i(
            TAG,
            "database_create name=$name"
        )
        val config = args.get<DatabaseConfiguration>("config")
        return if (config != null) {
            val dbDir = config.directory
            /*
                     dbDir is obtained from cblite database configuration
                     1. dbDir shouldn't be null unless a bad situation happen.
                     2. while TestServer app running as a daemon service,
                     cblite core sets dbDir "/", which will cause due permission issues.
                     set dbDir to wherever the application context points to
                     */
            if (dbDir == null || dbDir == "/") {
                config.directory = RequestHandlerDispatcher.context.filesDir.absolutePath
                Log.i(TAG, "database_create directory=" + config.directory)
            }
            Database(name, config)
        } else {
            Database(name)
        }
    }

    fun getCount(args: Args): Long {
        val database = args.get<Database>("database")
        return database.count
    }

    @Throws(CouchbaseLiteException::class)
    fun close(args: Args) {
        val database = args.get<Database>("database")
        database.close()
    }

    @Throws(CouchbaseLiteException::class)
    fun compact(args: Args) {
        val database = args.get<Database>("database")
        database.performMaintenance(MaintenanceType.COMPACT)
    }

    fun getPath(args: Args): String? {
        val database = args.get<Database>("database")
        return database.path
    }

    fun getName(args: Args): String {
        val database = args.get<Database>("database")
        return database.name
    }

    fun getDocument(args: Args): Document? {
        val database = args.get<Database>("database")
        val id = args.get<String>("id")
        return database.getDocument(id)
    }

    @Throws(CouchbaseLiteException::class)
    fun getIndexes(args: Args): List<String> {
        val database = args.get<Database>("database")
        return database.indexes
    }

    fun getDocuments(args: Args): Map<String, Map<String, Any?>> {
        val database = args.get<Database>("database")
        val ids = args.get<List<String>>("ids")
        val documents: MutableMap<String, Map<String, Any?>> = HashMap()
        for (id in ids) {
            val document = database.getDocument(id)
            if (document != null) {
                val doc = document.toMap()
                // looping through the document, replace the Blob with its properties
                for ((key, value1) in doc) {
                    if (value1 != null && value1 is Map<*, *>) {
                        if (value1.size == 0) {
                            continue
                        }
                        var isBlob = false
                        val newVal: MutableMap<String, Any> = HashMap()
                        for ((key1, value2) in value1) {
                            if (value2 != null && value2 is Blob) {
                                isBlob = true
                                newVal[key1.toString()] = value2.properties
                            }
                        }
                        if (isBlob) {
                            doc[key] = newVal
                        }
                    }
                }
                documents[id] = doc
            }
        }
        return documents
    }

    @Throws(CouchbaseLiteException::class)
    fun updateDocument(args: Args) {
        val database = args.get<Database>("database")
        val id = args.get<String>("id")
        val data = args.get<Map<String, Any>>("data")
        val updateDoc = database.getDocument(id)!!.toMutable()
        updateDoc.setData(data)
        database.save(updateDoc)
    }

    @Throws(CouchbaseLiteException::class)
    fun updateDocuments(args: Args) {
        val database = args.get<Database>("database")
        val documents = args.get<Map<String, Map<String, Any>>>("documents")
        database.inBatch<RuntimeException> {
            for ((id, data) in documents) {
                val updatedDoc = database.getDocument(id)!!.toMutable()
                updatedDoc.setData(data)
                try {
                    database.save(updatedDoc)
                } catch (e: CouchbaseLiteException) {
                    Log.e(
                        TAG,
                        "DB Save failed",
                        e
                    )
                }
            }
        }
    }

    @Throws(CouchbaseLiteException::class)
    fun purge(args: Args) {
        val database = args.get<Database>("database")
        val document = args.get<MutableDocument>("document")
        database.purge(document)
    }

    @Throws(CouchbaseLiteException::class)
    fun saveDocuments(args: Args) {
        val database = args.get<Database>("database")
        val documents = args.get<Map<String, Map<String, Any>>>("documents")
        database.inBatch<RuntimeException> {
            for ((id, data) in documents) {
                val document = MutableDocument(id, data)
                try {
                    database.save(document)
                } catch (e: CouchbaseLiteException) {
                    Log.e(
                        TAG,
                        "DB Save failed",
                        e
                    )
                }
            }
        }
    }

    @Throws(CouchbaseLiteException::class)
    fun save(args: Args) {
        val database = args.get<Database>("database")
        val document = args.get<MutableDocument>("document")
        database.save(document)
    }

    @Throws(CouchbaseLiteException::class)
    fun saveWithConcurrency(args: Args) {
        val database = args.get<Database>("database")
        val document = args.get<MutableDocument>("document")
        val concurrencyControlType = args.get<String>("concurrencyControlType")
        val concurrencyType =
            if (concurrencyControlType != null && concurrencyControlType == "failOnConflict") ConcurrencyControl.FAIL_ON_CONFLICT else ConcurrencyControl.LAST_WRITE_WINS
        database.save(document, concurrencyType)
    }

    @Throws(CouchbaseLiteException::class)
    fun delete(args: Args) {
        val database = args.get<Database>("database")
        val document = args.get<Document>("document")
        database.delete(document)
    }

    @Throws(CouchbaseLiteException::class)
    fun deleteWithConcurrency(args: Args) {
        val database = args.get<Database>("database")
        val document = args.get<Document>("document")
        val concurrencyControlType = args.get<String>("concurrencyControlType")
        val concurrencyType =
            if (concurrencyControlType != null && concurrencyControlType == "failOnConflict") ConcurrencyControl.FAIL_ON_CONFLICT else ConcurrencyControl.LAST_WRITE_WINS
        database.delete(document, concurrencyType)
    }

    fun deleteDB(args: Args) {
        val database = args.get<Database>("database")
        try {
            database.delete()
            Log.i(TAG, "database deleted")
        } catch (ex: CouchbaseLiteException) {
            Log.e(TAG, "deleteDB() ERROR !!!!!!", ex)
        }
    }

    @Throws(CouchbaseLiteException::class)
    fun changeEncryptionKey(args: Args) {
        val database = args.get<Database>("database")
        val password = args.get<String>("password")
        val encryptionKey: EncryptionKey?
        encryptionKey = if (password == "nil") {
            null
        } else {
            EncryptionKey(password)
        }
        database.changeEncryptionKey(encryptionKey)
    }

    @Throws(CouchbaseLiteException::class)
    fun deleteDbByName(args: Args) {
        val dbName = args.get<String>("dbName")
        val directory = args.get<File>("directory")
        Database.delete(dbName, directory.parentFile)
    }

    fun exists(args: Args): Boolean {
        val name = args.get<String>("name")
        val directory = File(args.get<Any>("directory").toString())
        return Database.exists(name, directory)
    }

    @Throws(CouchbaseLiteException::class)
    fun deleteBulkDocs(args: Args) {
        val db = args.get<Database>("database")
        val docIds = args.get<List<String>>("doc_ids")
        db.inBatch<RuntimeException> {
            for (id in docIds) {
                val document = db.getDocument(id)
                try {
                    db.delete(document!!)
                } catch (e: CouchbaseLiteException) {
                    Log.e(
                        TAG,
                        "DB Delete failed",
                        e
                    )
                }
            }
        }
    }

    @Throws(CouchbaseLiteException::class)
    fun getDocIds(args: Args): List<String?> {
        val database = args.get<Database>("database")
        val limit = args.get<Int>("limit")
        val offset = args.get<Int>("offset")
        val query: Query = QueryBuilder
            .select(SelectResult.expression(Meta.id))
            .from(DataSource.database(database))
            .limit(Expression.intValue(limit), Expression.intValue(offset))
        val result: MutableList<String?> = ArrayList()
        val results = query.execute()
        for (row in results) {
            result.add(row.getString("id"))
        }
        return result
    }

    fun addChangeListener(args: Args): ListenerToken {
        val database = args.get<Database>("database")
        val token: ListenerToken
        token = if (args.contain("docId")) {
            val docId = args.get<String>("docId")
            val changeListener = MyDocumentChangeListener()
            database.addDocumentChangeListener(docId, ConcurrentExecutor.EXECUTOR, changeListener)
        } else {
            val changeListener = MyDatabaseChangeListener()
            database.addChangeListener(ConcurrentExecutor.EXECUTOR, changeListener)
        }
        return token
    }

    fun removeChangeListener(args: Args) {
        val database = args.get<Database>("database")
        val token = args.get<ListenerToken>("changeListenerToken")
        database.removeChangeListener(token)
    }

    fun databaseChangeListenerChangesCount(args: Args): Int {
        val changeListener: MyDatabaseChangeListener = args["changeListener"]
        return changeListener.getChanges()!!.size
    }

    fun databaseChangeListenerGetChange(args: Args): DatabaseChange {
        val changeListener: MyDatabaseChangeListener = args["changeListener"]
        val index = args.get<Int>("index")
        return changeListener.getChanges()!![index]
    }

    fun changeGetDatabase(args: Args): Database {
        val change = args.get<DatabaseChange>("change")
        return change.database
    }

    fun changeGetDocumentId(args: Args): List<String> {
        val change = args.get<DatabaseChange>("change")
        return change.documentIDs
    }

    @Throws(CouchbaseLiteException::class)
    fun copy(args: Args) {
        val dbName = args.get<String>("dbName")
        val dbPath = args.get<String>("dbPath")
        val dbConfig = args.get<DatabaseConfiguration>("dbConfig")
        val oldDbPath = File(dbPath)
        Database.copy(oldDbPath, dbName, dbConfig)
    }

    @Throws(IOException::class)
    fun getPreBuiltDb(args: Args): String {
        val dbPath = args.get<String>("dbPath")
        var dbFileName = File(dbPath).name
        dbFileName = dbFileName.substring(0, dbFileName.lastIndexOf("."))
        val context = RequestHandlerDispatcher.context
        val zipper = ZipUtils()
        zipper.unzip(context.getAsset(dbPath), context.filesDir)
        return context.filesDir.absolutePath + "/" + dbFileName
    }

    companion object {
        private const val TAG = "DATABASE"
    }
}

internal class MyDatabaseChangeListener : DatabaseChangeListener {
    private val changes: MutableList<DatabaseChange>? = null
    fun getChanges(): List<DatabaseChange>? {
        return changes
    }

    override fun changed(change: DatabaseChange) {
        changes!!.add(change)
    }
}