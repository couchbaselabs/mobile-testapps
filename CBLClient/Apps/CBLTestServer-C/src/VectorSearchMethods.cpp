#include "VectorSearchMethods.h"
#include "MemoryMap.h"
#include "Router.h"
#include "Defines.h"
#include "Defer.hh"
#include "FleeceHelpers.h"

#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

using namespace nlohmann;
using namespace std;
using namespace fleece;

#include INCLUDE_CBL(CouchbaseLite.h)

static FLMutableDict wordMap;
ofstream MyFile("/root/ctestserver/gilad_log.txt");


static void appendLogMessage(string msg) {
    MyFile.open("/root/ctestserver/gilad_log.txt", std::ios_base::app);
    MyFile << msg;
    MyFile.close();
}

FLMutableDict getPrediction(FLDict input, string key) {
    const FLValue inputWord = FLDict_Get(input, flstr(key));
    appendLogMessage("After getting inputWord");
    FLMutableDict predictResult =  FLMutableDict_New();
    if (inputWord) {
        const FLValue embeddingsVector = FLDict_Get(wordMap, FLValue_AsString(inputWord));
        appendLogMessage("embeedingw");
        appendLogMessage(to_string(FLValue_AsString(embeddingsVector)));
        FLMutableDict_SetValue(predictResult, flstr("vector"), embeddingsVector);
    }
    return predictResult;
}

FLSliceResult predictFunction(void* context, FLDict input) {
    FLMutableDict predictionResult = FLMutableDict_New();
    predictionResult = getPrediction(input, "word");
    FLEncoder enc = FLEncoder_New();
    FLEncoder_BeginDict(enc, 1);
    FLEncoder_WriteValue(enc, (FLValue)predictionResult);
    FLEncoder_EndDict(enc);
    FLMutableDict_Release(predictionResult);
    return FLEncoder_Finish(enc, nullptr); 
}

class VectorModel : public CBLPredictiveModel {
    private:
    string key;

    public:
    VectorModel(string key) {
        this->key=key;
    }

    FLSliceResult Predict(void* context, FLDict input) {
      FLMutableDict predictionResult = FLMutableDict_New();
      predictionResult = getPrediction(input, this->key);
      FLEncoder enc = FLEncoder_New();
      FLEncoder_BeginDict(enc, 1);
      FLEncoder_WriteValue(enc, (FLValue)predictionResult);
      FLEncoder_EndDict(enc);
      FLMutableDict_Release(predictionResult);
      return FLEncoder_Finish(enc, nullptr); 
    }
};

static VectorModel* predictionVectorModel;

static void CBLDatabase_EntryDelete(void* ptr) {
    CBLDatabase_Release(static_cast<CBLDatabase *>(ptr));
}


static FLMutableDict getWordMap() {
         MyFile << "start\n";
         MyFile.close();
         std::string sql1 = "select word, vector from auxiliaryWords";
         std::string sql2 = "select word, vector from searchTerms";
         CBLError err;
         CBLDatabase* db;
         CBLQuery* query1;
         CBLQuery* query2;
         CBLResultSet* rs1; 
         CBLResultSet* rs2;
         FLMutableDict words = FLMutableDict_New();
         TRY(db = CBLDatabase_Open(flstr("vsTestDatabase"), nullptr, &err), err);
         TRY(query1 = CBLDatabase_CreateQuery(db, kCBLN1QLLanguage, flstr(sql1), nullptr, &err), err);
         TRY(query2 = CBLDatabase_CreateQuery(db, kCBLN1QLLanguage, flstr(sql2), nullptr, &err), err);
         TRY(rs1 = CBLQuery_Execute(query1, &err), err);
         TRY(rs2 = CBLQuery_Execute(query2, &err), err);
         while(CBLResultSet_Next(rs1)) {
            FLValue word = CBLResultSet_ValueForKey(rs1, flstr("word"));
            FLValue vector = CBLResultSet_ValueForKey(rs1, flstr("vector"));
            string sword = to_string(FLValue_AsString(word));
            string svector = to_string(FLValue_AsString(vector));
            if (vector) {
                FLMutableDict_SetValue(words, FLValue_AsString(word), vector);
            };

         }
         CBLQuery_Release(query1);
         CBLResultSet_Release(rs1);
         while(CBLResultSet_Next(rs2)) {
            FLValue word = CBLResultSet_ValueForKey(rs2, flstr("word"));
            FLValue vector = CBLResultSet_ValueForKey(rs2, flstr("vector"));
            if (vector) {
                appendLogMessage("vector=");
                appendLogMessage(to_string(FLValue_AsString(vector)));
                appendLogMessage("\n");
                FLMutableDict_SetValue(words, FLValue_AsString(word), vector);
            }
         }
         CBLQuery_Release(query2);
         TRY(CBLDatabase_Close(db, &err), err);
         return words;
}

