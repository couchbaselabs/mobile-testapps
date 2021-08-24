package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.*
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.RequestHandlerDispatcher
import com.couchbase.mobiletestkit.javacommon.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.*

class ReplicatorConfigurationRequestHandler {
    @Throws(URISyntaxException::class)
    fun builderCreate(args: Args): ReplicatorConfiguration {
        val sourceDb = args.get<Database>("sourceDb")
        val targetDb = args.get<Database>("targetDb")
        var targetURI: URI? = null
        if (args.get<Any?>("targetURI") != null) {
            targetURI = URI(args.get<Any>("targetURI") as String)
        }
        return if (targetDb != null) {
            val target = DatabaseEndpoint(targetDb)
            ReplicatorConfiguration(sourceDb, target)
        } else if (targetURI != null) {
            val target = URLEndpoint(targetURI)
            ReplicatorConfiguration(sourceDb, target)
        } else {
            throw IllegalArgumentException("Incorrect configuration parameter provided")
        }
    }

    @Throws(Exception::class)
    fun configure(args: Args): ReplicatorConfiguration {
        val sourceDb = args.get<Database>("source_db")
        var targetURL: URI? = null
        if (args.get<Any?>("target_url") != null) {
            targetURL = URI(args.get<Any>("target_url") as String)
        }
        val targetDb = args.get<Database>("target_db")
        var replicatorType = args.get<String>("replication_type")
        val continuous = args.get<Boolean>("continuous")
        val channels = args.get<List<String>>("channels")
        val documentIds = args.get<List<String>>("documentIDs")
        val pinnedservercert = args.get<String>("pinnedservercert")
        val authenticator = args.get<Authenticator>("authenticator")
        val push_filter = args.get<Boolean>("push_filter")
        val pull_filter = args.get<Boolean>("pull_filter")
        val filter_callback_func = args.get<String>("filter_callback_func")
        val conflict_resolver = args.get<String>("conflict_resolver")
        val headers = args.get<Map<String, String>>("headers")
        val heartbeat = args.get<String>("heartbeat")
        val maxRetries = args.get<String>("max_retries")
        val maxRetryWaitTime = args.get<String>("max_timeout")
        if (replicatorType == null) {
            replicatorType = "push_pull"
        }
        replicatorType = replicatorType.toLowerCase()
        val replType: ReplicatorType
        //val replType: AbstractReplicator.ReplicatorType
        replType = if (replicatorType == "push") {
            // ReplicatorConfiguration.ReplicatorType.PUSH
            ReplicatorType.PUSH
        } else if (replicatorType == "pull") {
            // ReplicatorConfiguration.ReplicatorType.PULL
            ReplicatorType.PULL
        } else {
            // ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL
            ReplicatorType.PUSH_AND_PULL
        }
        val config: ReplicatorConfiguration
        config = if (sourceDb != null && targetURL != null) {
            val target = URLEndpoint(targetURL)
            ReplicatorConfiguration(sourceDb, target)
        } else if (sourceDb != null && targetDb != null) {
            val target = DatabaseEndpoint(targetDb)
            ReplicatorConfiguration(sourceDb, target)
        } else {
            throw Exception("\"No source db provided or target url provided\"")
        }
        if (continuous != null) {
            config.isContinuous = continuous
        } else {
            config.isContinuous = false
        }
        if (headers != null) {
            config.headers = headers
        }
        if (authenticator != null) {
            config.setAuthenticator(authenticator)
        }
        config.setType(replType)
        /*if (conflictResolver != null) {
            config.setConflictResolver(conflictResolver);
        }*/if (channels != null) {
            config.channels = channels
        }
        if (documentIds != null) {
            config.documentIDs = documentIds
        }
        if (heartbeat != null && !heartbeat.trim { it <= ' ' }.isEmpty()) {
            config.heartbeat = heartbeat.toInt()
        }
        if (maxRetries != null && !maxRetries.trim { it <= ' ' }.isEmpty()) {
            config.maxAttempts = maxRetries.toInt()
        }
        if (maxRetryWaitTime != null && !maxRetryWaitTime.trim { it <= ' ' }.isEmpty()) {
            config.maxAttemptWaitTime = maxRetryWaitTime.toInt()
        }
        Log.d(
            TAG,
            "Args: $args"
        )
        if (pinnedservercert != null) {
            val ServerCert = pinnedCertFile
            // Set pinned certificate.
            config.pinnedServerCertificate = ServerCert
        }
        if (push_filter) {
            when (filter_callback_func) {
                "boolean" -> config.pushFilter =
                    ReplicatorBooleanFilterCallback()
                "deleted" -> config.pushFilter = ReplicatorDeletedFilterCallback()
                "access_revoked" -> config.pushFilter = ReplicatorAccessRevokedFilterCallback()
                else -> config.pushFilter = DefaultReplicatorFilterCallback()
            }
        }
        if (pull_filter) {
            when (filter_callback_func) {
                "boolean" -> config.pullFilter =
                    ReplicatorBooleanFilterCallback()
                "deleted" -> config.pullFilter = ReplicatorDeletedFilterCallback()
                "access_revoked" -> config.pullFilter = ReplicatorAccessRevokedFilterCallback()
                else -> config.pullFilter = DefaultReplicatorFilterCallback()
            }
        }
        when (conflict_resolver) {
            "local_wins" -> config.conflictResolver =
                LocalWinsCustomConflictResolver()
            "remote_wins" -> config.conflictResolver = RemoteWinsCustomConflictResolver()
            "null" -> config.conflictResolver = NullCustomConflictResolver()
            "merge" -> config.conflictResolver = MergeCustomConflictResolver()
            "incorrect_doc_id" -> config.conflictResolver = IncorrectDocIdConflictResolver()
            "delayed_local_win" -> config.conflictResolver = DelayedLocalWinConflictResolver()
            "delete_not_win" -> config.conflictResolver = DeleteDocConflictResolver()
            "exception_thrown" -> config.conflictResolver = ExceptionThrownConflictResolver()
            else -> config.conflictResolver = ConflictResolver.DEFAULT
        }
        return config
    }

