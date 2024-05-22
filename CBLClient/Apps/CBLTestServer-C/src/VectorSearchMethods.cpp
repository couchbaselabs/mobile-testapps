#include "VectorSearchMethods.h"
#include "MemoryMap.h"
#include "Router.h"
#include "Defines.h"
#include "Defer.hh"

using namespace nlohmann;
using namespace std;
using namespace fleece;

#include INCLUDE_CBL(CouchbaseLite.h)

namespace vectorSearch_methods
{

    void vectorSearch_createIndex(json &body, mg_connection *conn)
    {
        auto scopeName = body["scopeName"].get<string>();
        auto collectionName = body["collectionName"].get<string>();
        auto indexName = body["indexName"].get<string>();
        auto expression = body["expression"].get<string>();

        auto dimensions = body["dimensions"].get<uint32_t>();
        auto centroids = body["centroids"].get<uint32_t>();
        auto minTrainingSize = body["minTrainingSize"].get<uint32_t>();
        auto maxTrainingSize = body["maxTrainingSize"].get<uint32_t>();

        std::optional<uint32_t> bits;
        std::optional<uint32_t> subquantizers;
        std::optional<CBLScalarQuantizerType> scalarEncoding;

        try
        {
            bits = std::any_cast<uint32_t>(body["bits"]);
            subquantizers = std::any_cast<uint32_t>(body["subquantizers"]);
        }
        catch (...)
        {
            bits.reset();
            subquantizers.reset();
        }

        try
        {
            scalarEncoding = std::any_cast<CBLScalarQuantizerType>(body["scalarEncoding"]);
        }
        catch (...)
        {
            scalarEncoding.reset();
        }

        with<CBLDatabase *>(body, "database", [conn](CBLDatabase *db)
                            {
                                CBLError err = {};
                                auto collection = CBLDatabase_Collection(db, flstr(collectionName), flstr(scopeName), &err); });
    }

}