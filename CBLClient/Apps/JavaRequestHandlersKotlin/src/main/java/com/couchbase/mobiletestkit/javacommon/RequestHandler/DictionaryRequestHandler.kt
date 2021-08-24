package com.couchbase.mobiletestkit.javacommon.RequestHandler

import com.couchbase.lite.Array
import com.couchbase.lite.Blob
import com.couchbase.lite.Dictionary
import com.couchbase.lite.MutableDictionary
import com.couchbase.lite.internal.fleece.FLEncoder
import com.couchbase.mobiletestkit.javacommon.Args
import java.util.*

class DictionaryRequestHandler {
    /* -------------- */ /* - Dictionary - */ /* -------------- */
    fun create(args: Args): MutableDictionary {
        val dictionary = args.get<Map<String, Any>>("content_dict")
        return dictionary?.let { MutableDictionary(it) } ?: MutableDictionary()
    }

    fun toMutableDictionary(args: Args): MutableDictionary {
        return MutableDictionary((args.get<Any>("dictionary") as Map<String?, Any?>))
    }

    fun count(args: Args): Int {
        val dictionary = args.get<MutableDictionary>("dictionary")
        return dictionary.count()
    }

    fun fleeceEncode(args: Args) {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val encoder = args.get<FLEncoder>("encoder")
        dictionary.encodeTo(encoder)
    }

    fun getString(args: Args): String {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getString(key)!!
    }

    fun setString(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<String>("value")
        return dictionary.setString(key, value)
    }

    fun getNumber(args: Args): Number {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getNumber(key)!!
    }

    fun setNumber(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Number>("value")
        return dictionary.setNumber(key, value)
    }

    fun getInt(args: Args): Int {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getInt(key)
    }

    fun setInt(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Int>("value")
        return dictionary.setInt(key, value)
    }

    fun getLong(args: Args): Long {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getLong(key)
    }

    fun setLong(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Long>("value")
        return dictionary.setLong(key, value)
    }

    fun getFloat(args: Args): Float {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getFloat(key)
    }

    fun setFloat(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val valDouble = args.get<Double>("value")
        val value = valDouble.toFloat()
        return dictionary.setFloat(key, value)
    }

    fun getDouble(args: Args): Double {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getDouble(key)
    }

    fun setDouble(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Any>("value").toString().toDouble()
        return dictionary.setDouble(key, value)
    }

    fun getBoolean(args: Args): Boolean {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getBoolean(key)
    }

    fun setBoolean(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Boolean>("value")
        return dictionary.setBoolean(key, value)
    }

    fun getBlob(args: Args): Blob {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getBlob(key)!!
    }

    fun setBlob(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Blob>("value")
        return dictionary.setBlob(key, value)
    }

    fun getDate(args: Args): Date {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getDate(key)!!
    }

    fun setDate(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Date>("value")
        return dictionary.setDate(key, value)
    }

    fun getArray(args: Args): Array? {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getArray(key)
    }

    fun setArray(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Array>("value")
        return dictionary.setArray(key, value)
    }

    fun getDictionary(args: Args): MutableDictionary? {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getDictionary(key)
    }

    fun setDictionary(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Dictionary>("value")
        return dictionary.setDictionary(key, value)
    }

    fun getValue(args: Args): Any {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.getValue(key)!!
    }

    fun setValue(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        val value = args.get<Any>("value")
        return dictionary.setValue(key, value)
    }

    fun getKeys(args: Args): List<String> {
        val dictionary = args.get<MutableDictionary>("dictionary")
        return dictionary.keys
    }

    fun toMap(args: Args): Map<String, Any> {
        val dictionary = args.get<MutableDictionary>("dictionary")
        return dictionary.toMap()
    }

    fun remove(args: Args): MutableDictionary {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.remove(key)
    }

    operator fun contains(args: Args): Boolean {
        val dictionary = args.get<MutableDictionary>("dictionary")
        val key = args.get<String>("key")
        return dictionary.contains(key)
    }

    fun iterator(args: Args): Iterator<String> {
        val dictionary = args.get<MutableDictionary>("dictionary")
        return dictionary.iterator()
    }
}