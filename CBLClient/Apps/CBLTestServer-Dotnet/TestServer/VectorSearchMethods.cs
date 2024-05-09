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
                Collection collection = database.GetDefaultCollection() ?? throw new InvalidOperationException("Could not open specified collection");
                Console.WriteLine("=========================================AT START");
                // get values from postBody
                string indexName = postBody["indexName"].ToString();
                Console.WriteLine("=========================================After indexName");
                string expression = postBody["expression"].ToString();
                Console.WriteLine("=========================================After expression");

                // null coalescing checks
                uint dimensions = (uint)postBody["dimensions"];
                Console.WriteLine("=========================================After dimensions");

                uint centroids = (uint)postBody["centroids"];
                Console.WriteLine("=========================================After centroids");

                uint minTrainingSize = (uint)postBody["minTrainingSize"];
                Console.WriteLine("=========================================After minTrainingSize");

                uint maxTrainingSize = (uint)postBody["maxTrainingSize"];
                Console.WriteLine("=========================================After maxTrainingSize");

                uint? bits = 0;
                uint? subquantizers = 0;
                ScalarQuantizerType? scalarEncoding = new();

                try
                {
                    bits = (uint)postBody["bits"];
                    Console.WriteLine("=========================================After bits");
                    subquantizers = (uint)postBody["subquantizers"];
                    Console.WriteLine("=========================================After subquantizers");
                }
                catch (Exception e)
                {
                    bits = null;
                    subquantizers = null;
                }

                try
                {
                    scalarEncoding = (ScalarQuantizerType)postBody["scalarEncoding"];
                    Console.WriteLine("=========================================After scalarEncoding");
                }
                catch (Exception e)
                {
                    scalarEncoding = null;
                    Console.WriteLine("=========================================At scalarEncoding=NULL");
                }




                string metric = postBody["metric"].ToString();
                Console.WriteLine("=========================================After metric");

                // correctness checks
                if (scalarEncoding != null && (bits != null || subquantizers != null))
                {
                    throw new InvalidOperationException("Cannot have scalar quantization and arguments for product quantization at the same time");
                }

                if ((bits != null && subquantizers == null) || (bits == null && subquantizers != null))
                {
                    throw new InvalidOperationException("Product quantization requires both bits and subquantizers set");
                }
                if (bits == null && subquantizers == null)
                {
                    Console.WriteLine("======== broken because bits and subquantizers are null");
                    throw new InvalidOperationException("broken");
                }

                // setting values based on config from input
                Console.WriteLine("=========================================Before VectorEncoding encoding");
                VectorEncoding encoding = VectorEncoding.None();
                if (scalarEncoding != null)
                {
                    encoding = VectorEncoding.ScalarQuantizer((ScalarQuantizerType)scalarEncoding);
                }
                if (bits != null)
                {
                    encoding = VectorEncoding.ProductQuantizer((uint)subquantizers, (uint)bits);
                }
                Console.WriteLine("=========================================Before  DistanceMetric dMetric ");
                DistanceMetric dMetric = new();
                if (metric != null)
                {
                    switch (metric)
                    {
                        case "euclidean":
                            dMetric = DistanceMetric.Euclidean;
                            break;
                        case "cosine":
                            dMetric = DistanceMetric.Cosine;
                            break;
                        default:
                            throw new Exception("Invalid distance metric");
                    }
                }

                Console.WriteLine("========== DEBUG PRINTING VALUES ===============");
                Console.WriteLine("expression == " + expression + ", dimensions == " + dimensions + ", centroids == " + centroids + ", encoding == " + encoding + ", distance metric == " + dMetric + ", minSize == " + minTrainingSize + ", maxSize == " + maxTrainingSize);

                Console.WriteLine("=========================================Before  DVectorIndexConfiguration config");
                VectorIndexConfiguration config = new(expression, dimensions, centroids) // unure on types here again, undocumented specifics
                {
                    Encoding = encoding,
                    DistanceMetric = dMetric,
                    MinTrainingSize = minTrainingSize,
                    MaxTrainingSize = maxTrainingSize
                };

                Console.WriteLine("=========================================Before  creating final index");
                try
                {
                    collection.CreateIndex(indexName, config);
                    response.WriteBody("Created index with name " + indexName);
                }
                catch (Exception e)
                {
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
            response.WriteBody(MemoryMap.Store(vectorModel));
        }

        public static void LoadDatabase([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            Console.WriteLine("I AM HERE!!!!!!!!!!!!!!!!!!!!!GILAD");
            string dbPath = "Databases\\vsTestDatabase.cblite2\\";

            string dbName = "vsTestDatabase";

            string currDir = Directory.GetCurrentDirectory();
            string databasePath = Path.Combine(currDir, dbPath);

            DatabaseConfiguration dbConfig = new();
            Database.Copy(databasePath, dbName, dbConfig);

            var databaseId = MemoryMap.New<Database>(dbName, dbConfig);
            response.WriteBody(databaseId);

        }

        public static object GetEmbedding(NameValueCollection input)
        {
            VectorModel model = new("word", "vsTestDatabase");
            MutableDictionaryObject testDic = new();
            testDic.SetValue("word", input["input"].ToString());
            DictionaryObject value = model.Predict(testDic);
            return value.GetValue("vector");
        }

        public static List<object> Query([NotNull] NameValueCollection args,
                                  [NotNull] IReadOnlyDictionary<string, object> postBody,
                                  [NotNull] HttpListenerResponse response)
        {
            string term = postBody["term"].ToString();

            NameValueCollection embeddingArgs = new()
            {
                { "input", term }
            };

            object embeddedTerm = GetEmbedding(embeddingArgs);


            string sql = postBody["sql"].ToString();

            Database db = (Database)postBody["database"];

            IQuery query = db.CreateQuery(sql);
            query.Parameters.SetValue("vector", embeddedTerm);

            List<object> resultArray = new();
            foreach (Result row in query.Execute())
            {
                resultArray.Add(row.ToDictionary());
            }

            return resultArray;
        }

    }



    public sealed class VectorModel : IPredictiveModel
    {
        public string name;
        public string key;

        public VectorModel(string key, string name)
        {
            this.name = name;
            this.key = key;
        }
        public DictionaryObject? Predict(DictionaryObject input)
        {
            throw new NotImplementedException();
        }
    }
}