    fun create(args: Args): ReplicatorConfiguration {
        return args.get("configuration")
    }

    fun getAuthenticator(args: Args): Authenticator? {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.authenticator
    }

    fun getChannels(args: Args): List<String>? {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.channels
    }

    /*public ConflictResolver getConflictResolver(Args args){
        ReplicatorConfiguration replicatorConfiguration = args.get("configuration");
        return replicatorConfiguration.getConflictResolver();
    }*/
    fun getDatabase(args: Args): Database {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.database
    }

    fun getDocumentIDs(args: Args): List<String>? {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.documentIDs
    }

    fun getPinnedServerCertificate(args: Args): ByteArray? {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.pinnedServerCertificate
    }

    fun getReplicatorType(args: Args): String {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.replicatorType.toString()
    }

    fun getTarget(args: Args): String {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.target.toString()
    }

    fun isContinuous(args: Args): Boolean {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        return replicatorConfiguration.isContinuous
    }

    fun setAuthenticator(args: Args) {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        val authenticator = args.get<Authenticator>("authenticator")
        replicatorConfiguration.setAuthenticator(authenticator)
    }

    fun setChannels(args: Args) {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        val channels = args.get<List<String>>("channels")
        replicatorConfiguration.channels = channels
    }

    /*public void setConflictResolver(Args args){
        ReplicatorConfiguration replicatorConfiguration = args.get("configuration");
        ConflictResolver conflictResolver = args.get("conflictResolver");
        replicatorConfiguration.setConflictResolver(conflictResolver);
    }*/
    fun setContinuous(args: Args) {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        val continuous = args.get<Boolean>("continuous")
        replicatorConfiguration.isContinuous = continuous
    }

    fun setDocumentIDs(args: Args) {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        val documentIds = args.get<List<String>>("documentIds")
        replicatorConfiguration.documentIDs = documentIds
    }

    fun setPinnedServerCertificate(args: Args) {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        val cert = args.get<ByteArray>("cert")
        replicatorConfiguration.pinnedServerCertificate = cert
    }

