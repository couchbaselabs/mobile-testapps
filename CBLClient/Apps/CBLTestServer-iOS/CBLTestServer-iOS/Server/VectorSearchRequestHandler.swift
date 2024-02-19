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
    public func handleRequest(method: String, args: Args) throws -> Any? {
        switch method {
            
        case "vectorSearch_registerModel":
            let model = vectorModel(name: "model")
            let test = model.readConfig(name: "tokenizer")
            return test
            
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
            let subquantizers: Int? = args.get(name: "subquantizers")
            let bits: Int? = args.get(name: "bits")
            let metric: DistanceMetric? = args.get(name: "metric")
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
                config.metric = metric
            }
            if let minTrainingSize {
                config.minTrainingSize = UInt32(minTrainingSize)
            }
            if let maxTrainingSize {
                config.maxTrainingSize = UInt32(maxTrainingSize)
            }
            
            return try collection.createIndex(withName: indexName, config: config)
            
        default:
            throw RequestHandlerError.MethodNotFound(method)
        }
    }

}

// testing that coreml models can be loaded can only be done on
// a device as we cannot access the directories on sim
// for now work on other API endpoints and return to model later
// could add native coreml embedding model to get vectors and test
// gte-small in future

@available(iOS 16.0, *)
public class vectorModel: PredictiveModel {
    let name: String
    //let model = try! float32_model()
    
    public func predict(input: DictionaryObject) -> DictionaryObject? {
        nil
    }
    
// not sure this is working properly
    func readConfig(name: String) -> Config? {
        if let path = Bundle.main.path(forResource: name, ofType: "json") {
            do {
                let data = try Data(contentsOf: URL(fileURLWithPath: path), options: .mappedIfSafe)
                let jsonResult = try JSONSerialization.jsonObject(with: data, options: .mutableLeaves)
                if let jsonDict = jsonResult as? [String: Any] {
                    return Config(jsonDict)
                }
            } catch {
                print("Error reading config JSON file:", error)
            }
        }
        return nil
    }
    
//    func tokenizeInput(input: String) async -> [Int]? {
//        guard let tokenizerConfig = readConfig(name: "tokenizer_config") else { return nil }
//        guard let tokenizerData = readConfig(name: "tokenizer") else { return nil }
//        let tokenizer = try! AutoTokenizer.from(tokenizerConfig: tokenizerConfig, tokenizerData: tokenizerData)
//        let inputIds = tokenizer(input)
//        return inputIds
//    
//    }
    
    init(name: String) {
        self.name = name
    }
}
