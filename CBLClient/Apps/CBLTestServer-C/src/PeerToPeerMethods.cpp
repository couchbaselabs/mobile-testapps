// #include <openssl/pem.h>
// #include <openssl/x509.h>
// #include <openssl/x509v3.h>
// #include <openssl/ssl.h>
// #include <openssl/evp.h>
// #include <openssl/pkcs12.h>

#include "PeerToPeerMethods.h"
#include <cbl/CBLReplicator.h>
#include <cbl/CBLDatabase.h>
#include <iostream>
#include <string>
#include <mutex>
#include <vector>
#include <unordered_map>

#include "ReplicatorConfigurationMethods.h"
#include "EncryptableMethods.h"
#include "MemoryMap.h"
#include "Defines.h"
#include "FleeceHelpers.h"
#include "Router.h"
#include "FilePathResolver.h"

#include <algorithm>
#include <unordered_set>
#include <thread>
#include <cctype>
#include <fstream>

#include INCLUDE_CBL(CouchbaseLite.h)


#define CLIENT_CERT_LABEL "CBL-Client-Cert"
#define SERVER_CERT_LABEL "CBL-Server-Cert"
#define CLIENT_CA_CERT_PATH "certs/client.pem"
#define CLIENT_CA_KEY_PATH "certs/client-key.pem"
#define CERT_LOCATION "certs/ca.pem"
#define CERT_KEY_LOCATION "certs/ca-key.pem"


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
    return val == nullptr || FLValue_AsBool(val);
}

static bool replicator_deleted_filter_callback(void* context, CBLDocument* doc, CBLDocumentFlags flags) {
    return !(flags & kCBLDocumentFlagsDeleted);
}

static bool replicator_access_revoked_filter_callback(void* context, CBLDocument* doc, CBLDocumentFlags flags) {
    return !(flags & kCBLDocumentFlagsAccessRemoved);
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
    return localDocument;
}

