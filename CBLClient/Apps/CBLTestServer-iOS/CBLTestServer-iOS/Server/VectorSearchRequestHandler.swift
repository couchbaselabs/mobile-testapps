//
//  VectorSearchRequestHandler.swift
//  CBLTestServer-iOS
//
//  Created by Monty Ali on 13/02/2024.
//  Copyright © 2024 Raghu Sarangapani. All rights reserved.
//

import Foundation
import CouchbaseLiteSwift
import CoreML
import Tokenizers
import Hub

public class VectorSearchRequestHandler {
    public func handleRequest(method: String, args: Args) async throws -> Any? {
        switch method {
            
        // get tokenized input
        case "vectorSearch_testTokenizer":
            guard let input: String = args.get(name: "input") else { throw RequestHandlerError.InvalidArgument("Invalid input")}
            return try tokenizeInput(input: input)
        
        // tokenize input then decode back to string, including padding
        case "vectorSearch_testDecode":
            guard let input: String = args.get(name: "input") else { throw RequestHandlerError.InvalidArgument("Invalid input")}
            let tokens = try tokenizeInput(input: input)
            let decoded = try decodeTokenIds(encoded: tokens)
            return ["tokens": tokens, "decoded": decoded]
            
        // create index on collection
        case "vectorSearch_createIndex":
            guard let database: Database = args.get(name: "database") else { throw RequestHandlerError.InvalidArgument("Invalid database argument")}
            
            let scopeName: String = (args.get(name: "scopeName")) ?? "_default"
            let collectionName: String = (args.get(name: "collectionName")) ?? "_default"
            
            guard let collection: Collection = try database.collection(name: collectionName, scope: scopeName) else { throw RequestHandlerError.IOException("Could not open specified collection")}
            
            guard let indexName: String = args.get(name: "indexName") else { throw RequestHandlerError.InvalidArgument("Invalid index name")}
            
            guard let expression: String = args.get(name: "expression") else { throw RequestHandlerError.InvalidArgument("Invalid expression for vector index")}
            
            // IndexConfiguration type is UInt32 but Serializer does not handle UInt currently so use Int for now and assume always pass +ve numbers
            // For manual testing, note that serialization of ints uses format Ixx where xx is an int value
            guard let dimensions: Int = args.get(name: "dimensions") else { throw RequestHandlerError.InvalidArgument("Invalid dimensions argument for vector index")}
            
            // As above for dimensions, UInt32 vs Int
            guard let centroids: Int = args.get(name: "centroids") else { throw RequestHandlerError.InvalidArgument("Invalid centroids argument for vector index")}
            
            let scalarEncoding: ScalarQuantizerType? = args.get(name: "scalarEncoding")
            // UInt32/Int problem
            // Note that subquantizers needs to be a factor of dimensions, runtime error if not
            // and bits b needs 4 <= b <= 12, maybe worth adding input validation in future
            let subquantizers: Int? = args.get(name: "subquantizers")
            let bits: Int? = args.get(name: "bits")
            let metric: String? = args.get(name: "metric")
            // UInt32/Int problem
            let minTrainingSize: Int? = args.get(name: "minTrainingSize")
            let maxTrainingSize: Int? = args.get(name: "maxTrainingSize")
            
            if scalarEncoding != nil && (bits != nil || subquantizers != nil) {
                throw RequestHandlerError.InvalidArgument("Cannot have scalar quantization and arguments for product quantization at the same time")
            }
            
            if (bits != nil && subquantizers == nil) || (bits == nil && subquantizers != nil) {
                throw RequestHandlerError.InvalidArgument("Product quantization requires both bits and subquantizers set")
            }
            
            var config = VectorIndexConfiguration(expression: expression, dimensions: UInt32(dimensions), centroids: UInt32(centroids))
            if let scalarEncoding {
                config.encoding = VectorEncoding.scalarQuantizer(type: scalarEncoding)
            }
            if let bits {
                config.encoding = VectorEncoding.productQuantizer(subquantizers: UInt32(subquantizers!), bits: UInt32(bits))
            }
            if let metric {
                switch metric {
                case "euclidean":
                    config.metric = DistanceMetric.euclidean
                case "cosine":
                    config.metric = DistanceMetric.cosine
                default:
                    throw RequestHandlerError.InvalidArgument("Invalid distance metric")
                }
            }
            if let minTrainingSize {
                config.minTrainingSize = UInt32(minTrainingSize)
            }
            if let maxTrainingSize {
                config.maxTrainingSize = UInt32(maxTrainingSize)
            }
            // may change this to try catch, and return name of created index in future
            return try collection.createIndex(withName: indexName, config: config)
          
        // returns the embedding for input
        case "vectorSearch_testPredict":
            let model = vectorModel(key: "test")
            let testDic = MutableDictionaryObject()
            guard let input: String = args.get(name: "input") else { throw RequestHandlerError.InvalidArgument("Invalid input for prediction")}
            testDic.setValue(input, forKey: "test")
            let prediction = model.predict(input: testDic)
            let value = prediction?.array(forKey: "vector")
            if let value {
                return value.toArray()
            } else {
                return "not working"
            }
        
        // register model that creates embeddings on the field referred to by key
        case "vectorSearch_registerModel":
            guard let key: String = args.get(name: "key") else { throw RequestHandlerError.InvalidArgument("Invalid key")}
            guard let name: String = args.get(name: "name") else { throw RequestHandlerError.InvalidArgument("Invalid name")}
            let model = vectorModel(key: key)
            Database.prediction.registerModel(model, withName: name)
            return "Registered model with name \(name)"
            
        // this is a very bare bones sql++ query handler which expects the
        // user to pass the query as a string in the request body
        // and simply tries to create a query from that string
        // will fail for invalid query input
        // assumes that you already have created an index
        case "vectorSearch_query":
            // term is the search query that will be embedded and
            // queried against
            guard let term: String = args.get(name: "term") else { throw RequestHandlerError.InvalidArgument("Invalid search term")}
            
            // internal call to handler to get vector embedding for search term
            let embeddingArgs = Args()
            embeddingArgs.set(value: term, forName: "input")
            let embeddedTerm = try await self.handleRequest(method: "vectorSearch_testPredict", args: embeddingArgs)
            
            // the sql query, paramaterised by $vector which will be
            // the embedded term
            guard let sql: String = args.get(name: "sql") else { throw RequestHandlerError.InvalidArgument("Invalid sql string")}
            
            // database to execute query against
            // may change to handle collections
            guard let db: Database = args.get(name: "database") else { throw RequestHandlerError.InvalidArgument("Invalid database")}
            
            let params = Parameters()
            params.setValue(embeddedTerm, forName: "vector")
            let query = try db.createQuery(sql)
            query.parameters = params
            guard let queryResults = try? query.execute() else { throw RequestHandlerError.VectorPredictionError("Error executing SQL++ query")}
            
            var results: [[String:Any]] = []
            for result in queryResults {
                results.append(result.toDictionary())
            }
            
            return results
            
        // load pre generated words dataset with vector embeddings stored in doc body
        case "vectorSearch_loadWords":
            let dbHandler = DatabaseRequestHandler()
            let preBuiltArgs = Args()
            preBuiltArgs.set(value: "Databases/words.cblite2", forName: "dbPath")
            let wordsPath = try dbHandler.handleRequest(method: "database_getPreBuiltDb", args: preBuiltArgs)
            let copyArgs = Args()
            copyArgs.set(value: wordsPath!, forName: "dbPath")
            copyArgs.set(value: "words", forName: "dbName")
            _ = try dbHandler.handleRequest(method: "database_copy", args: copyArgs)
            let words: Database = try Database(name: "words")
            return words
        
        // for manual test, regenerate the embeddings in the word database to be from gte-small
        //manual testing note: database words is closed at the end of this, so need to reopen for subsequent
        //operations
        case "vectorSearch_regenerateWordsEmbeddings":
            let wordsDb = try Database(name: "words")
            let wordsCollection = try wordsDb.collection(name: "words")
            let collectionHandler = CollectionRequestHandler()
            
            let model = vectorModel(key: "word")
            
            let innerArgs = Args()
            let numDocs: Int = Int(wordsCollection!.count)
            innerArgs.set(value: wordsCollection!, forName: "collection")
            innerArgs.set(value: numDocs, forName: "limit")
            innerArgs.set(value: 0, forName: "offset")
            let docIds: [String] = try collectionHandler.handleRequest(method: "collection_getDocIds", args: innerArgs) as! [String]
            
            for id in docIds {
                innerArgs.set(value: id, forName: "docId")
                let doc = try collectionHandler.handleRequest(method: "collection_getDocument", args: innerArgs) as! Document
                
                var docData = doc.toDictionary()
                let mutableDict = MutableDictionaryObject(data: docData)
                let prediction = model.predict(input: mutableDict)
                let embedding = prediction?.array(forKey: "vector")
                if let embedding {
                    docData.updateValue(embedding.toArray(), forKey: "vector")
                } else {
                    continue
                }
                
                innerArgs.set(value: docData, forName: "data")
                innerArgs.set(value: id, forName: "id")
                _ = try collectionHandler.handleRequest(method: "collection_updateDocument", args: innerArgs)
            }
            
            return "Updated document embeddings"
            
        // for manual generation of the functional test database
        case "vectorSearch_generateTestDatabase":
            let innerVectorSearchHandler = VectorSearchRequestHandler()
            let innerArgs = Args()
            var wordsDatabase = try await innerVectorSearchHandler.handleRequest(method: "vectorSearch_loadWords", args: innerArgs)
            _ = try await innerVectorSearchHandler.handleRequest(method: "vectorSearch_regenerateWordsEmbeddings", args: innerArgs)
            
            // reopen words db
            let databaseHandler = DatabaseRequestHandler()
            innerArgs.set(value: "words", forName: "name")
            wordsDatabase = try databaseHandler.handleRequest(method: "database_create", args: innerArgs)
            
            // remove collections
            let collectionHandler = CollectionRequestHandler()
            innerArgs.set(value: wordsDatabase!, forName: "database")
            innerArgs.set(value: "extwords", forName: "collectionName")
            _ = try collectionHandler.handleRequest(method: "collection_deleteCollection", args: innerArgs)
            innerArgs.set(value: "categories", forName: "collectionName")
            _ = try collectionHandler.handleRequest(method: "collection_deleteCollection", args: innerArgs)
            
            // add collections
            innerArgs.set(value: "docBodyVectors", forName: "collectionName")
            let docBodyVectors = try collectionHandler.handleRequest(method: "collection_createCollection", args: innerArgs) as! Collection
            innerArgs.set(value: "indexVectors", forName: "collectionName")
            let indexVectors = try collectionHandler.handleRequest(method: "collection_createCollection", args: innerArgs) as! Collection
            innerArgs.set(value: "auxiliaryWords", forName: "collectionName")
            let auxiliaryWords = try collectionHandler.handleRequest(method: "collection_createCollection", args: innerArgs) as! Collection
            innerArgs.set(value: "searchTerms", forName: "collectionName")
            let searchTerms = try collectionHandler.handleRequest(method: "collection_createCollection", args: innerArgs) as! Collection
            
            // get doc ids for words
            innerArgs.set(value: "words", forName: "collectionName")
            let wordsCollection = try collectionHandler.handleRequest(method: "collection_collection", args: innerArgs) as! Collection
            innerArgs.set(value: wordsCollection, forName: "collection")
            // arbitrary high number to ensure all docs retrieved
            innerArgs.set(value: 10000, forName: "limit")
            innerArgs.set(value: 0, forName: "offset")
            let docIds = try collectionHandler.handleRequest(method: "collection_getDocIds", args: innerArgs) as! [String]
            
            // copy docs
            try self.copyWordDocument(copyFrom: wordsCollection, copyTo: docBodyVectors, docIds: docIds)
            try self.copyWordDocument(copyFrom: wordsCollection, copyTo: indexVectors, docIds: docIds)
            
            // remove word from first 5 cat3 docs in indexVectors
            // 101 is the beginning of cat3
            let cat3Offset = 100
            for i in 1...5 {
                let docId = "word\(cat3Offset+i)"
                let doc = try indexVectors.document(id: docId)?.toMutable()
                doc?.removeValue(forKey: "word")
                try indexVectors.save(document: doc!)
            }
            
            // remove embedding from first 10 docs in cat1 and 2 of docBodyVectors
            let cat2Offset = 50
            for i in 1...10 {
                let docIdCat1 = "word\(i)"
                let docIdCat2 = "word\(cat2Offset+i)"
                
                // copy the documents to aux words for android app
                try self.copyWordDocument(copyFrom: docBodyVectors, copyTo: auxiliaryWords, docIds: [docIdCat1, docIdCat2])
                
                let doc1 = try docBodyVectors.document(id: docIdCat1)?.toMutable()
                let doc2 = try docBodyVectors.document(id: docIdCat2)?.toMutable()
                
                doc1?.removeValue(forKey: "vector")
                doc2?.removeValue(forKey: "vector")
                try docBodyVectors.save(document: doc1!)
                try docBodyVectors.save(document: doc2!)
            }
            
            // add 10 new words with cat3 to auxiliaryWords
            let newWordsOffset = 300 // 300 words initially in words._default.words
            let newWords = ["pan", "pot", "fry", "burn", "stir", "spoon", "chop", "knife", "slice", "simmer"]
            for i in 0...9 {
                let docId = "word\(newWordsOffset+1+i)"
                let doc = MutableDocument(id: docId)
                doc.setString(newWords[i], forKey: "word")
                doc.setString("cat3", forKey: "catid")
                
                try auxiliaryWords.save(document: doc)
            }
            
            // add 15 words with cat1 to aux, 5 with, 5 without and 5 with wrong embeddings
            let correct = ["unhealthy", "toast", "kosher", "halal", "coffee"]
            let without = ["junk", "oily", "porridge", "waste", "dairy"]
            let wrong = ["carbohydrate", "breakfast", "eggs", "treat", "sugary"]
            
            let auxOffset = 311
            for i in 0...4 {
                let innerOffset = auxOffset + i
                let correctId = "word\(innerOffset)"
                let withoutId = "word\(innerOffset+5)"
                let wrongId = "word\(innerOffset+10)"
                
                let correctDoc = MutableDocument(id: correctId)
                let withoutDoc = MutableDocument(id: withoutId)
                let wrongDoc = MutableDocument(id: wrongId)
                
                let correctWord = correct[i]
                let withoutWord = without[i]
                let wrongWord = wrong[i]
                
                innerArgs.set(value: correctWord, forName: "input")
                let correctEmbedding: [Double] = try await innerVectorSearchHandler.handleRequest(method: "vectorSearch_testPredict", args: innerArgs) as! [Double]
                
                innerArgs.set(value: wrongWord, forName: "input")
                var wrongEmbedding: [Double] = try await innerVectorSearchHandler.handleRequest(method: "vectorSearch_testPredict", args: innerArgs) as! [Double]
                wrongEmbedding.removeLast(10)
                
                correctDoc.setString(correctWord, forKey: "word")
                correctDoc.setString("cat1", forKey: "catid")
                correctDoc.setArray(MutableArrayObject(data: correctEmbedding), forKey: "vector")
                
                withoutDoc.setString(withoutWord, forKey: "word")
                withoutDoc.setString("cat1", forKey: "catid")
                
                wrongDoc.setString(wrongWord, forKey: "word")
                wrongDoc.setString("cat1", forKey: "catid")
                correctDoc.setArray(MutableArrayObject(data: wrongEmbedding), forKey: "vector")
                
                try auxiliaryWords.save(document: correctDoc)
                try auxiliaryWords.save(document: withoutDoc)
                try auxiliaryWords.save(document: wrongDoc)
            }
            
            // add dinner search term to searchTerms
            let dinner = "dinner"
            innerArgs.set(value: dinner, forName: "input")
            let dinnerVector: [Double] = try await innerVectorSearchHandler.handleRequest(method: "vectorSearch_testPredict", args: innerArgs) as! [Double]
            let dinnerDoc = MutableDocument(id: "dinner")
            dinnerDoc.setArray(MutableArrayObject(data: dinnerVector), forKey: "vector")
            dinnerDoc.setString(dinner, forKey: "word")
            try searchTerms.save(document: dinnerDoc)
            
            // remove original words collection
            innerArgs.set(value: "words", forName: "collectionName")
            _ = try collectionHandler.handleRequest(method: "collection_deleteCollection", args: innerArgs)
            
            return "Generated test database"
            
            
        default:
            throw RequestHandlerError.MethodNotFound(method)
        }
    }

}

