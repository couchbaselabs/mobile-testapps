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
            print(test as Any)
            return test
            
        default:
            throw RequestHandlerError.MethodNotFound(method)
        }
    }

}

@available(iOS 16.0, *)
public class vectorModel: PredictiveModel {
    let name: String
    //let model = try! float32_model()
    
    public func predict(input: DictionaryObject) -> DictionaryObject? {
        nil
    }
    
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
