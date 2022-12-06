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
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

// Read only, relatively type safe object store
public class ObjectStore {
    @NonNull
    private final Map<String, Object> args;

    public ObjectStore(@NonNull Map<String, Object> args) { this.args = Collections.unmodifiableMap(args); }

    public boolean contains(String name) { return args.containsKey(name); }

    public Boolean getBoolean(String name) { return get(name, Boolean.class); }

    public Number getNumber(String name) { return get(name, Number.class); }

    public Integer getInt(String name) { return get(name, Integer.class); }

    public Long getLong(String name) { return get(name, Long.class); }

    public Float getFloat(String name) { return get(name, Float.class); }

    public Double getDouble(String name) { return get(name, Double.class); }

    public String getString(String name) { return get(name, String.class); }

    public byte[] getData(String name) { return get(name, byte[].class); }

    @SuppressWarnings("rawtypes")
    public List getList(String name) { return get(name, List.class); }

    @SuppressWarnings("rawtypes")
    public Map getMap(String name) { return get(name, Map.class); }

    @Nullable
    public <T> T get(String name, Class<T> expectedType) {
        final Object val = args.get(name);
        if (val == null) { return null; }
        final Class<?> actualType = val.getClass();
        if (expectedType.isAssignableFrom(actualType)) { return expectedType.cast(val); }
        throw new IllegalArgumentException("Cannot convert " + actualType + " to " + expectedType);
    }
}
