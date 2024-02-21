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
            
        case "vectorSearch_registerModel":
            let model = vectorModel(name: "test")
            return try await model.tokenizeInput(input: "test")
            
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
            
        default:
            throw RequestHandlerError.MethodNotFound(method)
        }
    }

}

@available(iOS 16.0, *)
public class vectorModel: PredictiveModel {
    let name: String
    let model = try! float32_model()
    
    public func predict(input: DictionaryObject) -> DictionaryObject? {
        nil
    }
    
    func tokenizeInput(input: String) async throws -> [Int] {
        guard let tokenizerConfig = try readConfig(name: "tokenizer_config") else { throw RequestHandlerError.IOException("Could not read config") }
        guard let tokenizerData = try readConfig(name: "tokenizer") else { throw RequestHandlerError.IOException("Could not read config") }
        let tokenizer = try! AutoTokenizer.from(tokenizerConfig: tokenizerConfig, tokenizerData: tokenizerData)
        let tokenized = tokenizer.encode(text: input)
        return padTokenizedInput(tokenIdList: tokenized)
    }
    
    init(name: String) {
        self.name = name
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
