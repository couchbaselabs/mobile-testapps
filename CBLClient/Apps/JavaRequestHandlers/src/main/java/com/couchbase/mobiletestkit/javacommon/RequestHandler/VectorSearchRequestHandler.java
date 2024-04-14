package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.couchbase.mobiletestkit.javacommon.*;
import com.couchbase.lite.*;
import com.couchbase.lite.internal.utils.FileUtils;
import com.couchbase.mobiletestkit.javacommon.util.Log;

public class VectorSearchRequestHandler {
    private static final String TAG = "GILAD";

    /*
     * public Object handleRequest(String method, Args args) throws Exception {
     * switch (method) {
     * case "createIndex":
     * Database database = args.get("database");
     * 
     * String scopeName = args.get("scopeName") != null ? args.get("scopeName") :
     * "_default";
     * String collectionName = args.get("collectionName") != null ?
     * args.get("collectionName") : "_default";
     * 
     * Collection collection = database.getCollection(collectionName, scopeName);
     * if (collection == null) {
     * throw new Exception("Could not open specified collection");
     * }
     * 
     * String indexName = args.get("indexName");
     * 
     * String expression = args.get("expression");
     * 
     * int dimensions = args.get("dimensions");
     * int centroids = args.get("centroids");
     * 
     * VectorEncoding.ScalarQuantizerType scalarEncoding =
     * args.get("scalarEncoding");
     * 
     * Integer subquantizers = args.get("subquantizers");
     * Integer bits = args.get("bits");
     * 
     * String metric = args.get("metric");
     * 
     * Integer minTrainingSize = args.get("minTrainingSize");
     * Integer maxTrainingSize = args.get("maxTrainingSize");
     * 
     * if (scalarEncoding != null && (bits != null || subquantizers != null)) {
     * throw new Exception(
     * "Cannot have scalar quantization and arguments for product quantization at the same time"
     * );
     * }
     * 
     * if ((bits != null && subquantizers == null) || (bits == null && subquantizers
     * != null)) {
     * throw new
     * Exception("Product quantization requires both bits and subquantizers set");
     * }
     * 
     * VectorIndexConfiguration config = new VectorIndexConfiguration(expression,
     * dimensions, centroids);
     * if (scalarEncoding != null) {
     * config.setEncoding(VectorEncoding.scalarQuantizer(scalarEncoding));
     * }
     * if (bits != null) {
     * config.setEncoding(VectorEncoding.productQuantizer(subquantizers, bits));
     * }
     * if (metric != null) {
     * switch (metric) {
     * case "euclidean":
     * config.setMetric(VectorIndexConfiguration.DistanceMetric.EUCLIDIAN);
     * break;
     * case "cosine":
     * config.setMetric(VectorIndexConfiguration.DistanceMetric.COSINE);
     * break;
     * default:
     * throw new Error("Invalid distance metric");
     * }
     * }
     * 
     * if (minTrainingSize != null) {
     * config.setMinTrainingSize(minTrainingSize);
     * }
     * 
     * if (maxTrainingSize != null) {
     * config.setMaxTrainingSize(maxTrainingSize);
     * }
     * 
     * collection.createIndex(indexName, config);
     * 
     * return "???";
     * 
     * case "registerModel":
     * String key = args.get("key");
     * String name = args.get("name");
     * vectorModel model = new vectorModel(key);
     * Database.prediction.registerModel(name, model);
     * return "Registered model with name " + name;
     * 
     * case "query":
     * String term = args.get("term");
     * 
     * Args embeddingArgs = new Args();
     * embeddingArgs.put(term, "input");
     * Object embeddedTerm = this.handleRequest("vectorSearch_getEmbedding",
     * embeddingArgs);
     * 
     * String sql = args.get("sql");
     * 
     * Database db = args.get("database");
     * 
     * Parameters params = new Parameters();
     * params.setValue("vector", embeddedTerm);
     * Query query = db.createQuery(sql);
     * query.setParameters(params);
     * 
     * List<Object> resultArray = new ArrayList<>();
     * ResultSet queryResults = query.execute();
     * for (Result row : queryResults) {
     * resultArray.add(row.toMap());
     * }
     * 
     * return resultArray;
     * 
     * case "getEmbedding":
     * Database db3 = (Database) handleRequest("vectorSearch_loadDatabase", args);
     * vectorModel model1 = new vectorModel("test", db3);
     * MutableDictionary testDic = new MutableDictionary();
     * String input = args.get("input");
     * testDic.setValue(input, "test");
     * Dictionary value = model1.predict(testDic);
     * 
     * return value;
     * 
     * default:
     * throw new Exception(method);
     * }
     * }
     */
    public String createIndex(Args args) throws CouchbaseLiteException, Exception {
        Database database = args.get("database");
        Log.d(TAG, "*******CHECKING THAT RUNNING THE LATEST BUILD*********");
        String scopeName = args.get("scopeName") != null ? args.get("scopeName") : "_default";
        String collectionName = args.get("collectionName") != null ? args.get("collectionName") : "_default";

        Collection collection = database.getCollection(collectionName, scopeName);
        if (collection == null) {
            throw new Exception("Could not open specified collection");
        }

        String indexName = args.get("indexName");

        String expression = args.get("expression");

        int dimensions = args.get("dimensions");
        int centroids = args.get("centroids");

        VectorEncoding.ScalarQuantizerType scalarEncoding = args.get("scalarEncoding");

        Integer subquantizers = args.get("subquantizers");
        Integer bits = args.get("bits");

        String metric = args.get("metric");

        Integer minTrainingSize = args.get("minTrainingSize");
        Integer maxTrainingSize = args.get("maxTrainingSize");

        if (scalarEncoding != null && (bits != null || subquantizers != null)) {
            throw new Exception(
                    "Cannot have scalar quantization and arguments for product quantization at the same time");
        }

        if ((bits != null && subquantizers == null) || (bits == null && subquantizers != null)) {
            throw new Exception("Product quantization requires both bits and subquantizers set");
        }

        VectorIndexConfiguration config = new VectorIndexConfiguration(expression, dimensions, centroids);
        if (scalarEncoding != null) {
            config.setEncoding(VectorEncoding.scalarQuantizer(scalarEncoding));
        }
        if (bits != null) {
            config.setEncoding(VectorEncoding.productQuantizer(subquantizers, bits));
        }
        if (metric != null) {
            switch (metric) {
                case "euclidean":
                    config.setMetric(VectorIndexConfiguration.DistanceMetric.EUCLIDEAN);
                    break;
                case "cosine":
                    config.setMetric(VectorIndexConfiguration.DistanceMetric.COSINE);
                    break;
                default:
                    throw new Error("Invalid distance metric");
            }
        }

        if (minTrainingSize != null) {
            config.setMinTrainingSize(minTrainingSize);
        }

        if (maxTrainingSize != null) {
            config.setMaxTrainingSize(maxTrainingSize);
        }

        try {
            collection.createIndex(indexName, config);
            return String.format("Created index with name %s on collection %s", indexName, collectionName);
        } catch (Exception e) {
            return "Could not create index: " + e;
        }
    }

