//
// Copyright (c) 2022 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.lite.mobiletestkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.couchbase.lite.mobiletestkit.legacy.ArrayRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.BasicAuthenticatorRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.BlobRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.CollatorRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.CollectionRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.DataSourceRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.DataTypesInitiatorHandler;
import com.couchbase.lite.mobiletestkit.legacy.DatabaseConfigurationRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.DatabaseRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.DictionaryRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.DocumentRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.ExpressionRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.ListenerAuthenticatorRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.LoggingRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.PeerToPeerRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.PredictiveQueriesRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.QueryRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.ReplicatorConfigurationRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.ReplicatorRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.ResultRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.SelectResultRequestHandler;
import com.couchbase.lite.mobiletestkit.legacy.SessionAuthenticatorRequestHandler;
import com.couchbase.lite.mobiletestkit.util.FileUtils;


public final class Dispatcher {
    @FunctionalInterface
    private interface Action {
        @NonNull
        Reply run(@Nullable String body, @NonNull Memory mem) throws Exception;
    }

    private final Map<String, Action> DISPATCH_TABLE = new HashMap<>();

    public void init() {
        DISPATCH_TABLE.put(
            "release",
            (body, mem) -> {
                mem.remove(Args.createLeaf(body).getString("object"));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "flushMemory",
            (body, mem) -> {
                mem.flush();
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "copy_files",
            (body, mem) -> {
                final Args args = Args.createLeaf(body);
                new FileUtils().moveFileOrDir(
                    new File(args.getString("source_path")),
                    new File(args.getString("destination_path")));
                return Reply.create("Copied");
            });

        DISPATCH_TABLE.put(
            "array_addDictionary",
            (body, mem) -> Reply.create(new ArrayRequestHandler().addDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "array_addString",
            (body, mem) -> Reply.create(new ArrayRequestHandler().addString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "array_create",
            (body, mem) -> Reply.create(new ArrayRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "array_getArray",
            (body, mem) -> Reply.create(new ArrayRequestHandler().getArray(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "array_getString",
            (body, mem) -> Reply.create(new ArrayRequestHandler().getString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "array_setString",
            (body, mem) -> Reply.create(new ArrayRequestHandler().setString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "basicAuthenticator_create",
            (body, mem) ->
                Reply.create(new BasicAuthenticatorRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "basicAuthenticator_getPassword",
            (body, mem) ->
                Reply.create(new BasicAuthenticatorRequestHandler().getPassword(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "basicAuthenticator_getUsername",
            (body, mem) ->
                Reply.create(new BasicAuthenticatorRequestHandler().getUsername(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_create",
            (body, mem) -> Reply.create(new BlobRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_createImageContent",
            (body, mem) -> Reply.create(new BlobRequestHandler().createImageContent(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_createImageFileUrl",
            (body, mem) -> Reply.create(new BlobRequestHandler().createImageFileUrl(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_createImageStream",
            (body, mem) -> Reply.create(new BlobRequestHandler().createImageStream(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_createUTFBytesContent",
            (body, mem) ->
                Reply.create(new BlobRequestHandler().createUTFBytesContent(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_digest",
            (body, mem) -> Reply.create(new BlobRequestHandler().digest(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_getContent",
            (body, mem) -> Reply.create(new BlobRequestHandler().getContent(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_getContentStream",
            (body, mem) -> Reply.create(new BlobRequestHandler().getContentStream(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_getContentType",
            (body, mem) -> Reply.create(new BlobRequestHandler().getContentType(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_getProperties",
            (body, mem) -> Reply.create(new BlobRequestHandler().getProperties(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_length",
            (body, mem) -> Reply.create(new BlobRequestHandler().length(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "blob_toString",
            (body, mem) -> Reply.create(new BlobRequestHandler().toString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collator_ascii",
            (body, mem) -> Reply.create(new CollatorRequestHandler().ascii(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collator_unicode",
            (body, mem) -> Reply.create(new CollatorRequestHandler().unicode(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_collection",
            (body, mem) -> Reply.create(new CollectionRequestHandler().collection(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_collectionNames",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().collectionNames(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_collectionScope",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().collectionScope(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_createCollection",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().createCollection(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_createValueIndex",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().createValueIndex(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_defaultCollection",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().defaultCollection(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_deleteCollection",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().deleteCollection(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_deleteDocument",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().deleteDocument(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_deleteIndex",
            (body, mem) -> Reply.create(new CollectionRequestHandler().deleteIndex(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_documentCount",
            (body, mem) -> Reply.create(new CollectionRequestHandler().documentCount(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_getCollectionName",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().getCollectionName(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_getDocument",
            (body, mem) -> Reply.create(new CollectionRequestHandler().getDocument(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_getDocumentExpiration",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().getDocumentExpiration(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_getIndexNames",
            (body, mem) -> Reply.create(new CollectionRequestHandler().getIndexNames(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_getMutableDocument",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().getMutableDocument(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_purgeDocument",
            (body, mem) -> Reply.create(new CollectionRequestHandler().purgeDocument(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_purgeDocumentID",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().purgeDocumentID(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_saveDocument",
            (body, mem) -> Reply.create(new CollectionRequestHandler().saveDocument(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_saveDocuments",
            (body, mem) -> Reply.create(new CollectionRequestHandler().saveDocuments(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "collection_setDocumentExpiration",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().setDocumentExpiration(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_addChangeListener",
            (body, mem) ->
                Reply.create(new DatabaseRequestHandler().addChangeListener(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_changeEncryptionKey",
            (body, mem) -> {
                new DatabaseRequestHandler().changeEncryptionKey(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_close",
            (body, mem) -> {
                new DatabaseRequestHandler().close(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_compact",
            (body, mem) -> {
                new DatabaseRequestHandler().compact(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_copy",
            (body, mem) -> {
                new DatabaseRequestHandler().copy(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_create",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_databaseChangeGetDocumentId",
            (body, mem) -> { throw new UnsupportedOperationException("database_databaseChangeGetDocumentId"); });

        DISPATCH_TABLE.put(
            "database_databaseChangeListenerChangesCount",
            (body, mem) ->
                Reply.create(
                    new DatabaseRequestHandler().databaseChangeListenerChangesCount(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "database_databaseChangeListenerGetChange",
            (body, mem) ->
                Reply.create(
                    new DatabaseRequestHandler().databaseChangeListenerGetChange(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "database_delete",
            (body, mem) -> {
                new DatabaseRequestHandler().delete(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_deleteBulkDocs",
            (body, mem) -> {
                new DatabaseRequestHandler().deleteBulkDocs(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_deleteDB",
            (body, mem) -> {
                new DatabaseRequestHandler().deleteDB(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_deleteDBbyName",
            (body, mem) -> {
                new DatabaseRequestHandler().deleteDbByName(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_deleteWithConcurrency",
            (body, mem) -> {
                new DatabaseRequestHandler().deleteWithConcurrency(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_exists",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().exists(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getCount",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getCount(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getDocIds",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getDocIds(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getDocument",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getDocument(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getDocuments",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getDocuments(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getIndexes",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getIndexes(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getName",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getName(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getPath",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getPath(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_getPreBuiltDb",
            (body, mem) -> Reply.create(new DatabaseRequestHandler().getPreBuiltDb(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "database_path",
            (body, mem) -> { throw new UnsupportedOperationException("database_path"); });

        DISPATCH_TABLE.put(
            "database_purge",
            (body, mem) -> {
                new DatabaseRequestHandler().purge(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_removeChangeListener",
            (body, mem) -> {
                new DatabaseRequestHandler().removeChangeListener(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_save",
            (body, mem) -> {
                new DatabaseRequestHandler().save(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_saveDocuments",
            (body, mem) -> {
                new DatabaseRequestHandler().saveDocuments(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_saveWithConcurrency",
            (body, mem) -> {
                new DatabaseRequestHandler().saveWithConcurrency(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_updateDocument",
            (body, mem) -> {
                new DatabaseRequestHandler().updateDocument(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "database_updateDocuments",
            (body, mem) -> {
                new DatabaseRequestHandler().updateDocuments(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "databaseConfiguration_configure",
            (body, mem) -> {
                new DatabaseConfigurationRequestHandler().configure(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "databaseConfiguration_configure_old",
            (body, mem) -> { throw new UnsupportedOperationException("databaseConfiguration_configure_old"); });

        DISPATCH_TABLE.put(
            "databaseConfiguration_getConflictResolver",
            (body, mem) -> { throw new UnsupportedOperationException("databaseConfiguration_getConflictResolver"); });

        DISPATCH_TABLE.put(
            "databaseConfiguration_getDirectory",
            (body, mem) ->
                Reply.create(new DatabaseConfigurationRequestHandler().getDirectory(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "databaseConfiguration_getEncryptionKey",
            (body, mem) ->
                Reply.create(
                    new DatabaseConfigurationRequestHandler().getEncryptionKey(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "databaseConfiguration_setConflictResolver",
            (body, mem) -> { throw new UnsupportedOperationException("databaseConfiguration_setConflictResolver"); });

        DISPATCH_TABLE.put(
            "databaseConfiguration_setDirectory",
            (body, mem) ->
                Reply.create(new DatabaseConfigurationRequestHandler().setDirectory(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "databaseConfiguration_setEncryptionKey",
            (body, mem) ->
                Reply.create(
                    new DatabaseConfigurationRequestHandler().setEncryptionKey(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "datasource_database",
            (body, mem) -> Reply.create(new DataSourceRequestHandler().database(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_compare",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().compare(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_compareDate",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().compareDate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_compareDouble",
            (body, mem) ->
                Reply.create(new DataTypesInitiatorHandler().compareDouble(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_compareHashMap",
            (body, mem) -> { throw new UnsupportedOperationException("datatype_compareHashMap"); });

        DISPATCH_TABLE.put(
            "datatype_compareLong",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().compareLong(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_get",
            (body, mem) -> { throw new UnsupportedOperationException("datatype_get"); });

        DISPATCH_TABLE.put(
            "datatype_hashMap",
            (body, mem) -> { throw new UnsupportedOperationException("datatype_hashMap"); });

        DISPATCH_TABLE.put(
            "datatype_put",
            (body, mem) -> { throw new UnsupportedOperationException("datatype_put"); });

        DISPATCH_TABLE.put(
            "datatype_setDate",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().setDate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_setDouble",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().setDouble(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_setFloat",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().setFloat(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "datatype_setLong",
            (body, mem) -> Reply.create(new DataTypesInitiatorHandler().setLong(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_contains",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().contains(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_count",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().count(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_create",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getArray",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getArray(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getBlob",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getBlob(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getBoolean",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getBoolean(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getDate",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getDate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getDictionary",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getDouble",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getDouble(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getFloat",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getFloat(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getInt",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getInt(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getKeys",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getKeys(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getLong",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getLong(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getNumber",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getNumber(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_getString",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().getString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_iterator",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().iterator(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_remove",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().remove(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setArray",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setArray(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setBlob",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setBlob(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setBoolean",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setBoolean(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setDate",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setDate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setDictionary",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setDouble",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setDouble(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setEncryptable",
            (body, mem) -> { throw new UnsupportedOperationException("dictionary_setEncryptable"); });

        DISPATCH_TABLE.put(
            "dictionary_setFloat",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setFloat(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setInt",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setInt(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setLong",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setLong(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setNumber",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setNumber(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setString",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_setValue",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().setValue(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_toMap",
            (body, mem) -> Reply.create(new DictionaryRequestHandler().toMap(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "dictionary_toMutableDictionary",
            (body, mem) ->
                Reply.create(new DictionaryRequestHandler().toMutableDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_contains",
            (body, mem) -> Reply.create(new DocumentRequestHandler().contains(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_count",
            (body, mem) -> Reply.create(new DocumentRequestHandler().count(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_create",
            (body, mem) -> Reply.create(new DocumentRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_delete",
            (body, mem) -> { throw new UnsupportedOperationException("document_delete"); });

        DISPATCH_TABLE.put(
            "document_getArray",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getArray(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getBlob",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getBlob(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getBoolean",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getBoolean(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getDate",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getDate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getDictionary",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getDouble",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getDouble(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getFloat",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getFloat(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getId",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getId(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getInt",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getInt(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getKeys",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getKeys(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getLong",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getLong(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getNumber",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getNumber(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getString",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_getValue",
            (body, mem) -> Reply.create(new DocumentRequestHandler().getValue(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_removeKey",
            (body, mem) -> Reply.create(new DocumentRequestHandler().removeKey(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setArray",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setArray(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setBlob",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setBlob(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setBoolean",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setBoolean(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setData",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setData(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setDate",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setDate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setDictionary",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setDouble",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setDouble(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setFloat",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setFloat(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setInt",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setInt(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setLong",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setLong(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setNumber",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setNumber(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setString",
            (body, mem) -> Reply.create(new DocumentRequestHandler().setString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_setValue",
            (body, mem) -> {
                new DocumentRequestHandler().setValue(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "document_toMap",
            (body, mem) -> Reply.create(new DocumentRequestHandler().toMap(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "document_toMutable",
            (body, mem) -> Reply.create(new DocumentRequestHandler().toMutable(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "encryptable_createEncryptor",
            (body, mem) -> { throw new UnsupportedOperationException("encryptable_createEncryptor"); });

        DISPATCH_TABLE.put(
            "encryptable_createValue",
            (body, mem) -> { throw new UnsupportedOperationException("encryptable_createValue"); });

        DISPATCH_TABLE.put(
            "encryptable_getEncryptableValue",
            (body, mem) -> { throw new UnsupportedOperationException("encryptable_getEncryptableValue"); });

        DISPATCH_TABLE.put(
            "encryptable_isEncryptableValue",
            (body, mem) -> { throw new UnsupportedOperationException("encryptable_isEncryptableValue"); });

        DISPATCH_TABLE.put(
            "encryptable_setEncryptableValue",
            (body, mem) -> { throw new UnsupportedOperationException("encryptable_setEncryptableValue"); });

        DISPATCH_TABLE.put(
            "encryptionKey_create",
            (body, mem) -> { throw new UnsupportedOperationException("encryptionKey_create"); });

        DISPATCH_TABLE.put(
            "expression_createAnd",
            (body, mem) -> Reply.create(new ExpressionRequestHandler().createAnd(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "expression_createEqualTo",
            (body, mem) -> Reply.create(new ExpressionRequestHandler().createEqualTo(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "expression_createOr",
            (body, mem) -> Reply.create(new ExpressionRequestHandler().createOr(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "expression_metaId",
            (body, mem) -> Reply.create(new ExpressionRequestHandler().metaId(), mem));

        DISPATCH_TABLE.put(
            "expression_metaSequence",
            (body, mem) -> Reply.create(new ExpressionRequestHandler().metaSequence(), mem));

        DISPATCH_TABLE.put(
            "expression_property",
            (body, mem) -> Reply.create(new ExpressionRequestHandler().property(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "listenerAuthenticator_create",
            (body, mem) ->
                Reply.create(new ListenerAuthenticatorRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "listenerCertificateAuthenticator_create",
            (body, mem) -> { throw new UnsupportedOperationException("listenerCertificateAuthenticator_create"); });

        DISPATCH_TABLE.put(
            "logging_configure",
            (body, mem) -> Reply.create(new LoggingRequestHandler().configure(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "logging_getConfig",
            (body, mem) -> Reply.create(new LoggingRequestHandler().getConfig(), mem));

        DISPATCH_TABLE.put(
            "logging_getDirectory",
            (body, mem) -> Reply.create(new LoggingRequestHandler().getDirectory(), mem));

        DISPATCH_TABLE.put(
            "logging_getLogLevel",
            (body, mem) -> Reply.create(new LoggingRequestHandler().getLogLevel(), mem));

        DISPATCH_TABLE.put(
            "logging_getLogsInZip",
            (body, mem) -> Reply.create(
                "application/zip",
                new LoggingRequestHandler().getLogsInZip()));

        DISPATCH_TABLE.put(
            "logging_getMaxRotateCount",
            (body, mem) ->
                Reply.create(new LoggingRequestHandler().getMaxRotateCount(), mem));

        DISPATCH_TABLE.put(
            "logging_getMaxSize",
            (body, mem) -> Reply.create(new LoggingRequestHandler().getMaxSize(), mem));

        DISPATCH_TABLE.put(
            "logging_getPlainTextStatus",
            (body, mem) ->
                Reply.create(new LoggingRequestHandler().getPlainTextStatus(), mem));

        DISPATCH_TABLE.put(
            "logging_setConfig",
            (body, mem) -> Reply.create(new LoggingRequestHandler().setConfig(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "logging_setLogLevel",
            (body, mem) -> Reply.create(new LoggingRequestHandler().setLogLevel(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "logging_setMaxRotateCount",
            (body, mem) ->
                Reply.create(new LoggingRequestHandler().setMaxRotateCount(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "logging_setMaxSize",
            (body, mem) -> Reply.create(new LoggingRequestHandler().setMaxSize(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "logging_setPlainTextStatus",
            (body, mem) ->
                Reply.create(new LoggingRequestHandler().setPlainTextStatus(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "peerToPeer_acceptClient",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_acceptClient"); });

        DISPATCH_TABLE.put(
            "peerToPeer_addReplicatorEventChangeListener",
            (body, mem) ->
                Reply.create(
                    new PeerToPeerRequestHandler().addReplicatorEventChangeListener(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "peerToPeer_clientStart",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_clientStart"); });

        DISPATCH_TABLE.put(
            "peerToPeer_clientStart_mep",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_clientStart_mep"); });

        DISPATCH_TABLE.put(
            "peerToPeer_configure",
            (body, mem) -> Reply.create(new PeerToPeerRequestHandler().configure(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "peerToPeer_createConnection",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_createConnection"); });

        DISPATCH_TABLE.put(
            "peerToPeer_getListenerPort",
            (body, mem) ->
                Reply.create(new PeerToPeerRequestHandler().getListenerPort(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "peerToPeer_initialize",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_initialize"); });

        DISPATCH_TABLE.put(
            "peerToPeer_messageEndpointListenerStart",
            (body, mem) ->
                Reply.create(
                    new PeerToPeerRequestHandler().messageEndpointListenerStart(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "peerToPeer_readDataFromClient",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_readDataFromClient"); });

        DISPATCH_TABLE.put(
            "peerToPeer_removeReplicatorEventListener",
            (body, mem) -> {
                new PeerToPeerRequestHandler().removeReplicatorEventListener(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "peerToPeer_replicatorEventChangesCount",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_replicatorEventChangesCount"); });

        DISPATCH_TABLE.put(
            "peerToPeer_replicatorEventGetChanges",
            (body, mem) ->
                Reply.create(
                    new PeerToPeerRequestHandler().replicatorEventGetChanges(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "peerToPeer_serverStart",
            (body, mem) -> Reply.create(new PeerToPeerRequestHandler().serverStart(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "peerToPeer_serverStop",
            (body, mem) -> {
                new PeerToPeerRequestHandler().serverStop(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "peerToPeer_socketClientConnection",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_socketClientConnection"); });

        DISPATCH_TABLE.put(
            "peerToPeer_socketConnection",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_socketConnection"); });

        DISPATCH_TABLE.put(
            "peerToPeer_start",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_start"); });

        DISPATCH_TABLE.put(
            "peerToPeer_stop",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_stop"); });

        DISPATCH_TABLE.put(
            "peerToPeer_stopSession",
            (body, mem) -> { throw new UnsupportedOperationException("peerToPeer_stopSession"); });

        DISPATCH_TABLE.put(
            "predictiveQuery_getCosineDistance",
            (body, mem) ->
                Reply.create(new PredictiveQueriesRequestHandler().getCosineDistance(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_getEuclideanDistance",
            (body, mem) ->
                Reply.create(
                    new PredictiveQueriesRequestHandler().getEuclideanDistance(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_getNumberOfCalls",
            (body, mem) ->
                Reply.create(new PredictiveQueriesRequestHandler().getNumberOfCalls(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_getPredictionQueryResult",
            (body, mem) ->
                Reply.create(
                    new PredictiveQueriesRequestHandler().getPredictionQueryResult(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_getSquaredEuclideanDistance",
            (body, mem) ->
                Reply.create(
                    new PredictiveQueriesRequestHandler().getSquaredEuclideanDistance(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_nonDictionary",
            (body, mem) ->
                Reply.create(new PredictiveQueriesRequestHandler().nonDictionary(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_registerModel",
            (body, mem) ->
                Reply.create(new PredictiveQueriesRequestHandler().registerModel(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "predictiveQuery_unRegisterModel",
            (body, mem) -> {
                new PredictiveQueriesRequestHandler().unRegisterModel(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "query_addChangeListener",
            (body, mem) -> Reply.create(new QueryRequestHandler().addChangeListener(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_anyOperator",
            (body, mem) -> Reply.create(new QueryRequestHandler().anyOperator(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_arthimetic",
            (body, mem) -> Reply.create(new QueryRequestHandler().arthimetic(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_between",
            (body, mem) -> Reply.create(new QueryRequestHandler().between(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_collation",
            (body, mem) -> Reply.create(new QueryRequestHandler().collation(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_create",
            (body, mem) -> Reply.create(new QueryRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_crossJoin",
            (body, mem) -> Reply.create(new QueryRequestHandler().crossJoin(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_docsLimitOffset",
            (body, mem) -> Reply.create(new QueryRequestHandler().docsLimitOffset(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_equalTo",
            (body, mem) -> Reply.create(new QueryRequestHandler().equalTo(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_ftsWithRanking",
            (body, mem) -> Reply.create(new QueryRequestHandler().ftsWithRanking(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_getDoc",
            (body, mem) -> Reply.create(new QueryRequestHandler().getDoc(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_getLiveQueryResponseTime",
            (body, mem) ->
                Reply.create(new QueryRequestHandler().getLiveQueryResponseTime(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_greaterThan",
            (body, mem) -> Reply.create(new QueryRequestHandler().greaterThan(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_greaterThanOrEqualTo",
            (body, mem) ->
                Reply.create(new QueryRequestHandler().greaterThanOrEqualTo(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_in",
            (body, mem) -> Reply.create(new QueryRequestHandler().in(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_innerJoin",
            (body, mem) -> Reply.create(new QueryRequestHandler().innerJoin(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_is",
            (body, mem) -> Reply.create(new QueryRequestHandler().is(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_isNot",
            (body, mem) -> Reply.create(new QueryRequestHandler().isNot(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_isNullOrMissing",
            (body, mem) -> Reply.create(new QueryRequestHandler().isNullOrMissing(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_join",
            (body, mem) -> Reply.create(new QueryRequestHandler().join(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_leftJoin",
            (body, mem) -> Reply.create(new QueryRequestHandler().leftJoin(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_leftOuterJoin",
            (body, mem) -> Reply.create(new QueryRequestHandler().leftOuterJoin(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_lessThan",
            (body, mem) -> Reply.create(new QueryRequestHandler().lessThan(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_lessThanOrEqualTo",
            (body, mem) -> Reply.create(new QueryRequestHandler().lessThanOrEqualTo(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_like",
            (body, mem) -> Reply.create(new QueryRequestHandler().like(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_multiplePropertyFTS",
            (body, mem) ->
                Reply.create(new QueryRequestHandler().multiplePropertyFTS(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_multipleSelects",
            (body, mem) -> Reply.create(new QueryRequestHandler().multipleSelects(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_multipleSelectsDoubleValue",
            (body, mem) ->
                Reply.create(new QueryRequestHandler().multipleSelectsDoubleValue(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_multipleSelectsOrderByLocaleValue",
            (body, mem) ->
                Reply.create(
                    new QueryRequestHandler().multipleSelectsOrderByLocaleValue(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "query_nextResult",
            (body, mem) -> Reply.create(new QueryRequestHandler().nextResult(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_not",
            (body, mem) -> Reply.create(new QueryRequestHandler().not(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_notEqualTo",
            (body, mem) -> Reply.create(new QueryRequestHandler().notEqualTo(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_ordering",
            (body, mem) -> Reply.create(new QueryRequestHandler().ordering(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_regex",
            (body, mem) -> Reply.create(new QueryRequestHandler().regex(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_removeChangeListener",
            (body, mem) -> {
                new QueryRequestHandler().removeChangeListener(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "query_run",
            (body, mem) -> Reply.create(new QueryRequestHandler().run(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_selectAll",
            (body, mem) -> Reply.create(new QueryRequestHandler().selectAll(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_singlePropertyFTS",
            (body, mem) -> Reply.create(new QueryRequestHandler().singlePropertyFTS(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_substring",
            (body, mem) -> Reply.create(new QueryRequestHandler().substring(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "query_whereAndOr",
            (body, mem) -> Reply.create(new QueryRequestHandler().whereAndOr(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_addChangeListener",
            (body, mem) ->
                Reply.create(new ReplicatorRequestHandler().addChangeListener(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_addReplicatorEventChangeListener",
            (body, mem) ->
                Reply.create(
                    new ReplicatorRequestHandler().addReplicatorEventChangeListener(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicator_changeListenerChangesCount",
            (body, mem) ->
                Reply.create(
                    new ReplicatorRequestHandler().changeListenerChangesCount(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicator_changeListenerGetChanges",
            (body, mem) ->
                Reply.create(new ReplicatorRequestHandler().changeListenerGetChanges(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_config",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().config(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_configureLocalDb",
            (body, mem) -> { throw new UnsupportedOperationException("replicator_configureLocalDb"); });

        DISPATCH_TABLE.put(
            "replicator_configureRemoteDbUrl",
            (body, mem) -> { throw new UnsupportedOperationException("replicator_configureRemoteDbUrl"); });

        DISPATCH_TABLE.put(
            "replicator_create",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_getActivityLevel",
            (body, mem) ->
                Reply.create(new ReplicatorRequestHandler().getActivityLevel(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_getCompleted",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().getCompleted(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_getError",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().getError(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_getTotal",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().getTotal(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_removeChangeListener",
            (body, mem) -> {
                new ReplicatorRequestHandler().removeChangeListener(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicator_removeReplicatorEventListener",
            (body, mem) -> {
                new ReplicatorRequestHandler().removeReplicatorEventListener(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicator_replicatorEventChangesCount",
            (body, mem) ->
                Reply.create(
                    new ReplicatorRequestHandler().replicatorEventChangesCount(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicator_replicatorEventGetChanges",
            (body, mem) ->
                Reply.create(
                    new ReplicatorRequestHandler().replicatorEventGetChanges(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicator_resetCheckpoint",
            (body, mem) -> {
                new ReplicatorRequestHandler().resetCheckpoint(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicator_start",
            (body, mem) -> {
                new ReplicatorRequestHandler().start(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicator_status",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().status(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicator_stop",
            (body, mem) -> {
                new ReplicatorRequestHandler().stop(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicator_toString",
            (body, mem) -> Reply.create(new ReplicatorRequestHandler().toString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicatorCollection_configure",
            (body, mem) -> { throw new UnsupportedOperationException("replicatorCollection_configure"); });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_addCollection",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().addCollection(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_builderCreate",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().builderCreate(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_configure",
            (body, mem) ->
                Reply.create(new ReplicatorConfigurationRequestHandler().configure(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_copy",
            (body, mem) -> { throw new UnsupportedOperationException("replicatorConfiguration_copy"); });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_create",
            (body, mem) ->
                Reply.create(new ReplicatorConfigurationRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getAuthenticator",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().getAuthenticator(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getChannels",
            (body, mem) ->
                Reply.create(new ReplicatorConfigurationRequestHandler().getChannels(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getConflictResolver",
            (body, mem) -> { throw new UnsupportedOperationException("replicatorConfiguration_getConflictResolver"); });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getDatabase",
            (body, mem) ->
                Reply.create(new ReplicatorConfigurationRequestHandler().getDatabase(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getDocumentIDs",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().getDocumentIDs(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getPinnedServerCertificate",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().getPinnedServerCertificate(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getReplicatorType",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().getReplicatorType(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_getTarget",
            (body, mem) ->
                Reply.create(new ReplicatorConfigurationRequestHandler().getTarget(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_isContinuous",
            (body, mem) ->
                Reply.create(
                    new ReplicatorConfigurationRequestHandler().isContinuous(Args.createTree(body, mem)),
                    mem));

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setAuthenticator",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setAuthenticator(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setAutoPurge",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setAutoPurge(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setChannels",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setChannels(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setConflictResolver",
            (body, mem) -> { throw new UnsupportedOperationException("replicatorConfiguration_setConflictResolver"); });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setContinuous",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setContinuous(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setDocumentIDs",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setDocumentIDs(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setPinnedServerCertificate",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setPinnedServerCertificate(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "replicatorConfiguration_setReplicatorType",
            (body, mem) -> {
                new ReplicatorConfigurationRequestHandler().setReplicatorType(Args.createTree(body, mem));
                return Reply.EMPTY;
            });

        DISPATCH_TABLE.put(
            "result_getString",
            (body, mem) -> Reply.create(new ResultRequestHandler().getString(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "scope_collectionObject",
            (body, mem) ->
                Reply.create(new CollectionRequestHandler().collectionObject(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "scope_defaultScope",
            (body, mem) -> Reply.create(new CollectionRequestHandler().defaultScope(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "scope_scopeName",
            (body, mem) -> Reply.create(new CollectionRequestHandler().scopeName(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "selectResult_all",
            (body, mem) -> Reply.create(new SelectResultRequestHandler().all(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "selectResult_expressionCreate",
            (body, mem) ->
                Reply.create(new SelectResultRequestHandler().expressionCreate(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "sessionAuthenticator_create",
            (body, mem) ->
                Reply.create(new SessionAuthenticatorRequestHandler().create(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "sessionAuthenticator_getCookieName",
            (body, mem) ->
                Reply.create(new SessionAuthenticatorRequestHandler().getCookieName(Args.createTree(body, mem)), mem));

        DISPATCH_TABLE.put(
            "sessionAuthenticator_getExpires",
            (body, mem) -> { throw new UnsupportedOperationException("sessionAuthenticator_getExpires"); });

        DISPATCH_TABLE.put(
            "sessionAuthenticator_getSessionId",
            (body, mem) ->
                Reply.create(new SessionAuthenticatorRequestHandler().getSessionId(Args.createTree(body, mem)), mem));
    }

    public Reply run(@NonNull String method, @Nullable String args, @NonNull Memory mem) throws Exception {
        final Action action = DISPATCH_TABLE.get(method);
        if (action == null) { throw new IllegalArgumentException("No such method: " + method); }
        return action.run(args, mem);
    }
}

