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

                // get values from postBody
                string indexName = postBody["indexName"].ToString();
                string expression = postBody["expression"].ToString();

                // null coalescing checks
                var dimensions = postBody["dimensions"] as uint? ?? null;
                Console.WriteLine("================ dimensions = " + dimensions);

                var centroids = postBody["centroids"] as uint? ?? null;
                Console.WriteLine("================ centroids = " + centroids);

                var minTrainingSize = postBody["minTrainingSize"] as uint? ?? null;
                Console.WriteLine("================ minTrainingSize = " + minTrainingSize);

                var maxTrainingSize = postBody["maxTrainingSize"] as uint? ?? null;
                Console.WriteLine("================ maxTrainingSize = " + maxTrainingSize);

                uint? bits = 0;
                uint? subquantizers = 0;
                ScalarQuantizerType? scalarEncoding = new();

                try
                {
                    bits = (uint)postBody["bits"];
                    subquantizers = (uint)postBody["subquantizers"];
                }
                catch (Exception e)
                {
                    bits = null;
                    subquantizers = null;
                }
                Console.WriteLine("================ bits = " + bits);
                Console.WriteLine("================ subquantizers = " + subquantizers);

                try
                {
                    scalarEncoding = (ScalarQuantizerType)postBody["scalarEncoding"];
                }
                catch (Exception e)
                {
                    scalarEncoding = null;
                }
                Console.WriteLine("================ scalarEncoding = " + scalarEncoding);




                string metric = postBody["metric"].ToString();
                Console.WriteLine("================ metric = " + metric);


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
                VectorEncoding encoding = null;
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

                VectorIndexConfiguration config = new(expression, (uint)dimensions, (uint)centroids) // unure on types here again, undocumented specifics
                {
                    Encoding = encoding,
                    DistanceMetric = dMetric,
                    MinTrainingSize = (uint)minTrainingSize,
                    MaxTrainingSize = (uint)maxTrainingSize
                };


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