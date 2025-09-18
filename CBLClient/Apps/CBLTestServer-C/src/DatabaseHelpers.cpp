#include "DatabaseHelpers.h"

#include "Defer.hh"
#include "Defines.h"

#include INCLUDE_CBL(CouchbaseLite.h)

void withDefaultCollection(const CBLDatabase* db, const std::function<void(CBLCollection*)>& fn) {
    CBLError err {};
    CBLCollection* collection = CBLDatabase_DefaultCollection(db, &err);
    DEFER { CBLCollection_Release(collection); };
    TRY(collection, err)
    fn(collection);
}