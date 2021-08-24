package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.*
import com.couchbase.lite.Array
import com.couchbase.lite.Dictionary
import com.couchbase.mobiletestkit.javacommon.Args
import java.util.*

class DocumentRequestHandler {
    /* ------------ */ /* - Document - */ /* ------------ */
    fun create(args: Args): MutableDocument {
        val id = args.get<String>("id")
        val dictionary = args.get<Map<String, Any>>("dictionary")
        return if (id != null) {
            dictionary?.let { MutableDocument(id, it) } ?: MutableDocument(id)
        } else {
            dictionary?.let { MutableDocument(it) } ?: MutableDocument()
        }
    }

    fun count(args: Args): Int {
        val document = args.get<MutableDocument>("document")
        return document.count()
    }

    fun getId(args: Args): String {
        val document = args.get<Document>("document")
        return document.id
    }

    fun getString(args: Args): String? {
        val document = args.get<Document>("document")
        val key = args.get<String>("key")
        return document.getString(key)
    }

    fun setString(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<String>("value")
        return document.setString(key, value)
    }

    fun getNumber(args: Args): Number {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getNumber(key)!!
    }

    fun setNumber(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Number>("value")
        return document.setNumber(key, value)
    }

    fun getInt(args: Args): Int {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getInt(key)
    }

    fun setInt(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Int>("value")
        return document.setInt(key, value)
    }

    fun getLong(args: Args): Long {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getLong(key)
    }

    fun setLong(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Long>("value")
        return document.setLong(key, value)
    }

    fun getFloat(args: Args): Float {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getFloat(key)
    }

    fun setFloat(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val valDouble = args.get<Double>("value")
        val value = valDouble.toFloat()
        return document.setFloat(key, value)
    }

    fun getDouble(args: Args): Double {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getDouble(key)
    }

    fun setDouble(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<String>("value")
        val set_value = value.toDouble()
        return document.setDouble(key, set_value)
    }

    fun getBoolean(args: Args): Boolean {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getBoolean(key)
    }

    fun setBoolean(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Boolean>("value")
        return document.setBoolean(key, value)
    }

    fun getBlob(args: Args): Blob? {
        val document = args.get<Document>("document")
        val key = args.get<String>("key")
        return document.getBlob(key)
    }

    fun setArray(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Array>("value")
        return document.setArray(key, value)
    }

    fun toMutable(args: Args): MutableDocument {
        val document = args.get<Document>("document")
        return document.toMutable()
    }

    fun setData(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val data = args.get<Map<String, Any>>("data")
        return document.setData(data)
    }

    fun setBlob(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Blob>("value")
        return document.setBlob(key, value)
    }

    fun getDate(args: Args): Date? {
        val document = args.get<Document>("document")
        val key = args.get<String>("key")
        return document.getDate(key)
    }

    fun setDate(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Date>("value")
        return document.setDate(key, value)
    }

    fun getArray(args: Args): Array? {
        val document = args.get<Document>("document")
        val key = args.get<String>("key")
        return document.getArray(key)
    }

    fun getDictionary(args: Args): Dictionary? {
        val document = args.get<Document>("document")
        val key = args.get<String>("key")
        return document.getDictionary(key)
    }

    fun setDictionary(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Dictionary>("value")
        return document.setDictionary(key, value)
    }

    fun getKeys(args: Args): List<String> {
        val document = args.get<Document>("document")
        return document.keys
    }

    fun toMap(args: Args): Map<String, Any> {
        val document = args.get<Document>("document")
        return document.toMap()
    }

    fun removeKey(args: Args): MutableDocument {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.remove(key)
    }

    operator fun contains(args: Args): Boolean {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.contains(key)
    }

    fun documentChange_getDocumentId(args: Args): String {
        val change = args.get<DocumentChange>("change")
        return change.documentID
    }

    fun documentChange_toString(args: Args): String {
        val change = args.get<DocumentChange>("change")
        return change.toString()
    }

    fun getValue(args: Args): Any {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        return document.getValue(key)!!
    }

    fun setValue(args: Args) {
        val document = args.get<MutableDocument>("document")
        val key = args.get<String>("key")
        val value = args.get<Any>("value")
        document.setValue(key, value)
    }
}

internal class MyDocumentChangeListener : DocumentChangeListener {
    private val changes: MutableList<DocumentChange>? = null
    fun getChanges(): List<DocumentChange>? {
        return changes
    }

    override fun changed(change: DocumentChange) {
        changes!!.add(change)
    }
}