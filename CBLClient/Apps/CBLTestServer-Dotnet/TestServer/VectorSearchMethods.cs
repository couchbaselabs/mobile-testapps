using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Net;

using JetBrains.Annotations;

using Couchbase.Lite;
using Couchbase.Lite.Enterprise.Query;
using Couchbase.Lite.Logging;
using Couchbase.Lite.Query;

using static Couchbase.Lite.MutableDictionaryObject;
using static Couchbase.Lite.DatabaseConfiguration;
using static Couchbase.Lite.Database;

using static Couchbase.Lite.Testing.DatabaseMethods;

namespace Couchbase.Lite.Testing
{
    public static class VectorSearchMethods
    {
        public static void CreateIndex([NotNull] NameValueCollection args,
                                       [NotNull] IReadOnlyDictionary<string, object> postBody,
                                       [NotNull] HttpListenerResponse response)
        {
            // using db get defualt collection and store in Collection collection
            With<Database>(postBody, "database", database =>
            {
                //Collection collection = database.GetDefaultCollection() ?? throw new InvalidOperationException("Could not open specified collection");
                Collection collection = database.GetCollection(postBody["collectionName"].ToString(), "_default"); // TO: change the hardocded scope
                // get values from postBody
                string indexName = postBody["indexName"].ToString();
                string expression = postBody["expression"].ToString();

                // null coalescing checks
                uint dimensions = Convert.ToUInt32(postBody["dimensions"]);

                uint centroids = Convert.ToUInt32(postBody["centroids"]);

                uint minTrainingSize = Convert.ToUInt32(postBody["minTrainingSize"]);

                uint maxTrainingSize = Convert.ToUInt32(postBody["maxTrainingSize"]);

                uint? bits = 0;
                uint? subquantizers = 0;
                ScalarQuantizerType? scalarEncoding = new();

                try
                {
                    bits = Convert.ToUInt32(postBody["bits"]);
                    subquantizers = Convert.ToUInt32(postBody["subquantizers"]);
                }
                catch (Exception e)
                {
                    bits = null;
                    subquantizers = null;
                }

                try
                {
                    scalarEncoding = (ScalarQuantizerType)postBody["scalarEncoding"];
                }
                catch (Exception e)
                {
                    scalarEncoding = null;
                }




                string metric = postBody["metric"].ToString();

                // correctness checks
                if (scalarEncoding != null && (bits != null || subquantizers != null))
                {
                    throw new InvalidOperationException("Cannot have scalar quantization and arguments for product quantization at the same time");
                }

                if ((bits != null && subquantizers == null) || (bits == null && subquantizers != null))
                {
                    throw new InvalidOperationException("Product quantization requires both bits and subquantizers set");
                }


                // setting values based on config from input
                VectorEncoding encoding = VectorEncoding.None();
                if (scalarEncoding != null)
                {
                    encoding = VectorEncoding.ScalarQuantizer((ScalarQuantizerType)scalarEncoding);
                }
                if (bits != null)
                {
                    encoding = VectorEncoding.ProductQuantizer((uint)subquantizers, (uint)bits);
                }
                DistanceMetric dMetric = new();
                if (metric != null)
                {
                    dMetric = metric switch
                    {
                        "euclidean" => DistanceMetric.Euclidean,
                        "cosine" => DistanceMetric.Cosine,
                        _ => throw new Exception("Invalid distance metric"),
                    };
                }

                VectorIndexConfiguration config = new(expression, dimensions, centroids) // unure on types here again, undocumented specifics
                {
                    Encoding = encoding,
                    DistanceMetric = dMetric,
                    MinTrainingSize = minTrainingSize,
                    MaxTrainingSize = maxTrainingSize
                };

                // const uint xDIMENSIONS = 300;
                // const uint xCENTROIDS = 20;
                // const uint xMIN_TRAINING_SIZE = 100;
                // const uint xMAX_TRAINING_SIZE = 200;
                // const string xEXPRESSION = "vector";
                // const DistanceMetric xMETRIC = DistanceMetric.Cosine;

                // var config = new VectorIndexConfiguration(xEXPRESSION, xDIMENSIONS, xCENTROIDS)
                // {
                //     Encoding = VectorEncoding.None(),
                //     DistanceMetric = xMETRIC,
                //     MinTrainingSize = xMIN_TRAINING_SIZE,
                //     MaxTrainingSize = xMAX_TRAINING_SIZE
                // };

                try
                {
                    collection.CreateIndex(indexName, config);
                    Console.WriteLine("Successfully created index");
                    response.WriteBody("Created index with name " + indexName);
                }
                catch (Exception e)
                {
                    Console.WriteLine("Failed to create index");
                    response.WriteBody("Could not create index: " + e);
                }

            });

        }

