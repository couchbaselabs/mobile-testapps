#include "VectorSearchMethods.h"
#include "MemoryMap.h"
#include "Router.h"
#include "Defines.h"
#include "Defer.hh"
#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

using namespace nlohmann;
using namespace std;
using namespace fleece;

#include INCLUDE_CBL(CouchbaseLite.h)



static void CBLDatabase_EntryDelete(void* ptr) {
    CBLDatabase_Release(static_cast<CBLDatabase *>(ptr));
}
namespace vectorSearch_methods
{
    public static map<string, object> wordMap;
    private static string InMemoryDbName = "vsTestDatabase";


    void vectorSearch_createIndex(json& body, mg_connection* conn)
    {
            const auto scopeName = body["scopeName"].get<string>();
            const auto collectionName = body["collectionName"].get<string>();
            const auto indexName = body["indexName"].get<string>();
            const auto expression = body["expression"].get<string>();
            const auto metric = body["metric"].get<string>();
            const auto dimensions = body["dimensions"].get<uint32_t>();
            const auto centroids = body["centroids"].get<uint32_t>();
            const auto minTrainingSize = body["minTrainingSize"].get<uint32_t>();
            const auto maxTrainingSize = body["maxTrainingSize"].get<uint32_t>();
            std::optional<uint32_t> bits;
            std::optional<uint32_t> subquantizers;
            std::optional<CBLScalarQuantizerType> scalarEncoding;
            CBLDistanceMetric dMetric;

            //static char *var = "LD_LIBRARY_PATH=/root/ctestserver/Extensions";
            //putenv(var);
            
            //std::filesystem::create_symlink('/root/ctestserver/Extensions/libgomp.so.1', '/root/ctestserver/Extensions/libgomp.so.1.0.0');
            auto* encoding = CBLVectorEncoding_CreateNone();
            try
            {
                bits = static_cast<uint32_t>(body["bits"]);
                subquantizers = static_cast<uint32_t>(body["subquantizers"]);
            }
            catch (...)
            {
                bits.reset();
                subquantizers.reset();
            }

            try
            {
                scalarEncoding = static_cast<CBLScalarQuantizerType>(body["scalarEncoding"]);
            }
            catch (...)
            {
                scalarEncoding.reset();
            }

            if (scalarEncoding.has_value() && (bits.has_value() || subquantizers.has_value()))
            {
                throw std::invalid_argument("Cannot have scalar quantization and arguments for product quantization at the same time");
            }

            if ((bits.has_value() && !subquantizers.has_value()) || (!bits.has_value() && subquantizers.has_value()))
            {
                throw std::invalid_argument("Product quantization requires both bits and subquantizers set");
            }

            if (scalarEncoding.has_value())
            {
                encoding = static_cast<CBLVectorEncoding*>(CBLVectorEncoding_CreateScalarQuantizer(scalarEncoding.value()));
            }
            if (bits.has_value() && subquantizers.has_value())
            {
                encoding = static_cast<CBLVectorEncoding*>(CBLVectorEncoding_CreateProductQuantizer(subquantizers.value(), bits.value()));
            }

            if (metric == "euclidean")
            {
                dMetric = kCBLDistanceMetricEuclidean;
            }
            else if (metric == "cosine")
            {
                dMetric = kCBLDistanceMetricCosine;
            }
            else
            {
                throw std::invalid_argument("Invalid distance metric");
            }

            // CBLVectorIndexConfiguration config{kCBLN1QLLanguage, expression, dimensions, centroids};
            CBLVectorIndexConfiguration config = {};
            config.expression = flstr(expression);
            config.dimensions = dimensions;
            config.encoding = encoding;
            config.metric = dMetric;
            config.centroids = centroids;
            config.minTrainingSize = minTrainingSize;
            config.maxTrainingSize = maxTrainingSize;
            config.expressionLanguage = kCBLN1QLLanguage;
            with<CBLDatabase *>(body,"database", [conn, collectionName, scopeName, indexName, config](CBLDatabase* db)
            {
                CBLError err;
                CBLCollection* collection;
                TRY(collection = CBLDatabase_CreateCollection(db, flstr(collectionName),  flstr(scopeName), &err), err);

               // try
               // {
                ofstream MyFile("/root/ctestserver/gilad_log.txt");
                MyFile << "After create collection\n";
                MyFile.close();
                TRY(CBLCollection_CreateVectorIndex(collection, flstr(indexName), config, &err), err);
                MyFile.open("/root/ctestserver/gilad_log.txt", std::ios_base::app);
                MyFile << "index created successfully";
                MyFile.close();
                std::cout << "Successfully created index" << std::endl;
                write_serialized_body(conn, "Created index with name " + indexName);
                //}
               // catch (const std::exception &e)
               // {
               //     std::cout << "Failed to create index" << std::endl;
               //     write_serialized_body(conn, "Could not create index: " + std::string(e.what()));
               // }
            });
    }

