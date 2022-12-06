package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import com.couchbase.lite.Collation;
import com.couchbase.mobiletestkit.javacommon.Args;


public class CollatorRequestHandler {

    public Collation.ASCII ascii(Args args) {
        Boolean ignoreCase = args.getBoolean("ignoreCase");
        return Collation.ascii().setIgnoreCase(ignoreCase);
    }

    public Collation.Unicode unicode(Args args) {
        Boolean ignoreCase = args.getBoolean("ignoreCase");
        Boolean ignoreAccents = args.getBoolean("ignoreAccents");
        return Collation.unicode().setIgnoreCase(ignoreCase).setIgnoreAccents(ignoreAccents);
    }
}
