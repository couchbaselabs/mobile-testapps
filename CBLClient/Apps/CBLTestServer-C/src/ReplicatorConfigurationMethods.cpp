#include "ReplicatorConfigurationMethods.h"
#include "MemoryMap.h"
#include "Defines.h"
#include "FleeceHelpers.h"
#include "Router.h"

#include <string>
#include <algorithm>
#include <unordered_set>
#include <thread>
#include <cctype>
#include INCLUDE_CBL(CouchbaseLite.h)

using namespace nlohmann;
using namespace std;

static void tolower(string& str) {
    transform(str.begin(), str.end(), str.begin(), [](unsigned char c) {
    return tolower(c);
    });
}

static bool replicator_boolean_filter_callback(void* context, CBLDocument* doc, CBLDocumentFlags flags) {
    const auto* properties = CBLDocument_Properties(doc);
    FLValue val = FLDict_Get(properties, FLSTR("new_field_1"));
    return val != nullptr;
}

static bool replicator_deleted_filter_callback(void* context, CBLDocument* doc, CBLDocumentFlags flags) {
    return flags & kCBLDocumentFlagsDeleted;
}

static void checkMismatchDocID(const CBLDocument* localDoc, const CBLDocument* remoteDoc, string&& docId) {
    const auto localID = to_string(CBLDocument_ID(localDoc));
    const auto remoteID = to_string(CBLDocument_ID(remoteDoc));
    if(docId != localID || docId != remoteID) {
        throw domain_error("DocID mismatch");
    }
}

static const CBLDocument* local_wins_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    // Retain this so we can release the return value of any conflict resolver unconditionally
    return CBLDocument_Retain(localDocument);
}

static const CBLDocument* remote_wins_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));
    
    // Retain this so we can release the return value of any conflict resolver unconditionally
    return CBLDocument_Retain(remoteDocument);
}

static const CBLDocument* null_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    return nullptr;
}

static const CBLDocument* merge_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    auto remoteProperties = CBLDocument_Properties(remoteDocument);
    auto localProperties = CBLDocument_Properties(localDocument);
    if(FLDict_Count(remoteProperties) == 0) {
        return localDocument;
    }

    CBLDocument* newDoc = CBLDocument_MutableCopy(localDocument);
    auto properties = CBLDocument_MutableProperties(newDoc);
    FLDictIterator i;
    FLDictIterator_Begin(remoteProperties, &i);
    do {
        auto key = FLDictIterator_GetKeyString(&i);
        const auto* val = FLDict_Get(localProperties, key);
        if(val != nullptr) {
            FLSlot slot = FLMutableDict_Set(properties, key);
            FLSlot_SetValue(slot, FLDictIterator_GetValue(&i));
        }
    } while(FLDictIterator_Next(&i));

    return newDoc;
}

static const CBLDocument* incorrect_docid_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    string nid = string("changed") + to_string(documentID);
    FLString newId = flstr(nid);
    CBLDocument* newDoc = CBLDocument_CreateWithID(newId);
    FLMutableDict localProperties = FLDict_MutableCopy(CBLDocument_Properties(localDocument), kFLDefaultCopy);
    FLSlot slot = FLMutableDict_Set(localProperties, FLSTR("new_value"));
    FLSlot_SetString(slot, FLSTR("couchbase"));
    CBLDocument_SetProperties(newDoc, localProperties);
    FLMutableDict_Release(localProperties);

    return newDoc;
}

static const CBLDocument* delete_doc_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    return remoteDocument ? nullptr : localDocument;
}

static const CBLDocument* exception_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    throw runtime_error("Throwing an exception");
}

static const CBLDocument* delayed_local_win_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));

    this_thread::sleep_for(10s);

    return localDocument;
}