        public static void RegisterModel([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            string modelName = postBody["name"].ToString();
            string key = postBody["key"].ToString();

            VectorModel vectorModel = new(key, modelName);
            Database.Prediction.RegisterModel(modelName, vectorModel);
            Console.WriteLine("Successfully registered Model");
            response.WriteBody("Successfully registered model: " + modelName);
        }

        public static void LoadDatabase([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            string dbPath = "Databases\\vsTestDatabase.cblite2\\";

            string dbName = "vsTestDatabase";

            string currDir = Directory.GetCurrentDirectory();
            string databasePath = Path.Combine(currDir, dbPath);

            DatabaseConfiguration dbConfig = new();
            Database.Copy(databasePath, dbName, dbConfig);

            var databaseId = MemoryMap.New<Database>(dbName, dbConfig);
            Console.WriteLine("Succesfully loaded database");
            response.WriteBody(databaseId);

        }

        public static object GetEmbedding(Dictionary<string, object> input)
        {
            Console.WriteLine("===== START METHOD: GET EMBEDDING");
            VectorModel model = new("word", "vsTestDatabase", (Database)input["database"]);
            Console.WriteLine("=== instantiated vector model");
            MutableDictionaryObject testDic = new();
            testDic.SetValue("word", input["input"].ToString());
            Console.WriteLine("XXXXXXXX inputWord in GetEmbedding = " + testDic["word"].ToString() + " XXXXXXXX");
            DictionaryObject value = model.Predict(testDic);
            Console.WriteLine("=== called prediction on model");
            Console.WriteLine("=== prediction result val = " + value.GetValue("vector"));
            return value.GetValue("vector");
        }

        public static void Query([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            Console.WriteLine("===== START METHOD: QUERY");
            object term = postBody["term"];
            //string db = postBody["database"].ToString();

            With<Database>(postBody, "database", db =>
            {
                Dictionary<string, object> embeddingArgs = new()
                {
                    { "input", term },
                    { "database", db }
                };

                Console.WriteLine("=== Call Get Embedding with embeddingArgs");
                Console.WriteLine("XXXXXXXX inputWord in query = " + embeddingArgs["input"].ToString() + " XXXXXXXX");
                object embeddedTerm = GetEmbedding(embeddingArgs);
                Console.WriteLine("=== value of embeddedTerm from Get Embedding method = " + embeddedTerm);

                string sql = postBody["sql"].ToString();

                Console.WriteLine("=== create IQuery and set params");
                IQuery query = db.CreateQuery(sql);
                query.Parameters.SetValue("vector", embeddedTerm);

                Console.WriteLine("=== call query.execute on each query");
                List<object> resultArray = new();
                int c = 0;
                foreach (Result row in query.Execute())
                {
                    resultArray.Add(row.ToDictionary());
                    Console.WriteLine("=== added result of query to result array");
                    Console.WriteLine("=== query result = " + resultArray[c].ToString());
                    c++;
                }

                Console.WriteLine("=== completed query executions, writing result array to response");
                response.WriteBody(resultArray);
            });
        }

    }



    public sealed class VectorModel : IPredictiveModel
    {
        public string name;
        public string key;

        public Database database;

        public VectorModel(string key, string name)
        {
            this.name = name;
            this.key = key;
        }
        public VectorModel(string key, string name, Database database)
        {
            this.name = name;
            this.key = key;
            this.database = database;
        }

        private List<object?>? GetWordVector(string word, string collection)
        {
            Console.WriteLine("===== START METHOD: GetWordVector");
            using var query = database.CreateQuery($"SELECT vector FROM {collection} WHERE word = '{word}'");
            using var rs = query.Execute();
            Console.WriteLine("=== executed word vector query");

            // Important to call ToList here, otherwise disposing the above result set invalidates the data
            var val = rs.FirstOrDefault()?.GetArray(0)?.ToList();
            Console.WriteLine("=== return val of get word vector = " + val);
            return val;
        }

        public DictionaryObject? Predict(DictionaryObject input)
        {
            Console.WriteLine("===== START METHOD: PREDICT");
            var inputWord = input.GetString(key);
            Console.WriteLine("XXXXXXXX inputWord in Predict = " + inputWord + " XXXXXXXX");
            if (inputWord == null)
            {
                Console.WriteLine("ERROR: no inputWord!");
                return null;
            }

            var result = GetWordVector(inputWord, "searchTerms") ?? GetWordVector(inputWord, "docBodyVectors") ?? GetWordVector(inputWord, "indexVectors") ?? GetWordVector(inputWord, "auxiliaryWords");
            Console.WriteLine("=== returned a val from call of get word vector");
            if (result == null)
            {
                return null;
            }

            MutableDictionaryObject retVal = new();
            retVal.SetValue("vector", result);
            Console.WriteLine("=== return val of predict = " + retVal);
            return retVal;
        }
    }
}