@available(iOS 16.0, *)
public class vectorModel: PredictiveModel {
    // key is the field name in the doc
    // to create embeddings on
    let key: String
    let model = try! float32_model()

    public func predict(input: DictionaryObject) -> DictionaryObject? {
        guard let text = input.string(forKey: self.key) else { return nil }
        let result = MutableDictionaryObject()
        do {
            let tokens = try tokenizeInput(input: text)
            let modelTokens = convertModelInputs(feature: tokens)
            let attentionMask = generateAttentionMask(tokenIdList: tokens)
            let modelMask = convertModelInputs(feature: attentionMask)
            let modelInput = float32_modelInput(input_ids: modelTokens, attention_mask: modelMask)
            let embedding = try model.prediction(input: modelInput)
            let vectorArray = castToDoubleArray(embedding.pooler_output)
            result.setValue(vectorArray, forKey: "vector")
            return result
        } catch {
            return nil
        }
    }
    
    init(key: String) {
        self.key = key
    }
}

// Reads a config from Files/ directory
func readConfig(name: String) throws -> Config? {
    if let url = Bundle.main.url(forResource: "Files/\(name)", withExtension: "json") {
        do {
            let data = try Data(contentsOf: url, options: .mappedIfSafe)
            let jsonResult = try JSONSerialization.jsonObject(with: data, options: .mutableLeaves)
            if let jsonDict = jsonResult as? [String: Any] {
                return Config(jsonDict)
            }
        } catch {
            throw RequestHandlerError.IOException("Error retrieving json config: \(error)")
        }
    }
    return nil
}