namespace replicator_configuration_methods {
    void replicatorConfiguration_create(json& body, mg_connection* conn) {
        with<CBLDatabase *>(body, "source_db", [conn, &body](CBLDatabase* db)
        {
            auto config = static_cast<CBLReplicatorConfiguration *>(malloc(sizeof(CBLReplicatorConfiguration)));
            config->database = db;
            if(body.contains("target_url")) {
                const auto url = body["target_url"].get<string>();
                auto* endpoint = CBLEndpoint_NewWithURL(flstr(url));
                config->endpoint = endpoint;
            } else if(body.contains("target_db")) {
                // TODO
                throw domain_error("target_db not implemented yet");
            } else {
                throw domain_error("Illegal arguments provided");
            }

            if(body.contains("continuous")) {
                config->continuous = body["continuous"].get<bool>();
            }

            if(body.contains("channels")) {
                FLMutableArray channels = FLMutableArray_New();
                for(const auto& c : body["channels"]) {
                    writeFleece(channels, c);
                }

                config->channels = channels;
            }

            if(body.contains("documentIDs")) {
                FLMutableArray docIDs = FLMutableArray_New();
                for(const auto& c : body["documentIDs"]) {
                    writeFleece(docIDs, c);
                }

                config->documentIDs = docIDs;
            }

            if(body.contains("auhenticator")) {
                config->authenticator = static_cast<CBLAuthenticator*>(memory_map::get(body["authenticator"].get<string>()));
            }

            if(body.contains("headers")) {
                FLMutableDict headers = FLMutableDict_New();
                for(const auto& [key, value] : body["headers"].items()) {
                    writeFleece(headers, key, value);
                }

                config->headers = headers;
            }

            if(body.contains("heartbeat")) {
                // TODO: Missing from C API
            }

            if(body.contains("pinnedservercert")) {
                // TODO: Need platform specific implementation
            }

            if(body.contains("replication_type")) {
                auto replicatorType = body["replication_type"].get<string>();
                tolower(replicatorType);
                if(replicatorType == "push") {
                    config->replicatorType = kCBLReplicatorTypePush;
                } else if(replicatorType == "pull") {
                    config->replicatorType = kCBLReplicatorTypePull;
                } else {
                    config->replicatorType = kCBLReplicatorTypePushAndPull;
                }
            }

            if(body.contains("push_filter") && body["push_filter"].get<bool>()) {
                const auto filterCallbackFunction = body["filter_callback_func"].get<string>();
                if(filterCallbackFunction == "boolean") {
                    config->pushFilter = replicator_boolean_filter_callback;
                } else if(filterCallbackFunction == "deleted") {
                    config->pushFilter = replicator_deleted_filter_callback;
                } else if(filterCallbackFunction == "access_revoked") {
                    // Need C API support
                    throw domain_error("Not implemented");
                }
            }

            if(body.contains("pull_filter") && body["pull_filter"].get<bool>()) {
                const auto filterCallbackFunction = body["filter_callback_func"].get<string>();
                if(filterCallbackFunction == "boolean") {
                    config->pullFilter = replicator_boolean_filter_callback;
                } else if(filterCallbackFunction == "deleted") {
                    config->pullFilter = replicator_deleted_filter_callback;
                } else if(filterCallbackFunction == "access_revoked") {
                    // Need C API support
                    throw domain_error("Not implemented");
                }
            }

            if(body.contains("conflict_resolver")) {
                const auto conflictResolver = body["conflict_resolver"].get<string>();
                if(conflictResolver == "local_wins") {
                    config->conflictResolver = local_wins_conflict_resolution;
                } else if(conflictResolver == "remote_wins") {
                    config->conflictResolver = remote_wins_conflict_resolution;
                } else if(conflictResolver == "null") {
                    config->conflictResolver = null_conflict_resolution;
                } else if(conflictResolver == "merge") {
                    config->conflictResolver = merge_conflict_resolution;
                } else if(conflictResolver == "incorrect_doc_id") {
                    config->conflictResolver = incorrect_docid_conflict_resolution;
                } else if(conflictResolver == "delayed_local_win") {
                    config->conflictResolver = delayed_local_win_conflict_resolution;
                } else if(conflictResolver == "delete_not_win") {
                    config->conflictResolver = delete_doc_conflict_resolution;
                } else if(conflictResolver == "exception_thrown") {
                    config->conflictResolver = exception_conflict_resolution;
                } else {
                    config->conflictResolver = CBLDefaultConflictResolver;
                }
            }

            write_serialized_body(conn, memory_map::store(config, free));
        });
    }

    void replicatorConfiguration_setAuthenticator(json& body, mg_connection* conn) {
        with<CBLReplicatorConfiguration *>(body, "configuration", [&body](CBLReplicatorConfiguration *repConf)
        {
            with<CBLAuthenticator *>(body, "authenticator", [repConf](CBLAuthenticator* auth)
            {
                repConf->authenticator = auth;
            });
        });

        mg_send_http_ok(conn, "application/text", 0);
    }

    void replicatorConfiguration_setReplicatorType(json& body, mg_connection* conn) {
        auto replicatorType = body["replication_type"].get<string>();
        tolower(replicatorType);
                
        with<CBLReplicatorConfiguration *>(body, "configuration", [&replicatorType](CBLReplicatorConfiguration *repConf)
        {
            if(replicatorType == "push") {
                repConf->replicatorType = kCBLReplicatorTypePush;
            } else if(replicatorType == "pull") {
                repConf->replicatorType = kCBLReplicatorTypePull;
            } else {
                repConf->replicatorType = kCBLReplicatorTypePushAndPull;
            }
        });

        mg_send_http_ok(conn, "application/text", 0);
    }

    void replicatorConfiguration_isContinuous(json& body, mg_connection* conn) {
        with<CBLReplicatorConfiguration *>(body, "configuration", [conn](CBLReplicatorConfiguration *repConf)
        {
            write_serialized_body(conn, repConf->continuous);
        });
    }
}
