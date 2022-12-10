package com.couchbase.lite.mobiletestkit.legacy;


import java.util.List;

import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.Array;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.MutableArray;


public class ArrayRequestHandler {
    /* ------------ */
    /* -- Array --  */
    /* ------------ */

    public MutableArray create(Args args) {
        List<Object> array = args.getList("content_array");
        if (array != null) {
            return new MutableArray(array);
        }
        return new MutableArray();
    }

    public String getString(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        int index = args.getInt("key");
        return array.getString(index);
    }

    public MutableArray setString(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        int index = args.getInt("key");
        String value = args.getString("value");
        return array.setString(index, value);
    }

    public Array getArray(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        int index = args.getInt("key");
        return array.getArray(index);
    }

    public MutableArray setArray(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        int index = args.getInt("key");
        Array value = args.get("value", Array.class);
        return array.setArray(index, value);
    }

    public Dictionary getDictionary(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        int index = args.getInt("key");
        return array.getDictionary(index);
    }

    public MutableArray setDictionary(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        int index = args.getInt("key");
        Dictionary dictionary = args.get("dictionary", Dictionary.class);
        return array.setDictionary(index, dictionary);
    }

    public MutableArray addArray(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        Array value = args.get("value", Array.class);
        return array.addArray(value);
    }

    public MutableArray addString(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        String value = args.getString("value");
        return array.addString(value);
    }

    public MutableArray addDictionary(Args args) {
        MutableArray array = args.get("array", MutableArray.class);
        Dictionary value = args.get("value", Dictionary.class);
        return array.addDictionary(value);
    }
}


