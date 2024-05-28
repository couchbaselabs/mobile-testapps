#include "VectorSearchMethods.h"
#include "MemoryMap.h"
#include "Router.h"
#include "Defines.h"
#include "Defer.hh"
#include <iostream>
#include <vector>

using namespace nlohmann;
using namespace std;
using namespace fleece;

#include INCLUDE_CBL(CouchbaseLite.h)

namespace vectorSearch_methods
{

    void vectorSearch_createIndex(json& body, mg_connection* conn)
    {
        auto scopeName = body["scopeName"].get<string>();
        auto collectionName = body["collectionName"].get<string>();
        auto indexName = body["indexName"].get<string>();
        auto expression = body["expression"].get<string>();
        auto metric = body["metric"].get<string>();

        auto dimensions = body["dimensions"].get<uint32_t>();
        auto centroids = body["centroids"].get<uint32_t>();
        auto minTrainingSize = body["minTrainingSize"].get<uint32_t>();
        auto maxTrainingSize = body["maxTrainingSize"].get<uint32_t>();

        std::optional<uint32_t> bits;
        std::optional<uint32_t> subquantizers;
        std::optional<CBLScalarQuantizerType> scalarEncoding;
        std::optional<CBLDistanceMetric> dMetric;

        auto encoding = CBLVectorEncoding_CreateNone;

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
            encoding = CBLVectorEncoding_CreateScalarQuantizer(scalarEncoding.value());
        }
        if (bits.has_value() && subquantizers.has_value())
        {
            encoding = CBLVectorEncoding_CreateProductQuantizer(subquantizers.value(), bits.value());
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

        with<CBLDatabase *>(body, "database", [conn](CBLDatabase* db)
                            {

                                CBLVectorIndexConfiguration config{kCBLN1QLLanguage, expression, dimensions, centroids};
                                config.encoding = encoding;
                                config.metric = metric;
                                config.minTrainingSize = minTrainingSize;
                                config.maxTrainingSize = maxTrainingSize;

                                CBLError err = {};
                                auto collection = CBLDatabase_Collection(db, collectionName, scopeName, &err);

                                try
                                {
                                    CBLCollection_CreateVectorIndex(collection, indexName, config);
                                    std::cout << "Successfully created index" << std::endl;
                                    write_serialized_body(conn, "Created index with name " + indexName);
                                }
                                catch (const std::exception &e)
                                {
                                    std::cout << "Failed to create index" << std::endl;
                                    write_serialized_body(conn, "Could not create index: " + e.what());
                                } });
    }
}