package com.couchbase.lite.mobiletestkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import com.couchbase.lite.internal.utils.Fn;


public final class Args extends ObjectStore {
    private static final Gson GSON = new Gson();


    public static Args createLeaf(@Nullable String json) {
        return new Args(deserializeMap(json, s -> s));
    }

    public static Args createTree(@Nullable String json, @NonNull Memory mem) {
        return new Args(deserializeMap(json, s -> deserialize(s, mem)));
    }

    private static Map<String, Object> deserializeMap(@Nullable String json, Fn.Function<String, Object> deserializer) {
        final Map<String, Object> map = new HashMap<>();
        if (json == null) { return map; }

        for (Map.Entry<?, ?> entry: ((Map<?, ?>) GSON.fromJson(json, Map.class)).entrySet()) {
            Object k = entry.getKey();
            Object v = entry.getValue();
            if (!((k instanceof String) && (v instanceof String))) { continue; }
            map.put((String) k, deserializer.apply((String) v));
        }
        return map;
    }

    @NonNull
    private static List<Object> deserializeList(@Nullable String json, @NonNull Memory mem) {
        List<Object> list = new ArrayList<>();
        if (json == null) { return list; }
        for (Object item: GSON.fromJson(json, List.class)) {
            if (!(item instanceof String)) { continue; }
            list.add(deserialize((String) item, mem));
        }
        return list;
    }

    private static Object deserialize(@Nullable String value, @NonNull Memory mem) {
        if ((value == null) || (value.equals("null"))) { return null; }
        if (value.startsWith("@")) { return mem.get(value, Object.class); }
        if (value.equals("true")) { return Boolean.TRUE; }
        if (value.equals("false")) { return Boolean.FALSE; }
        if (value.startsWith("\"") && value.endsWith("\"")) { return value.substring(1, value.length() - 1); }
        switch (value.substring(0, 1)) {
            case "I":
                return Integer.valueOf(1);
            case "L":
                return Long.valueOf(1);
            case "F":
            case "D":
                return Double.valueOf(1);
            case "[":
                return deserializeList(value, mem);
            case "{":
                return deserializeMap(value, s -> deserialize(s, mem));
            default:
                throw new IllegalArgumentException("Unrecognized value in deserializer: " + value);
        }
    }

    public Args(Map<String, Object> args) { super(args); }
}