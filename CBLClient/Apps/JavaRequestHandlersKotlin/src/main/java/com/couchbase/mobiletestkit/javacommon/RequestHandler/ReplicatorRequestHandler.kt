package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.*
import com.couchbase.mobiletestkit.javacommon.Args
import com.couchbase.mobiletestkit.javacommon.util.ConcurrentExecutor
import com.couchbase.mobiletestkit.javacommon.util.Log
import java.util.*

class ReplicatorRequestHandler {
    /* -------------- */ /* - Replicator - */ /* -------------- */
    fun create(args: Args): Replicator {
        val config = args.get<ReplicatorConfiguration>("config")
        return Replicator(config)
    }

    fun getConfig(args: Args): ReplicatorConfiguration {
        val replicator = args.get<Replicator>("replicator")
        return replicator.config
    }

    fun status(args: Args): String {
        val replicator = args.get<Replicator>("replicator")
        return replicator.status.toString()
    }

    fun getActivityLevel(args: Args): String {
        val replicator = args.get<Replicator>("replicator")
        return replicator.status.activityLevel.toString().toLowerCase()
    }

    fun addChangeListener(args: Args): ReplicatorChangeListener {
        val replicator = args.get<Replicator>("replicator")
        val changeListener = MyReplicatorListener()
        val token = replicator.addChangeListener(ConcurrentExecutor.EXECUTOR, changeListener)
        changeListener.token = token
        return changeListener
    }

    fun removeChangeListener(args: Args) {
        val replicator = args.get<Replicator>("replicator")
        val changeListener: MyReplicatorListener = args["changeListener"]
        replicator.removeChangeListener(changeListener.token!!)
    }

    fun addReplicatorEventChangeListener(args: Args): MyDocumentReplicatorListener {
        val replicator = args.get<Replicator>("replicator")
        val changeListener = MyDocumentReplicatorListener()
        val token =
            replicator.addDocumentReplicationListener(ConcurrentExecutor.EXECUTOR, changeListener)
        changeListener.token = token
        return changeListener
    }

    fun removeReplicatorEventListener(args: Args) {
        val replicator = args.get<Replicator>("replicator")
        val changeListener: MyDocumentReplicatorListener = args["changeListener"]
        replicator.removeChangeListener(changeListener.token!!)
    }

    fun changeListenerChangesCount(args: Args): Int {
        val changeListener: MyReplicatorListener = args["changeListener"]
        return changeListener.getChanges().size
    }

    fun replicatorEventGetChanges(args: Args): List<String> {
        val changeListener: MyDocumentReplicatorListener = args["changeListener"]
        val changes = changeListener.getChanges()
        val event_list: MutableList<String> = ArrayList()
        for (change in changes) {
            for (document in change.documents) {
                val event = document.toString()
                val doc_id = "doc_id: " + document.id
                var error = ", error_code: "
                var error_domain = "0"
                var error_code = 0
                if (document.error != null) {
                    error_code = document.error!!.code
                    error_domain = document.error!!.domain
                }
                error = "$error$error_code, error_domain: $error_domain"
                val flags = ", flags: " + document.getFlags()
                val push = ", push: " + change.isPush
                event_list.add(doc_id + error + push + flags)
            }
        }
        return event_list
    }

    fun toString(args: Args): String {
        val replicator = args.get<Replicator>("replicator")
        return replicator.toString()
    }

    fun start(args: Args) {
        val replicator = args.get<Replicator>("replicator")
        replicator.start()
    }

    fun stop(args: Args) {
        val replicator = args.get<Replicator>("replicator")
        replicator.stop()
    }

    fun changeGetReplicator(args: Args): Replicator {
        val change = args.get<ReplicatorChange>("change")
        return change.replicator
    }

    fun changeGetStatus(args: Args): ReplicatorStatus {
        val change = args.get<ReplicatorChange>("change")
        return change.status
    }

    fun replicatorEventChangesCount(args: Args): Int {
        val changeListener: MyDocumentReplicatorListener = args["changeListener"]
        return changeListener.getChanges().size
    }

    fun changeListenerGetChanges(args: Args): List<ReplicatorChange> {
        val changeListener: MyReplicatorListener = args["changeListener"]
        return changeListener.getChanges()
    }

    fun replicatorGetError(args: Args): CouchbaseLiteException? {
        val replicator = args.get<Replicator>("replicator")
        Log.i(TAG, "Replication Error... ")
        Log.i(TAG, "replicator.getStatus().getError()")
        return replicator.status.error
    }

    fun config(args: Args): ReplicatorConfiguration {
        val replicator = args.get<Replicator>("replicator")
        return replicator.config
    }

    fun getCompleted(args: Args): Long {
        val replicator = args.get<Replicator>("replicator")
        return replicator.status.progress.completed
    }

    fun getTotal(args: Args): Long {
        val replicator = args.get<Replicator>("replicator")
        return replicator.status.progress.total
    }

    fun getError(args: Args): String? {
        val replicator = args.get<Replicator>("replicator")
        val error = replicator.status.error
        return error?.toString()
    }

    fun isContinuous(args: Args): Boolean {
        val config = args.get<ReplicatorConfiguration>("config")
        return config.isContinuous
    }

    fun resetCheckpoint(args: Args) {
        val replicator = args.get<Replicator>("replicator")
        replicator.start(true)
    }

    companion object {
        private const val TAG = "Replicator"
    }
}

class MyReplicatorListener : ReplicatorChangeListener {
    private val changes: MutableList<ReplicatorChange> = ArrayList()
    var token: ListenerToken? = null

    fun getChanges(): List<ReplicatorChange> {
        return changes
    }

    override fun changed(change: ReplicatorChange) {
        changes.add(change)
    }
}

class MyDocumentReplicatorListener : DocumentReplicationListener {
    private val changes: MutableList<DocumentReplication> = ArrayList()
    var token: ListenerToken? = null

    fun getChanges(): List<DocumentReplication> {
        return changes
    }

    override fun replication(replication: DocumentReplication) {
        changes.add(replication)
    }
}