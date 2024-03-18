package com.couchbase.mobiletestkit.javacommon.RequestHandler;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.couchbase.mobiletestkit.javacommon.*;
import com.couchbase.lite.*;

public class VectorSearchRequestHandler {
    public Object handleRequest(String method, Args args) throws Exception {
        switch (method) {

            case "vectorSearch_createIndex":
                Database database = args.get("database");

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
                            config.setMetric(VectorIndexConfiguration.DistanceMetric.EUCLIDIAN);
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

                collection.createIndex(indexName, config);

                return "???";

            case "vectorSearch_registerModel":
                String key = args.get("key");
                String name = args.get("name");
                vectorModel model = new vectorModel(key);
                Database.prediction.registerModel(name, model);
                return "Registered model with name " + name;

            case "vectorSearch_query":
                String term = args.get("term");

                Args embeddingArgs = new Args();
                embeddingArgs.put(term, "input");
                Object embeddedTerm = this.handleRequest("vectorSearch_getEmbedding", embeddingArgs);

                String sql = args.get("sql");

                Database db = args.get("database");

                Parameters params = new Parameters();
                params.setValue("vector", embeddedTerm);
                Query query = db.createQuery(sql);
                query.setParameters(params);

                List<Object> resultArray = new ArrayList<>();
                ResultSet queryResults = query.execute();
                for (Result row : queryResults) {
                    resultArray.add(row.toMap());
                }

                return resultArray;

            case "vectorSearch_loadDatabase":
                // loads the given database vsTestDatabase
                DatabaseRequestHandler dbHandler = new DatabaseRequestHandler();
                Args newArgs = args;
                newArgs.put("dbPath", "Databases/vsTestDatabase.cblite2");
                try {
                    String dbPath = dbHandler.getPreBuiltDb(newArgs);
                    newArgs.put("dbPath", dbPath);
                    newArgs.put("dbName", "vsTestDatabase");
                    dbHandler.copy(newArgs);
                } catch (Exception e) {
                    // TODO: handle exception
                }
                Database db1 = new Database("vsTestDatabase");
                return db1;

            case "vectorSearch_getEmbedding":
                Database db3 = (Database) handleRequest("vectorSearch_loadDatabase", args);
                vectorModel model1 = new vectorModel("test", db3);
                MutableDictionary testDic = new MutableDictionary();
                String input = args.get("input");
                testDic.setValue(input, "test");
                Dictionary value = model1.predict(testDic);

                return value;

            default:
                throw new Exception(method);
        }
    }

    private class vectorModel implements PredictiveModel {
        String key;
        Database db;

        vectorModel(String key, Database db) {
            this.key = key;
            this.db = db;
        }

        vectorModel(String key) {
            this.key = key;
        }

        List<Object> getWordVector(String word, String collection) throws CouchbaseLiteException {
            String sql = String.format("select vector from %s where word = '%s'", collection, word);
            Query query = this.db.createQuery(sql);
            ResultSet rs = query.execute();
            List<Object> resultArray = new ArrayList<>();
            for (Object row : rs) {
                resultArray.add(((ResultSet) row));
            }
            return resultArray;
        }

        @Override
        public Dictionary predict(Dictionary input) {
            String inputWord = input.getString("word");

            List<Object> result = getWordVector(inputWord, "words");

            MutableDictionary output = new MutableDictionary();
            output.setValue("vector", result);
            return output;
        }

    }
    // Define other methods
}

// Define other helper classes and methods
