package com.couchbase.mobiletestkit.javacommon.RequestHandler;


import com.couchbase.lite.Collection;
import com.couchbase.mobiletestkit.javacommon.Args;
import com.couchbase.lite.DataSource;


public class DataSourceRequestHandler {

    public DataSource collection(Args args) {
        Collection collection = args.get("collection");
        return DataSource.collection(collection);
    }
}