    void vectorSearch_loadDatabase(json& body, mg_connection* conn) {
        const auto dbPath = "Databases/vsTestDatabase.cblite2/";
        const auto dbName = "vsTestDatabase";
  
        CBL_SetExtensionPath(flstr("/root/ctestserver/Extensions"));
        char cwd[1024];
        cbl_getcwd(cwd, 1024);
        const auto databasePath = string(cwd) + DIRECTORY_SEPARATOR + dbPath;
        CBLDatabaseConfiguration* databaseConfig = nullptr;
        CBLError err;
        CBLDatabase* db;
        TRY(CBL_CopyDatabase(flstr(databasePath), flstr(dbName), databaseConfig, &err), err);
        // to rename the folder because it is copied to be under "r" for some reason
        rename("/root/ctestserver/r", dbName);
        wordMap = vectorSearch_getWordMap();
        TRY(db = CBLDatabase_Open(flstr(dbName), databaseConfig, &err), err);
         write_serialized_body(conn, memory_map::store(db, CBLDatabase_EntryDelete));
    }
   
   void vectorSearch_registerModel(json& body, mg_connection* conn) {
        const auto name = body["name"].get<string>();
        const auto key = body["key"].get<string>();

        CBLPredictiveModel model;
        CBL_RegisterPredictiveModel(flstr(name), model);
        write_serialized_body(conn, "Model registered");
    }

    map<string, object> getWordMap() {
        const auto db = new CBLDatabase(InMemoryDbName);
        try {
            /* string sql1 = string.Format("select word, vector from auxiliaryWords");
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
            db.Close();
            return words; */
        }
        catch (exception e) {
            /* Console.WriteLine(e + "retrieving vector could not be done - getWordVector query returned no results");
            db.Close();
            return null; */
        }
    }
    
    void vectorSearch_getEmbedding(json& body, mg_connection* conn) {
        /* auto value = GetEmbeddingDic(postBody["input"].ToString());
        Dictionary<String, Object> vectorDict = value.ToDictionary();
        List<object> embedding = (List<object>)vectorDict["vector"];
        response.WriteBody(embedding); */
    }


    void vectorSearch_query(json& body, mg_connection* conn) {
        const auto term = body["term"].get<string>();

        with<CBLDatabase *>(body,"database", [conn, term](CBLDatabase* db)
            {
                /* DictionaryObject embeddedTermDic = GetEmbeddingDic(term.ToString());
                var embeddedTerm = embeddedTermDic.GetValue("vector");
                string sql = postBody["sql"].ToString();
                Console.WriteLine("QE-DEBUG Calling query string: " + sql);

                IQuery query = db.CreateQuery(sql);
                query.Parameters.SetValue("vector", embeddedTerm);

                List<object> resultArray = new();
                int c = 0;
                foreach (Result row in query.Execute())
                {
                    resultArray.Add(row.ToDictionary());
                    c++;
                }
                response.WriteBody(resultArray); */
            }
    }

    map<string, object> getEmbeddingDic(string term) {
        /* VectorModel model = new("word");
        MutableDictionaryObject testDic = new();
        testDic.SetValue("word", term);
        DictionaryObject value = model.Predict(testDic);
        return value; */
    }

    /* public sealed class VectorModel : IPredictiveModel
        {
            public string key;

            public VectorModel(string key)
            {
                this.key = key;
                Console.WriteLine("QE-DEBUG Vector Model object instantiated with key: " + key);
            }

            public DictionaryObject? Predict(DictionaryObject input)
            {
                Console.WriteLine("QE-DEBUG Calling predict function");
                string inputWord = input.GetString(key);
                Console.WriteLine("QE-DEBUG Predicting for word: " + inputWord);
                object result = new();
                result = wordMap.GetValue(inputWord);
                if (result == null) {
                    Console.WriteLine("QE-DEBUG Prediction gave null result");
                } else {
                    Console.WriteLine("QE-DEBUG Prediction gave non-null result");
                }
                MutableDictionaryObject output = new();
                output.SetValue("vector", result);
                return output;
            }
        }
        */
    /* public static void RegisterModel([NotNull] NameValueCollection args,
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
        */
}