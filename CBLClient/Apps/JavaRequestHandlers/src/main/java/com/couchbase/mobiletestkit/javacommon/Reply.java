//
// Copyright (c) 2022 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.mobiletestkit.javacommon;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class Reply {
    public static final Reply EMPTY = create("I-1");

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public static Reply create(@NonNull String type, @NonNull byte[] data) { return new Reply(type, data); }

    public static Reply create(@NonNull String str) { return new Reply("text/plain", '"' + str + '"'); }

    public static Reply create(@NonNull Object data, @NonNull Memory mem) {
        return new Reply("text/plain", serialize(data, mem));
    }

    @NonNull
    private static String serialize(Object value, Memory memory) {
        if (value == null) { return "null"; }
        if (value instanceof Boolean) { return ((Boolean) value) ? "true" : "false"; }
        if (value instanceof Integer) { return "I" + value; }
        if (value instanceof Long) { return "L" + value; }
        if (value instanceof Float) { return "F" + value; }
        if (value instanceof Double) { return "D" + value; }
        if (value instanceof String) { return "\"" + value + "\""; }
        if (value instanceof List) {
            final List<String> list = new ArrayList<>();
            for (Object object: (List<?>) value) { list.add(serialize(object, memory)); }
            return GSON.toJson(list);
        }
        if (value instanceof Map) {
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<?, ?> entry: ((Map<?, ?>) value).entrySet()) {
                final Object key = entry.getKey();
                if (!(key instanceof String)) {
                    throw new IllegalArgumentException("Key is not a string in serialize: " + key);
                }
                map.put((String) key, serialize(entry.getValue(), memory));
            }
            return GSON.toJson(map);
        }

        return memory.add(value);
    }


    private final String contentType;
    private final byte[] data;

    private Reply(@NonNull String contentType, @NonNull String data) {
        this(contentType, data.getBytes(StandardCharsets.UTF_8));
    }

    private Reply(@NonNull String contentType, @NonNull byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }

    public String getContentType() { return contentType; }

    public byte[] getData() { return data; }
}
