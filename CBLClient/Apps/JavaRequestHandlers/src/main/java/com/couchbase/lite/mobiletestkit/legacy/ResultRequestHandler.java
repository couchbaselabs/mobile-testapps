package com.couchbase.lite.mobiletestkit.legacy;

import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.Result;


public class ResultRequestHandler {

    public String getString(Args args) {
        Result query_result = args.get("query_result", Result.class);
        String key = args.getString("key");
        return query_result.getString(key);
    }
}
