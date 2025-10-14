//
//  DataSourceRequestHandler.swift
//  CBLTestServer-iOS
//
//  Created by Raghu Sarangapani on 1/16/18.
//  Copyright © 2018 Raghu Sarangapani. All rights reserved.
//

import Foundation
import CouchbaseLiteSwift

public class DataSourceRequestHandler {
    public static let VOID: String? = nil
    fileprivate var _pushPullReplListener:NSObjectProtocol?
    
    public func handleRequest(method: String, args: Args) throws -> Any? {
        switch method {
        ////////////////
        // DataSource //
        ////////////////
        case "datasource_database":
            let database: Database = args.get(name: "database")!
            let defaultCol = try database.defaultCollection()
            return DataSource.collection(defaultCol)

        default:
            throw RequestHandlerError.MethodNotFound(method)
        }
    }
}
