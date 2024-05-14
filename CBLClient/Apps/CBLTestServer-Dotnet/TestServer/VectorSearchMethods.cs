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
        public static MutableDictionaryObject wordMap;
        private static readonly bool UseInMemoryDb = true;
        private static readonly string InMemoryDbName = "vsTestDatabase";


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

            VectorModel vectorModel = new(key);
            Database.Prediction.RegisterModel(modelName, vectorModel);
            Console.WriteLine("Successfully registered Model");
            response.WriteBody("Successfully registered model: " + modelName);
        }

        static MutableDictionaryObject GetWordVectMap()
        {
            try
            {
                Database db = new(InMemoryDbName);
                string sql1 = string.Format("select word, vector from auxiliaryWords");
                IQuery query1 = db.CreateQuery(sql1);
                IResultSet rs1 = query1.Execute();
                string sql2 = string.Format("select word, vector from searchTerms");
                IQuery query2 = db.CreateQuery(sql2);
                IResultSet rs2 = query2.Execute();

                MutableDictionaryObject words = new();
                List<Result> rl = rs1.AllResults();
                List<Result> rl2 = rs2.AllResults();
                rl.AddRange(rl2);

                foreach (Result r in rl)
                {
                    string word = r.GetString("word");
                    ArrayObject vector = r.GetArray("vector");
                    words.SetValue(word, vector);
                }
                return words;

            }
            catch (Exception e)
            {
                Console.WriteLine(e + "retrieving vector could not be done - getWordVector query returned no results");
                return null;
            }
        }

        public static void LoadDatabase([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            if (UseInMemoryDb)
            {
                string dbID = PreparePredefinedDatabase(InMemoryDbName);
                wordMap = GetWordVectMap();
                response.WriteBody(dbID);
            }
            else
            {
                string dbID = PreparePredefinedDatabase("dummtDBIgnoreIt");
                response.WriteBody(dbID);
            }

        }

        private static string PreparePredefinedDatabase(string dbName)
        {
            string dbPath = "Databases\\vsTestDatabase.cblite2\\";

            string currDir = Directory.GetCurrentDirectory();
            string databasePath = Path.Combine(currDir, dbPath);

            DatabaseConfiguration dbConfig = new();
            Database.Copy(databasePath, dbName, dbConfig);

            var db = MemoryMap.New<Database>(dbName, dbConfig);
            Console.WriteLine("Succesfully loaded database");
            return db;
        }



        public static object GetEmbedding(Dictionary<string, object> input)
        {
            Console.WriteLine("===== START METHOD: GET EMBEDDING");
            if ((Database)input["database"] == null)
            {
                Console.WriteLine("===== DATABASE IS NULL!!!" + (Database)input["database"]);
            }
            else
            {
                Console.WriteLine("===== DATABASE IS NOT NULL!!!" + (Database)input["database"]);
            }
            VectorModel model = new("word");
            Console.WriteLine("=== instantiated vector model");
            MutableDictionaryObject testDic = new();
            testDic.SetValue("word", input["input"].ToString());
            Console.WriteLine("XXXXXXXX inputWord in GetEmbedding = " + testDic["word"].ToString() + " XXXXXXXX");
            DictionaryObject value = model.Predict(testDic);
            Console.WriteLine("=== called prediction on model");
            Console.WriteLine("=== prediction result val = " + value.GetValue("vector"));
            return value.GetValue("vector");
        }

        public static void ExtGetEmbedding([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            VectorModel model = new("word");
            MutableDictionaryObject testDic = new();
            string input = postBody["input"].ToString();
            testDic.SetValue("word", input);
            DictionaryObject value = model.Predict(testDic);
            response.WriteBody(value.GetValue("vector"));
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

        public sealed class VectorModel : IPredictiveModel
        {
            public string key;

            public VectorModel(string key)
            {
                this.key = key;
            }

            public DictionaryObject? Predict(DictionaryObject input)
            {
                string inputWord = input.GetString(key);
                object result = new();
                result = wordMap.GetValue(inputWord);
                MutableDictionaryObject output = new();
                output.SetValue("vector", result);
                return output;
            }
        }

    }
}