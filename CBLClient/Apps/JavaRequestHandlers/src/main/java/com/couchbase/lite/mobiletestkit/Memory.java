package com.couchbase.lite.mobiletestkit;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class Memory extends ObjectStore {
    private final String id;
    private final String platform;

    private final Map<String, Object> symTab;
    private final AtomicInteger nextAddress = new AtomicInteger(0);

    public static Memory create(String id) { return new Memory(id, new HashMap<>()); }


    private Memory(String id, Map<String, Object> symTab) {
        super(symTab);
        this.id = id;
        this.symTab = symTab;
        platform = TestKitApp.getApp().getPlatform();
    }

    public String add(Object value) {
        String address = "@" + nextAddress.getAndIncrement() + "_" + id + "_" + platform;
        synchronized (symTab) { symTab.put(address, value); }
        return address;
    }

    public void remove(String address) {
        synchronized (symTab) { symTab.remove(address); }
    }

    public void flush() {
        synchronized (symTab) { symTab.clear(); }
        nextAddress.set(0);
    }

    @Nullable
    @Override
    public <T> T get(String name, Class<T> expectedType) {
        synchronized (symTab) { return super.get(name, expectedType); }
    }
}