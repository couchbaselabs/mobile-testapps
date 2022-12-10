package com.couchbase.lite.mobiletestkit.legacy;


import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;


public class DataSourceRequestHandler {

    public DataSource database(Args args) {
        Database database = args.get("database", Database.class);
        return DataSource.database(database);
    }
}
