package com.couchbase.mobiletestkit.javacommon.RequestHandler;

/*
  Created by sridevi.saragadam on 2/26/19.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.couchbase.CouchbaseLiteServ.util.Log;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Function;
import com.couchbase.lite.PredictionFunction;
import com.couchbase.lite.PredictiveModel;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.mobiletestkit.javacommon.Args;


public class PredictiveQueriesRequestHandler {


    public EchoModel registerModel(Args args) {
        String modelName = args.getString("model_name");
        EchoModel echoModel = new EchoModel(modelName);
        Database.prediction.registerModel(modelName, echoModel);
        return echoModel;
    }

    public void unRegisterModel(Args args) {
        String modelName = args.getString("model_name");
        Database.prediction.unregisterModel(modelName);
    }

    public List<Object> getPredictionQueryResult(Args args) throws CouchbaseLiteException {
        EchoModel echoModel = args.get("model", EchoModel.class);
        Database database = args.get("database", Database.class);
        Map<String, Object> dict = args.getMap("dictionary");
        Expression input = Expression.value(dict);
        PredictionFunction prediction = Function.prediction(echoModel.getName(), input);

        Query query = QueryBuilder
            .select(SelectResult.expression(prediction))
            .from(DataSource.database(database));

        List<Object> resultArray = new ArrayList<>();
        ResultSet rows = query.execute();
        for (Result row: rows) {
            resultArray.add(row.toMap());
        }
        return resultArray;
    }

    public String nonDictionary(Args args) throws CouchbaseLiteException {
        EchoModel echoModel = args.get("model", EchoModel.class);
        Database database = args.get("database", Database.class);
        String dict = args.getString("nonDictionary");
        Expression input = Expression.value(dict);
        PredictionFunction prediction = Function.prediction(echoModel.getName(), input);

        Query query = QueryBuilder
            .select(SelectResult.expression(prediction))
            .from(DataSource.database(database));

        query.execute();

        return "success";
    }

    public List<Object> getEuclideanDistance(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);

        String key1 = args.getString("key1");
        String key2 = args.getString("key2");
        Expression distance = Function.euclideanDistance(Expression.property(key1), Expression.property(key2));

        Query query = QueryBuilder
            .select(SelectResult.expression(distance))
            .from(DataSource.database(database));

        List<Object> resultArray = new ArrayList<>();
        ResultSet rows = query.execute();
        for (Result row: rows) {
            resultArray.add(row.toMap());
        }
        return resultArray;
    }

    public int getNumberOfCalls(Args args) {
        EchoModel echoModel = args.get("model", EchoModel.class);
        return echoModel.numberOfCalls;
    }

    public List<Object> getSquaredEuclideanDistance(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);

        String key1 = args.getString("key1");
        String key2 = args.getString("key2");
        Expression distance = Function.squaredEuclideanDistance(Expression.property(key1), Expression.property(key2));

        Query query = QueryBuilder
            .select(SelectResult.expression(distance))
            .from(DataSource.database(database));

        List<Object> resultArray = new ArrayList<>();
        ResultSet rows = query.execute();
        for (Result row: rows) {
            resultArray.add(row.toMap());
        }
        return resultArray;
    }

    public List<Object> getCosineDistance(Args args) throws CouchbaseLiteException {
        Database database = args.get("database", Database.class);

        String key1 = args.getString("key1");
        String key2 = args.getString("key2");
        Expression distance = Function.cosineDistance(Expression.property(key1), Expression.property(key2));

        Query query = QueryBuilder
            .select(SelectResult.expression(distance))
            .from(DataSource.database(database));

        List<Object> resultArray = new ArrayList<>();
        ResultSet rows = query.execute();
        for (Result row: rows) {
            resultArray.add(row.toMap());
        }
        return resultArray;
    }

    private static final class EchoModel implements PredictiveModel {
        private static final String TAG = "ECHO";
        public final String name;
        private int numberOfCalls = 0;

        EchoModel(String name) {
            Log.i(TAG, "Entered into echo model");
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Dictionary predict(Dictionary input) {
            numberOfCalls++;
            return input;
        }
    }
}