// might need validation on input size, gte-small model only
// takes up to 128 tokens, so need to truncate otherwise
func tokenizeInput(input: String) throws -> [Int] {
    guard let tokenizerConfig = try readConfig(name: "tokenizer_config") else { throw RequestHandlerError.IOException("Could not read config") }
    guard let tokenizerData = try readConfig(name: "tokenizer") else { throw RequestHandlerError.IOException("Could not read config") }
    let tokenizer = try! AutoTokenizer.from(tokenizerConfig: tokenizerConfig, tokenizerData: tokenizerData)
    let tokenized = tokenizer.encode(text: input)
    return padTokenizedInput(tokenIdList: tokenized)
}

// refactor tokenizer stuff
func decodeTokenIds(encoded: [Int]) throws -> String {
    guard let tokenizerConfig = try readConfig(name: "tokenizer_config") else { throw RequestHandlerError.IOException("Could not read config") }
    guard let tokenizerData = try readConfig(name: "tokenizer") else { throw RequestHandlerError.IOException("Could not read config") }
    let tokenizer = try! AutoTokenizer.from(tokenizerConfig: tokenizerConfig, tokenizerData: tokenizerData)
    let decoded = tokenizer.decode(tokens: encoded)
    return decoded
}

func padTokenizedInput(tokenIdList: [Int]) -> [Int] {
    // gte-small constants
    let modelInputLength = 128
    let padTokenId = 0
    let inputNumTokens = tokenIdList.count
    var paddedTokenList = [Int]()
    
    paddedTokenList += tokenIdList
    if inputNumTokens < modelInputLength {
        var numTokensToAdd = modelInputLength - inputNumTokens
        while numTokensToAdd > 0 {
            paddedTokenList.append(padTokenId)
            numTokensToAdd -= 1
        }
    }
    return paddedTokenList
}