    public String registerModel(Args args) {
        String key = args.get("key");
        String name = args.get("name");
        vectorModel model = new vectorModel(key, "giladDB");
        Database.prediction.registerModel(name, model);
        return "Registered model with name " + name;
    }

    public List<Object> query(Args args) throws CouchbaseLiteException, IOException {
        String term = args.get("term");

        Args embeddingArgs = new Args();
        embeddingArgs.put("input", term);
        Object embeddedTerm = this.getEmbedding(embeddingArgs);

        String sql = args.get("sql");

        Database db = args.get("database");

        Parameters params = new Parameters();
        params.setValue("vector", embeddedTerm);
        Query query = db.createQuery(sql);
        query.setParameters(params);

        List<Object> resultArray = new ArrayList<>();
        try (ResultSet rs = query.execute()) {
            for (Result row : rs) {
                resultArray.add(row.toMap());
            }
        }

        return resultArray;
    }

    public Object getEmbedding(Args args) throws CouchbaseLiteException, IOException {
        vectorModel model1 = new vectorModel("word", "giladDB");
        MutableDictionary testDic = new MutableDictionary();
        String input = args.get("input");
        testDic.setValue("word", input);
        Dictionary value = model1.predict(testDic);
        return value.getValue("vector");
    }

    public Database loadDatabase(Args args) throws CouchbaseLiteException, IOException {
        // loads the given database vsTestDatabase
        DatabaseRequestHandler dbHandler = new DatabaseRequestHandler();
        Args newArgs = args;
        String dbPath = dbHandler.getPreBuiltDb(newArgs);
        newArgs.put("dbPath", "vstestDatabase.cblite2");
        newArgs.put("directory", new File(dbPath).getParent());
        Log.d(TAG, "dbPath=" + dbPath);
        Log.d(TAG, "dbPath=" + new File(dbPath).getParent());
        Database.exists("vstestDatabase.cblite2", new File(dbPath));
        DatabaseConfigurationRequestHandler configHandler = new DatabaseConfigurationRequestHandler();
        DatabaseConfiguration dbConfig = new DatabaseConfiguration();
        dbConfig = configHandler.configure(args);
        newArgs.put("dbPath", dbPath);
        newArgs.put("dbName", "giladDB");
        newArgs.put("dbConfig", dbConfig);
        dbHandler.copy(newArgs);
        Database db1 = new Database("giladDB");

        return db1;
    }

    private class vectorModel implements PredictiveModel {
        String key;
        String dbName;

        vectorModel(String key, String name) {
            try {
                this.key = key;
                this.dbName = name;
            } catch (Exception e) {
                System.err.println(e + "Error creating instance of vectorModel");
            }
        }

        Object getWordVector(String word) throws CouchbaseLiteException {
            Database db = new Database(this.dbName);
            String sql = String.format("select vector from searchTerms where word = '%s'", word);
            Query query = db.createQuery(sql);
            ResultSet rs = query.execute();
            db.close();
            try {
                List<Result> rl = rs.allResults();
                Map<String, Object> res = rl.get(0).toMap();
                Log.d("getWordVector", "vector=" + res);
                Log.d("getWordVector", "vector=" + res.get("vector"));
                return res.get("vector");
            } catch (Exception e) {
                System.err.println(e + "retrieving vector could not be done - getWordVector query returned no results");
                return null;
            }
        }

        @Override
        public Dictionary predict(Dictionary input) {
            String inputWord = input.getString(this.key);

            Object result = new ArrayList<>();

            try {
                result = getWordVector(inputWord);
            } catch (CouchbaseLiteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            MutableDictionary output = new MutableDictionary();
            output.setValue("vector", result);
            return output;
        }

    }
    // Define other methods
}

// Define other helper classes and methods