FLDict getEmbeddingDic(string term) {
    //VectorModel* model = new VectorModel("word");
    FLMutableDict testDict = FLMutableDict_New();
    FLMutableDict_SetString(testDict, flstr("word"), flstr(term));
    FLDict value = getPrediction(testDict, "word");
    FLMutableDict_Release(testDict);
    return value;
}
namespace vectorSearch_methods
{
    const string InMemoryDbName = "vsTestDatabase";

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
            appendLogMessage("Before creating the index");
            with<CBLDatabase *>(body,"database", [conn, collectionName, scopeName, indexName, config](CBLDatabase* db)
            {
                CBLError err;
                CBLCollection* collection;
                TRY(collection = CBLDatabase_CreateCollection(db, flstr(collectionName),  flstr(scopeName), &err), err);
                appendLogMessage("Just before creating the index");
                TRY(CBLCollection_CreateVectorIndex(collection, flstr(indexName), config, &err), err);
                appendLogMessage("After creating hte index");
                write_serialized_body(conn, "Created index with name " + indexName);
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
        wordMap = getWordMap();
        FLDictIterator iter;
        FLDictIterator_Begin(wordMap, &iter);
        FLValue value;
        while (NULL != (value = FLDictIterator_GetValue(&iter))) {
            FLString key = FLDictIterator_GetKeyString(&iter);
            appendLogMessage("key= " + to_string(key) + " ");
            appendLogMessage("value= " + to_string(FLValue_AsString(value)) + "\n");
            FLDictIterator_Next(&iter);
        }
        TRY(db = CBLDatabase_Open(flstr(dbName), databaseConfig, &err), err);
         write_serialized_body(conn, memory_map::store(db, CBLDatabase_EntryDelete));
    }
   
   void vectorSearch_registerModel(json& body, mg_connection* conn) {
        const auto name = body["name"].get<string>();
        const auto key = body["key"].get<string>();

        CBLPredictiveModel model = {};
        //predictionVectorModel = new VectorModel(key);
        model.context = nullptr;
        model.prediction = predictFunction;
        CBL_RegisterPredictiveModel(flstr(name), model);
        appendLogMessage("Registered the model");
        write_serialized_body(conn, "Model registered");
    }
    
    void vectorSearch_getEmbedding(json& body, mg_connection* conn) {
        auto vectorDict = getEmbeddingDic(body["input"].get<string>());
        FLValue embedding = FLDict_Get(vectorDict, flstr("vector"));
        write_serialized_body(conn, embedding);
    }


    void vectorSearch_query(json& body, mg_connection* conn) {

        with<CBLDatabase *>(body,"database", [conn, body](CBLDatabase* db)
            {
                auto embeddedTermDic = getEmbeddingDic(body["term"].get<string>());
                auto embeddedTerm = FLDict_Get(embeddedTermDic, flstr("vector"));
                auto sql = body["sql"].get<string>();
                json retVal = json::array();
                CBLError err;

                CBLQuery* query;
                TRY((query = CBLDatabase_CreateQuery(db, kCBLN1QLLanguage, flstr(sql), nullptr, &err)), err)
                DEFER {
                    CBLQuery_Release(query);
                };

                FLMutableDict qParam = FLMutableDict_New();
                FLMutableDict_SetValue(qParam, flstr("vector"), embeddedTerm);
                CBLQuery_SetParameters(query, FLDict(qParam));
                
                CBLResultSet* results;
                TRY(results = CBLQuery_Execute(query, &err), err);
                DEFER {
                    CBLResultSet_Release(results);
                };

                while(CBLResultSet_Next(results)) {
                    FLDict nextDict = CBLResultSet_ResultDict(results);
                    FLStringResult nextJSON = FLValue_ToJSON((FLValue)nextDict);
                    json next = json::parse(string((const char *)nextJSON.buf, nextJSON.size));
                    retVal.push_back(next);
                }
                FLMutableDict_Release(qParam);
                write_serialized_body(conn, retVal);
            });
    }

}