func generateAttentionMask(tokenIdList: [Int]) -> [Int] {
    var mask = [Int]()
    for i in tokenIdList {
        if i == 0 {
            mask.append(0)
        } else {
            mask.append(1)
        }
    }
    return mask
}

// convert input tokens and attention mask to the correct shape
func convertModelInputs(feature: [Int]) -> MLMultiArray {
    let attentionMaskMultiArray = try? MLMultiArray(shape: [1, NSNumber(value: feature.count)], dataType: .int32)
    for i in 0..<feature.count {
        attentionMaskMultiArray?[i] = NSNumber(value: feature[i])
    }
    return attentionMaskMultiArray!
}

// https://stackoverflow.com/q/62887013
func castToDoubleArray(_ o: MLMultiArray) -> [Double] {
    var result: [Double] = Array(repeating: 0.0, count: o.count)
    for i in 0 ..< o.count {
        result[i] = o[i].doubleValue
    }
    return result
}

private extension VectorSearchRequestHandler {
    // internal function for copying documents to the test database
    func copyWordDocument(copyFrom: Collection, copyTo: Collection, docIds: [String]) throws {
        let copyToCollectionName = copyTo.name
        for docId in docIds {
            guard let doc = try copyFrom.document(id: docId) else { throw RequestHandlerError.IOException("Could not get document \(docId)")}
            var docData = doc.toDictionary()
            
            if copyToCollectionName == "indexVectors" {
                docData.removeValue(forKey: "vector")
            }
            
            let newDoc = MutableDocument(id: docId, data: docData)
            try copyTo.save(document: newDoc)
        }
    }
}