    fun setReplicatorType(args: Args) {
        val replicatorConfiguration = args.get<ReplicatorConfiguration>("configuration")
        val type = args.get<String>("replType")
        val replicatorType: ReplicatorType
        replicatorType = when (type) {
            /*
            "PUSH" -> ReplicatorConfiguration.ReplicatorType.PUSH
            "PULL" -> ReplicatorConfiguration.ReplicatorType.PULL
            else -> ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL*/
            "PUSH" -> ReplicatorType.PUSH
            "PULL" -> ReplicatorType.PULL
            else -> ReplicatorType.PUSH_AND_PULL
        }
        replicatorConfiguration.setType(replicatorType)
    }

    private val pinnedCertFile: ByteArray
        private get() {
            var `is`: InputStream? = null
            return try {
                `is` = RequestHandlerDispatcher.context.getAsset("sg_cert.cer")
                toByteArray(`is`)
            } finally {
                if (`is` != null) {
                    try {
                        `is`.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }

    companion object {
        private const val TAG = "REPLCONFIGHANDLER"
        fun toByteArray(`is`: InputStream?): ByteArray {
            val bos = ByteArrayOutputStream()
            val b = ByteArray(1024)
            try {
                var bytesRead = `is`!!.read(b)
                while (bytesRead != -1) {
                    bos.write(b, 0, bytesRead)
                    bytesRead = `is`.read(b)
                }
            } catch (io: IOException) {
                Log.w(TAG, "Got exception " + io.message + ", Ignoring...")
            }
            return bos.toByteArray()
        }
    }
}

internal class ReplicatorBooleanFilterCallback : ReplicationFilter {
    override fun filtered(document: Document, flags: EnumSet<DocumentFlag>): Boolean {
        val key = "new_field_1"
        return if (document.contains(key)) {
            document.getBoolean(key)
        } else true
    }
}

internal class DefaultReplicatorFilterCallback : ReplicationFilter {
    override fun filtered(document: Document, flags: EnumSet<DocumentFlag>): Boolean {
        return true
    }
}

internal class ReplicatorDeletedFilterCallback : ReplicationFilter {
    override fun filtered(document: Document, flags: EnumSet<DocumentFlag>): Boolean {
        return !flags.contains(DocumentFlag.DELETED)
    }
}

internal class ReplicatorAccessRevokedFilterCallback : ReplicationFilter {
    override fun filtered(document: Document, flags: EnumSet<DocumentFlag>): Boolean {
        return !flags.contains(DocumentFlag.ACCESS_REMOVED)
    }
}

internal class LocalWinsCustomConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        return localDoc
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class RemoteWinsCustomConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        return remoteDoc
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class NullCustomConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        return null
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class MergeCustomConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        /**
         * Migrate the conflicted doc.
         * Algorithm creates a new doc with copying local doc and then adding any additional key
         * from remote doc. Conflicting keys will have value from local doc.
         */
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        val newDoc = localDoc.toMutable()
        val remoteDocMap = remoteDoc.toMap()
        for ((key, value) in remoteDocMap) {
            if (!newDoc.contains(key)) {
                newDoc.setValue(key, value)
            }
        }
        return newDoc
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class IncorrectDocIdConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        val newId = "changed$docId"
        val newDoc = MutableDocument(newId, localDoc.toMap())
        newDoc.setValue("new_value", "couchbase")
        return newDoc
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class DelayedLocalWinConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        try {
            Thread.sleep((1000 * 10).toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return localDoc
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class DeleteDocConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        return if (remoteDoc == null) {
            localDoc
        } else {
            null
        }
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class ExceptionThrownConflictResolver : ConflictResolver {
    override fun resolve(conflict: Conflict): Document? {
        val localDoc = conflict.localDocument
        val remoteDoc = conflict.remoteDocument
        check(!(localDoc == null || remoteDoc == null)) { "Either local doc or remote is/are null" }
        val docId = conflict.documentId
        val util_obj = Utility()
        util_obj.checkMismatchDocId(localDoc, remoteDoc, docId)
        throw IllegalStateException("Throwing an exception")
    }

    companion object {
        private const val TAG = "CCRREPLCONFIGHANDLER"
    }
}

internal class Utility {
    fun checkMismatchDocId(localDoc: Document, remoteDoc: Document, docId: String?) {
        val remoteDocId = remoteDoc.id
        val localDocId = localDoc.id
        check(!(remoteDocId !== docId)) { "DocId mismatch" }
        check(!(docId !== localDocId)) { "DocId mismatch" }
    }
}