package com.couchbase.lite.mobiletestkit.legacy;


import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.Array;
import com.couchbase.lite.Blob;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.MutableDictionary;
import com.couchbase.lite.internal.fleece.FLEncoder;


public class DictionaryRequestHandler {
    /* -------------- */
    /* - Dictionary - */
    /* -------------- */

    public MutableDictionary create(Args args) {
        Map<String, Object> dictionary = args.getMap("content_dict");
        if (dictionary != null) {
            return new MutableDictionary(dictionary);
        }
        return new MutableDictionary();
    }

    public MutableDictionary toMutableDictionary(Args args) {
        return new MutableDictionary((Map<String, Object>) args.getMap("dictionary"));
    }

    public int count(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        return dictionary.count();
    }

    public void fleeceEncode(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        FLEncoder encoder = args.get("encoder", FLEncoder.class);
        dictionary.encodeTo(encoder);
    }

    public String getString(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getString(key);
    }

    public MutableDictionary setString(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        String value = args.getString("value");
        return dictionary.setString(key, value);
    }


    public Number getNumber(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getNumber(key);
    }

    public MutableDictionary setNumber(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Number value = args.getNumber("value");
        return dictionary.setNumber(key, value);
    }


    public Integer getInt(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getInt(key);
    }

    public MutableDictionary setInt(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Integer value = args.getInt("value");
        return dictionary.setInt(key, value);
    }


    public Long getLong(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getLong(key);
    }

    public MutableDictionary setLong(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Long value = args.getLong("value");
        return dictionary.setLong(key, value);
    }


    public Float getFloat(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getFloat(key);
    }

    public MutableDictionary setFloat(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Double valDouble = args.getDouble("value");
        float value = valDouble.floatValue();
        return dictionary.setFloat(key, value);
    }


    public Double getDouble(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getDouble(key);
    }

    public MutableDictionary setDouble(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        double value = Double.parseDouble(args.getString("value"));
        return dictionary.setDouble(key, value);
    }


    public Boolean getBoolean(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getBoolean(key);
    }

    public MutableDictionary setBoolean(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Boolean value = args.getBoolean("value");
        return dictionary.setBoolean(key, value);
    }


    public Blob getBlob(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getBlob(key);
    }

    public MutableDictionary setBlob(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Blob value = args.get("value", Blob.class);
        return dictionary.setBlob(key, value);
    }


    public Date getDate(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getDate(key);
    }

    public MutableDictionary setDate(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Date value = args.get("value", Date.class);
        return dictionary.setDate(key, value);
    }


    public Array getArray(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getArray(key);
    }

    public MutableDictionary setArray(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Array value = args.get("value", Array.class);
        return dictionary.setArray(key, value);
    }


    public MutableDictionary getDictionary(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getDictionary(key);
    }

    public MutableDictionary setDictionary(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Dictionary value = args.get("value", Dictionary.class);
        return dictionary.setDictionary(key, value);
    }

    public Object getValue(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.getValue(key);
    }

    public MutableDictionary setValue(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        Object value = args.getString("value");
        return dictionary.setValue(key, value);
    }

    public List<String> getKeys(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        return dictionary.getKeys();
    }

    public Map<String, Object> toMap(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        return dictionary.toMap();
    }

    public MutableDictionary remove(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.remove(key);
    }

    public boolean contains(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        String key = args.getString("key");
        return dictionary.contains(key);
    }

    public Iterator<String> iterator(Args args) {
        MutableDictionary dictionary = args.get("dictionary", MutableDictionary.class);
        return dictionary.iterator();
    }
}