static const CBLDocument* remote_wins_conflict_resolution(void *context, FLString documentID, 
    const CBLDocument *localDocument, const CBLDocument *remoteDocument) {
    if(!localDocument || !remoteDocument) {
        throw domain_error("Either local doc or remote doc is null");
    }

    checkMismatchDocID(localDocument, remoteDocument, to_string(documentID));
    return remoteDocument;
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
        if(!val) {
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

static const FLSliceResult readFile(const std::string& filepath) {
    std::ifstream inFile(filepath, std::ios::binary);
    if (!inFile.is_open()) {
        throw std::runtime_error("Failed to open file: " + filepath);
    }

    try {
        inFile.exceptions(std::ifstream::failbit | std::ifstream::badbit);

        std::vector<char> buffer((std::istreambuf_iterator<char>(inFile)),
                                  std::istreambuf_iterator<char>());

        return FLSliceResult_CreateWith(buffer.data(), buffer.size());
    } catch (const std::ios_base::failure& e) {
        throw std::runtime_error("Error reading file: " + filepath + " - " + e.what());
    }
}




static void CBLURLEndpointListener_EntryDelete(void* ptr ){
    // auto* config = (CBLURLEndpointListenerConfiguration *)ptr;
    // CBLTLSIdentity_Release(config->tlsIdentity);
    // free(config);
    auto listener = static_cast<CBLURLEndpointListener*>(ptr);
    // CBLURLEndpointListener_Release(listener);

}


static void CBLReplicator_EntryDelete(void* ptr) {
    CBLReplicator_Release(static_cast<CBLReplicator *>(ptr));
}


namespace peer_to_peer_methods {

    void peerToPeer_serverStart(nlohmann::json& body, mg_connection* conn) {

        auto config = static_cast<CBLURLEndpointListenerConfiguration *>(malloc(sizeof(CBLURLEndpointListenerConfiguration)));
        if (config == nullptr) {
            throw std::runtime_error("Memory allocation failed for CBLURLEndpointListenerConfiguration");
        }
        memset(config, 0, sizeof(CBLURLEndpointListenerConfiguration));

        vector<CBLCollection*>* vec = new vector<CBLCollection*>();

        if (body.contains("collections"))
        {
            for(const auto& c: body["collections"]) 
            {
                CBLCollection *rep_object = static_cast<CBLCollection*>(memory_map::get(c.get<string>()));
                if (rep_object == nullptr) {
                    throw std::runtime_error("Failed to map collection: " + c.get<std::string>());
                }
                vec->push_back(rep_object);
            }
        } else {
            auto dbname=body["database"].get<string>();
            CBLDatabase* db=static_cast<CBLDatabase*>(memory_map::get(dbname));
            if (!db) {
                throw std::runtime_error("Could not resolve database: " + dbname);
            }
            CBLError error{};
            CBLCollection* defaultCollection = CBLDatabase_DefaultCollection(db, &error);
            if (!defaultCollection) {
                throw std::runtime_error("Failed to get default collection: " + std::string(CBLError_Message(&error)));
            }
             vec->push_back(defaultCollection);
        }
        if (vec->empty()){
            throw std::runtime_error("Collection vector length is 0");
        }
        config->collections = vec->data();
        config->collectionCount = vec->size();
        if (body.contains("port")) {
            config->port = body["port"].get<int>();
        } 
        else {
            config->port = 0;
        }
        if (body.contains("tls_disable")){
            config->disableTLS = body["tls_disable"].get<bool>();
        }
        if (body.contains("tls_auth_type")){
            CBLKeyPair* keyPair = nullptr;
            auto tlsAuthType= body["tls_auth_type"].get<string>();
            if (tlsAuthType == "self_signed"){
                std::string keyFile = file_resolution::resolve_path(CERT_KEY_LOCATION, false); // .pem 
                FLSlice keyData = FLSliceResult_AsSlice(readFile(keyFile));

                std::string certFile = file_resolution::resolve_path(CERT_LOCATION, false); // .pem 
                FLSlice certData = FLSliceResult_AsSlice(readFile(certFile));
                CBLError error{};
                keyPair = CBLKeyPair_CreateWithPrivateKeyData(keyData, kFLSliceNull, &error);
                if (!keyPair){
                    throw(error);
                }
                CBLCert* certificate= CBLCert_CreateWithData(certData,&error);
                if (!certificate){
                    throw(error);
                }
                CBLTLSIdentity* identity = CBLTLSIdentity_IdentityWithKeyPairAndCerts(keyPair, certificate, &error);
                config->tlsIdentity = identity;
            }
            else if(tlsAuthType == "self_signed_create"){
                //TLSIdentity_DeleteIdentity(store, SERVER_CERT_LABEL, nullptr);
                // std::map<std::string, std::string> certAttributes;
                // certAttributes["CN"] = SERVER_CERT_LABEL;
                // CBLTLSIdentity* identity = CBLTLSIdentity_SelfSignedCertIdentity(certAttributes, kCBLKeyAlgorithmRSA, nullptr);
                // config->tlsIdentity = identity;
                CBLError error{};
                FLMutableDict attrDict = FLMutableDict_New();
                FLMutableDict_SetString(attrDict, kCBLCertAttrKeyCommonName, FLStr(SERVER_CERT_LABEL));
                FLDict attributes = FLValue_AsDict((FLValue)attrDict);
                CBLKeyUsages usage = kCBLKeyUsagesClientAuth;
                CBLTLSIdentity* identity = CBLTLSIdentity_CreateIdentity(usage, attributes, 0, kFLSliceNull, &error);
                config->tlsIdentity= identity;
                FLMutableDict_Release(attrDict);
            }
            if(body.contains("tls_authenticator")&& (body["tls_authenticator"].get<bool>())){
                //TLSIdentity_DeleteIdentity(store, SERVER_CERT_LABEL, nullptr);
                CBLError error{};
                std::string certFile = file_resolution::resolve_path(CLIENT_CA_CERT_PATH, false); // .pem 
                FLSlice certData = FLSliceResult_AsSlice(readFile(certFile));
                CBLCert* certificate= CBLCert_CreateWithData(certData,&error);
                if (certificate==nullptr){
                    throw(error);
                }
                CBLListenerAuthenticator* auth= CBLListenerAuth_CreateCertificateWithRootCerts(certificate);
                config->authenticator = auth;
            }
        }
        if (body.contains("enable_delta_sync")){
            config->enableDeltaSync=body["enable_delta_sync"].get<bool>();
        }
        CBLURLEndpointListener* listener;
        CBLError error = {};
        listener= CBLURLEndpointListener_Create(config, &error);
        if(!listener){
            throw error;
        }
        bool success= CBLURLEndpointListener_Start(listener, &error);
        if(!success){
            throw runtime_error("Server did not start");
        }
        write_serialized_body(conn, memory_map::store(listener, CBLURLEndpointListener_EntryDelete));

    }

    void peerToPeer_getListenerPort(nlohmann::json& body, mg_connection* conn) {
        with<CBLURLEndpointListener *>(body, "listener", [conn, &body](CBLURLEndpointListener* u){
            auto port= CBLURLEndpointListener_Port(u);
            write_serialized_body(conn,port );
        });
    }

    void peerToPeer_serverStop(nlohmann::json& body, mg_connection* conn) {
        if (body.contains("endPointType")){
          string endpointType=body["endPointType"].get<string>();
          if (endpointType == "MessageEndPoint"){
            //TODO: Message endpoint ops arent supported yet.
            return;
          }
          else{
            with<CBLURLEndpointListener *>(body, "listener", [conn, &body](CBLURLEndpointListener* u){
               auto port= CBLURLEndpointListener_Port(u);
               fprintf(stderr,"%d",port);
              CBLURLEndpointListener_Stop(u);
            });
          }
        }
        write_empty_body(conn);
    }

    void peerToPeer_clientStart(nlohmann::json& body, mg_connection* conn) {
        with<CBLReplicator *>(body, "replicator", [conn, &body](CBLReplicator* r)
        {
            CBLReplicator_Start(r, false);
        });
        write_empty_body(conn);
    }
    void peerToPeer_configure(nlohmann::json& body, mg_connection* conn) {
        with <CBLDatabase *>(body, "database", [conn, &body](CBLDatabase* db){
            int port = (int)body["port"].get<int>();
            string targetIP = body["host"].get<string>();
            string remote_DBName = body["serverDBName"].get<string>();
            string replicatorType = body["replicationType"].get<string>();
            string endPointType = body["endPointType"].get<string>();
            string filter_callback_func = body["filter_callback_func"].get<string>();
            bool tls_disable = body["tls_disable"].get<bool>();
            string tlsAuthType = body["tls_auth_type"].get<string>();
            bool tls_authenticator = body["tls_authenticator"].get<bool>();
            bool server_verification_mode = body["server_verification_mode"].get<bool>();
            CBLError err;
            string host_url="";
            if (tls_disable){
              host_url="ws://" + targetIP + ":" + std::to_string(port);
            }
            else{
              host_url="wss://" +targetIP+":"+std::to_string(port);
            }
            string db_url=host_url+"/"+remote_DBName;
            CBLEndpoint* endpoint;
            if (endPointType =="URLEndPoint"){
                TRY(endpoint = CBLEndpoint_CreateWithURL(flstr(db_url), &err), err);
            }
            auto config = static_cast<CBLReplicatorConfiguration *>(malloc(sizeof(CBLReplicatorConfiguration)));
            memset(config, 0, sizeof(CBLReplicatorConfiguration));
            config-> database= db;
            config-> endpoint= endpoint;
            tolower(replicatorType);
            if(replicatorType == "push") {
                config->replicatorType = kCBLReplicatorTypePush;
            } else if(replicatorType == "pull") {
                config->replicatorType = kCBLReplicatorTypePull;
            } else {
                config->replicatorType = kCBLReplicatorTypePushAndPull;
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
             if(body.contains("basic_auth")) {
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
                config->heartbeat = (unsigned int)stoul(body["heartbeat"].get<string>(), nullptr, 10);
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
            if(body.contains("auto_purge")) {
                auto autoPurge = body["auto_purge"].get<string>();
                tolower(autoPurge);
                if(autoPurge == "disabled") {
                    config->disableAutoPurge = true;
                } else {
                    config->disableAutoPurge = false;
                }
            }
            if(body.contains("push_filter") && body["push_filter"].get<bool>()) {
                const auto filterCallbackFunction = body["filter_callback_func"].get<string>();
                if(filterCallbackFunction == "boolean") {
                    config->pushFilter = replicator_boolean_filter_callback;
                } else if(filterCallbackFunction == "deleted") {
                    config->pushFilter = replicator_deleted_filter_callback;
                } else if(filterCallbackFunction == "access_revoked") {
                    config->pushFilter = replicator_access_revoked_filter_callback;
                }
            }

            if(body.contains("pull_filter") && body["pull_filter"].get<bool>()) {
                const auto filterCallbackFunction = body["filter_callback_func"].get<string>();
                if(filterCallbackFunction == "boolean") {
                    config->pullFilter = replicator_boolean_filter_callback;
                } else if(filterCallbackFunction == "deleted") {
                    config->pullFilter = replicator_deleted_filter_callback;
                } else if(filterCallbackFunction == "access_revoked") {
                    config->pullFilter = replicator_access_revoked_filter_callback;
                }
            }
             if (tlsAuthType == "self_signed")
            {
                std::string keyFile = file_resolution::resolve_path(CERT_KEY_LOCATION, false); // .pem 
                FLSlice keyData = FLSliceResult_AsSlice(readFile(keyFile));

                std::string certFile = file_resolution::resolve_path(CERT_LOCATION, false); // .pem 
                FLSlice certData = FLSliceResult_AsSlice(readFile(certFile));
                CBLError error{};
                CBLKeyPair* keyPair = CBLKeyPair_CreateWithPrivateKeyData(keyData, kFLSliceNull, &error);
                if (!keyPair){
                    throw(error);
                }
                CBLCert* certificate= CBLCert_CreateWithData(certData,&error);
                if (!certificate){
                    throw(error);
                }
                CBLTLSIdentity* identity = CBLTLSIdentity_IdentityWithKeyPairAndCerts(keyPair, certificate, &error);
                config->pinnedServerCertificate = certData;
            }
            if (tls_authenticator) {
                std::string keyFile = file_resolution::resolve_path(CLIENT_CA_KEY_PATH, false); // .pem 
                FLSlice keyData = FLSliceResult_AsSlice(readFile(keyFile));

                std::string certFile = file_resolution::resolve_path(CLIENT_CA_CERT_PATH, false); // .pem 
                FLSlice certData = FLSliceResult_AsSlice(readFile(certFile));
                CBLError error{};
                CBLKeyPair* keyPair = CBLKeyPair_CreateWithPrivateKeyData(keyData, kFLSliceNull, &error);
                if (!keyPair){
                    throw(error);
                }
                CBLCert* certificate= CBLCert_CreateWithData(certData,&error);
                if (!certificate){
                    throw(error);
                }
                CBLTLSIdentity* identity = CBLTLSIdentity_IdentityWithKeyPairAndCerts(keyPair, certificate, &error);
                CBLAuthenticator* auth=CBLAuth_CreateCertificate(identity);
                config->authenticator=auth;

            }
             if (server_verification_mode) {
                config->acceptOnlySelfSignedServerCertificate = true;
            }
            if (body.contains("max_retries") )
            {
                config->maxAttempts=body["max_retries"].get<int>();
            }
            if (body.contains("max_timeout") )
            {
                config->maxAttemptWaitTime=body["max_timeout"].get<int>();
            }
            if (!db) {
                fprintf(stderr, "Database is null!\n");
            }
            CBLReplicator* repl;
            repl = CBLReplicator_Create(config, &err);
            write_serialized_body(conn, memory_map::store(repl, CBLReplicator_EntryDelete));
            
        });
    }

    void peerToPeer_configureCollection(nlohmann::json& body, mg_connection* conn) {
        with<CBLDatabase *>(body, "database", [conn, &body](CBLDatabase* db){
            int port = (int)body["port"].get<int>();
            string targetIP = body["host"].get<string>();
            string remote_DBName = body["serverDBName"].get<string>();
            string replicatorType = body["replicationType"].get<string>();
            string endPointType = body["endPointType"].get<string>();
            bool tls_disable = body["tls_disable"].get<bool>();
            string tlsAuthType = body["tls_auth_type"].get<string>();
            bool tls_authenticator = body["tls_authenticator"].get<bool>();
            bool server_verification_mode = body["server_verification_mode"].get<bool>();
            CBLError err;
            string host_url="";
            if (tls_disable){
              host_url="ws://" + targetIP + ":" + std::to_string(port);
            }
            else{
              host_url="wss://" +targetIP+":" + std::to_string(port);
            }
            string db_url=host_url+"/"+ remote_DBName;
            CBLEndpoint* endpoint;
            if (endPointType =="URLEndPoint"){
                TRY(endpoint = CBLEndpoint_CreateWithURL(flstr(db_url), &err), err);
            }
            auto config = static_cast<CBLReplicatorConfiguration *>(malloc(sizeof(CBLReplicatorConfiguration)));
            memset(config, 0, sizeof(CBLReplicatorConfiguration));
            config-> database= db;
            config-> endpoint= endpoint;
            tolower(replicatorType);
            if(replicatorType == "push") {
                config->replicatorType = kCBLReplicatorTypePush;
            } else if(replicatorType == "pull") {
                config->replicatorType = kCBLReplicatorTypePull;
            } else {
                config->replicatorType = kCBLReplicatorTypePushAndPull;
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
             if(body.contains("basic_auth")) {
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
                config->heartbeat = (unsigned int)stoul(body["heartbeat"].get<string>(), nullptr, 10);
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
            if(body.contains("auto_purge")) {
                auto autoPurge = body["auto_purge"].get<string>();
                tolower(autoPurge);
                if(autoPurge == "disabled") {
                    config->disableAutoPurge = true;
                } else {
                    config->disableAutoPurge = false;
                }
            }
            if(body.contains("push_filter") && body["push_filter"].get<bool>()) {
                const auto filterCallbackFunction = body["filter_callback_func"].get<string>();
                if(filterCallbackFunction == "boolean") {
                    config->pushFilter = replicator_boolean_filter_callback;
                } else if(filterCallbackFunction == "deleted") {
                    config->pushFilter = replicator_deleted_filter_callback;
                } else if(filterCallbackFunction == "access_revoked") {
                    config->pushFilter = replicator_access_revoked_filter_callback;
                }
            }

            if(body.contains("pull_filter") && body["pull_filter"].get<bool>()) {
                const auto filterCallbackFunction = body["filter_callback_func"].get<string>();
                if(filterCallbackFunction == "boolean") {
                    config->pullFilter = replicator_boolean_filter_callback;
                } else if(filterCallbackFunction == "deleted") {
                    config->pullFilter = replicator_deleted_filter_callback;
                } else if(filterCallbackFunction == "access_revoked") {
                    config->pullFilter = replicator_access_revoked_filter_callback;
                }
            }
             if (tlsAuthType == "self_signed")
            {
                std::string certLocation = file_resolution::resolve_path(CERT_LOCATION, false); // .pem 
                std::ifstream certFile(certLocation, std::ios::binary);
                certFile.exceptions(certFile.failbit | certFile.badbit);
                certFile.seekg(0, ios::end);
                auto fileSize = certFile.tellg();
                certFile.seekg(0, ios::beg);
                FLSlice s {
                    malloc(fileSize),
                    (size_t)fileSize
                };
                certFile.read((char *)s.buf, fileSize);
                certFile.close();
                CBLError error;
                CBLKeyPair* keyPair = CBLKeyPair_CreateWithPrivateKeyData(s, kFLSliceNull, &error);
                CBLCert* certificate= CBLCert_CreateWithData(s,&error);
                CBLTLSIdentity* identity = CBLTLSIdentity_IdentityWithKeyPairAndCerts(keyPair, certificate, &error);
                config->pinnedServerCertificate = s;
            }
            if (tls_authenticator) {
                std::string certLocation = file_resolution::resolve_path(CERT_LOCATION, false); // .pem 
                std::ifstream certFile(certLocation, std::ios::binary);
                certFile.exceptions(certFile.failbit | certFile.badbit);
                certFile.seekg(0, ios::end);
                auto fileSize = certFile.tellg();
                certFile.seekg(0, ios::beg);
                FLSlice s {
                    malloc(fileSize),
                    (size_t)fileSize
                };
                certFile.read((char *)s.buf, fileSize);
                certFile.close();
                CBLError error;
                CBLKeyPair* keyPair = CBLKeyPair_CreateWithPrivateKeyData(s, kFLSliceNull, &error);
                CBLCert* certificate= CBLCert_CreateWithData(s,&error);
                CBLTLSIdentity* identity = CBLTLSIdentity_IdentityWithKeyPairAndCerts(keyPair, certificate, &error);
                CBLAuthenticator* auth=CBLAuth_CreateCertificate(identity);
                config->authenticator=auth;

            }
             if (server_verification_mode) {
                config->acceptOnlySelfSignedServerCertificate = true;
            }
            if (body.contains("max_retries") )
            {
                config->maxAttempts=body["max_retries"].get<int>();
            }
            if (body.contains("max_timeout") )
            {
                config->maxAttemptWaitTime=body["max_timeout"].get<int>();
            }
            vector<CBLReplicationCollection> vec;
            // need to handle configurations param
            if(body.contains("collections")) {
                for(const auto& c: body["collections"]) {
                    if (c.is_string()) {
                        auto key = c.get<std::string>();
                        auto raw = memory_map::get(key);
                        if (raw) {
                            CBLReplicationCollection *rep_object = static_cast<CBLReplicationCollection*>(raw);
                            vec.push_back(*rep_object);
                        } else{
                            throw std::runtime_error("Error reading memory pointer: " + key);
                        }
                    }
                    else{
                        throw std::runtime_error("Error in collection array, contents: "+ c.dump());
                    }

                }
                config->collections = vec.data();
                config->collectionCount = vec.size();
            }
            CBLReplicator* repl;
            repl = CBLReplicator_Create(config, &err);
            write_serialized_body(conn, memory_map::store(repl, CBLReplicator_EntryDelete));

        });
    }
}






