package com.couchbase.lite.mobiletestkit.legacy;


import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.Expression;
import com.couchbase.lite.SelectResult;


public class SelectResultRequestHandler {

    public SelectResult expressionCreate(Args args) {
        Expression expression = args.get("expression", Expression.class);
        return SelectResult.expression(expression);
    }

    public SelectResult all(Args args) {
        return SelectResult.all();
    }
}
