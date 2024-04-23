using System;
using System.IO;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;
using System.Net;
using System.Threading.Tasks;
using Couchbase.Lite.Query;
using JetBrains.Annotations;
using Newtonsoft.Json.Linq;

using static Couchbase.Lite.Testing.DatabaseMethods;

namespace Couchbase.Lite.Testing
{
    public static class VectorSearchMethods
    {
        public static void createIndex([NotNull] NameValueCollection args,
                                       [NotNull] IReadOnlyDictionary<string, object> postBody,
                                       [NotNull] HttpListenerResponse response)
        {
            // using db get defualt collection and store in Collection collection
            With<Database>(postBody, "database", database =>
            {
                Collection collection = database.GetDefaultCollection();

                if (collection == null)
                {
                    throw new InvalidOperationException("Could not open specified collection");
                }

                // get values from postBody
                string indexName = postBody["indexName"].ToString();
                string expression = postBody["expression"].ToString();

                _ = Int32.TryParse(postBody["dimensions"], out int dimensions);
                _ = Int32.TryParse(postBody["centroids"], out int centroids);
                // IExpression dimensions = expression.Int((int)postBody["dimensions"]);
                // IExpression centroids = expression.Int((int)postBody["centroids"]);

                VectorEncoding.ScalarQuantizerType scalarEncoding = postBody["scalarEncoding"]; // unsure about the types and get method here

                _ = Int32.TryParse(postBody["subquantizers"], out int subquantizers);
                _ = Int32.TryParse(postBody["bits"], out int bits);
                // IExpression subquantizers = expression.Int((int)postBody["subquantizers"]);
                // IExpression bits = expression.Int((int)postBody["bits"]);

                string metric = postBody["metric"].ToString();

                _ = Int32.TryParse(postBody["minTrainingSize"], out int minTrainingSize);
                _ = Int32.TryParse(postBody["maxTrainingSize"], out int maxTrainingSize);
                // IExpression minTrainingSize = expression.Int((int)postBody["minTrainingSize"]);
                // IExpression maxTrainingSize = expression.Int((int)postBody["maxTrainingSize"]);

                // correctness checks
                if (scalarEncoding != null && (bits != null || subquantizers != null))
                {
                    throw new InvalidOperationException("Cannot have scalar quantization and arguments for product quantization at the same time");
                }

                if ((bits != null && subquantizers == null) || (bits == null && subquantizers != null))
                {
                    throw new InvalidOperationException("Product quantization requires both bits and subquantizers set");
                }

                VectorIndexConfiguration config = new VectorIndexConfiguration(expression, dimensions, centroids); // unure on types here again, undocumented specifics

                if (scalarEncoding != null)
                {
                    config.SetEncoding(VectorEncoding.ScalarQuantizer(scalarEncoding));
                }
                if (bits != null)
                {
                    config.SetEncoding(VectorEncoding.ProductQuantizer(subquantizers, bits));
                }
                if (metric != null)
                {
                    switch (metric)
                    {
                        case "euclidean":
                            config.SetMetric(VectorIndexConfiguration.DistanceMetric.EUCLIDEAN);
                            break;
                        case "cosine":
                            config.SetMetric(VectorIndexConfiguration.DistanceMetric.COSINE);
                            break;
                        default:
                            throw new Exception("Invalid distance metric");
                    }
                }

                if (minTrainingSize != null)
                {
                    config.SetMinTrainingSize(minTrainingSize);
                }
                if (maxTrainingSize != null)
                {
                    config.MaxTrainingSize(maxTrainingSize);
                }

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

    }
}