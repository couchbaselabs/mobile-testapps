package com.couchbase.lite.mobiletestkit.legacy;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.Array;
import com.couchbase.lite.Blob;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.DocumentChangeListener;
import com.couchbase.lite.MutableDocument;


public class DocumentRequestHandler {
    /* ------------ */
    /* - Document - */
    /* ------------ */
    public MutableDocument create(Args args) {
        String id = args.getString("id");
        Map<String, Object> dictionary = args.getMap("dictionary");

        if (id != null) {
            if (dictionary == null) {
                return new MutableDocument(id);
            }
            else {
                return new MutableDocument(id, dictionary);
            }
        }
        else {
            if (dictionary == null) {
                return new MutableDocument();
            }
            else {
                return new MutableDocument(dictionary);
            }
        }
    }

    public int count(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        return document.count();
    }

    public String getId(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        return document.getId();
    }

    public String getString(Args args) {
        Document document = args.get("document", Document.class);
        String key = args.getString("key");
        return document.getString(key);
    }

    public MutableDocument setString(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        String value = args.getString("value");
        return document.setString(key, value);
    }

    public Number getNumber(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getNumber(key);
    }

    public MutableDocument setNumber(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Number value = args.getNumber("value");
        return document.setNumber(key, value);
    }


    public Integer getInt(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getInt(key);
    }

    public MutableDocument setInt(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Integer value = args.getInt("value");
        return document.setInt(key, value);
    }


    public Long getLong(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getLong(key);
    }

    public MutableDocument setLong(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Long value = args.getLong("value");
        return document.setLong(key, value);
    }


    public Float getFloat(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getFloat(key);
    }

    public MutableDocument setFloat(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Double valDouble = args.getDouble("value");
        float value = valDouble.floatValue();
        return document.setFloat(key, value);
    }


    public Double getDouble(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getDouble(key);
    }

    public MutableDocument setDouble(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        String value = args.getString("value");
        double set_value = Double.parseDouble(value);
        return document.setDouble(key, set_value);
    }

    public Boolean getBoolean(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getBoolean(key);
    }

    public MutableDocument setBoolean(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Boolean value = args.getBoolean("value");
        return document.setBoolean(key, value);
    }


    public Blob getBlob(Args args) {
        Document document = args.get("document", Document.class);
        String key = args.getString("key");
        return document.getBlob(key);
    }

    public MutableDocument setArray(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Array value = args.get("value", Array.class);
        return document.setArray(key, value);
    }

    public MutableDocument toMutable(Args args) {
        Document document = args.get("document", Document.class);
        return document.toMutable();
    }

    public MutableDocument setData(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        Map<String, Object> data = args.getMap("data");
        return document.setData(data);

    }

    public MutableDocument setBlob(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Blob value = args.get("value", Blob.class);
        return document.setBlob(key, value);
    }


    public Date getDate(Args args) {
        Document document = args.get("document", Document.class);
        String key = args.getString("key");
        return document.getDate(key);
    }

    public MutableDocument setDate(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Date value = args.get("value", Date.class);
        return document.setDate(key, value);
    }


    public Array getArray(Args args) {
        Document document = args.get("document", Document.class);
        String key = args.getString("key");
        return document.getArray(key);
    }

    public Dictionary getDictionary(Args args) {
        Document document = args.get("document", Document.class);
        String key = args.getString("key");
        return document.getDictionary(key);
    }

    public MutableDocument setDictionary(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Dictionary value = args.get("value", Dictionary.class);
        return document.setDictionary(key, value);
    }

    public List<String> getKeys(Args args) {
        Document document = args.get("document", Document.class);
        return document.getKeys();
    }

    public Map<String, Object> toMap(Args args) {
        Document document = args.get("document", Document.class);
        return document.toMap();
    }

    public MutableDocument removeKey(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.remove(key);
    }

    public boolean contains(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.contains(key);
    }

    public String documentChange_getDocumentId(Args args) {
        DocumentChange change = args.get("change", DocumentChange.class);
        return change.getDocumentID();
    }

    public String documentChange_toString(Args args) {
        DocumentChange change = args.get("change", DocumentChange.class);
        return change.toString();
    }

    public Object getValue(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        return document.getValue(key);
    }

    public void setValue(Args args) {
        MutableDocument document = args.get("document", MutableDocument.class);
        String key = args.getString("key");
        Object value = args.getString("value");
        document.setValue(key, value);
    }
}

class MyDocumentChangeListener implements DocumentChangeListener {
    private final List<DocumentChange> changes = new ArrayList<>();

    public List<DocumentChange> getChanges() { return changes; }

    @Override
    public void changed(DocumentChange change) { changes.add(change); }
}


