//
//  FileLoggingRequestHandler.swift
//  CBLTestServer-iOS
//
//  Created by Hemant on 27/02/19.
//  Copyright © 2019 Hemant Rajput. All rights reserved.
//

import Foundation
import CouchbaseLiteSwift
import Zip

public class FileLoggingRequestHandler {
    public static let VOID = NSObject()
    fileprivate var _pushPullReplListener:NSObjectProtocol?
    
    public func handleRequest(method: String, args: Args) throws -> Any? {
        let max_rotate_count: Int = args.get(name: "max_rotate_count")!
        let max_file_size: UInt64 = UInt64(truncating: args.get(name: "max_size")!)
        
        let log_level: String = args.get(name: "log_level")!
        let level = parseLogLevel(log_level)
        
        let plain_text: Bool = args.get(name: "plain_text")!
        var directory: String = args.get(name: "directory")!
        if (directory.isEmpty) {
            let cacheDir = try FileManager.default.url(for: .cachesDirectory,
                                                       in: .userDomainMask,
                                                       appropriateFor: nil,
                                                       create: false)
            directory = cacheDir.appendingPathComponent("logs_\(Date().timeIntervalSince1970)").path
            print("File logging configured at : " + directory)
        }
        switch method {
            /////////////////
            // CBL Logging //
            /////////////////
        case "logging_configure":
            LogSinks.file = FileLogSink(
                        level: level,
                        directory: directory,
                        usePlainText: plain_text,
                        maxKeptFiles: max_rotate_count + 1,
                        maxFileSize: max_file_size
                    )
            return serializeConfig(LogSinks.file!)

        case "logging_getPlainTextStatus":
            guard let fileSink = LogSinks.file else { return nil }
            return fileSink.usePlaintext

        case "logging_getMaxRotateCount":
            guard let fileSink = LogSinks.file else { return nil }
            return fileSink.maxKeptFiles - 1
            
        case "logging_getMaxSize":
            guard let fileSink = LogSinks.file else { return nil }
            return fileSink.maxFileSize

        case "logging_getDirectory":
            guard let fileSink = LogSinks.file else { return nil }
            return fileSink.directory

        case "logging_getLogLevel":
            guard let fileSink = LogSinks.file else { return nil }
            return fileSink.level

        case "logging_getConfig":
            guard let fileSink = LogSinks.file else { return nil }
            return serializeConfig(fileSink)
            
        case "logging_getLogsInZip":
            guard let fileSink = LogSinks.file else {
                print("File Logging was not enabled")
                return nil
            }
            let path = fileSink.directory
            do {
                let dirURL = URL(fileURLWithPath: path)
                let zipFilePath = try Zip.quickZipFiles([dirURL],
                                                        fileName: "Archive")
                return RawData(data: try Data(contentsOf: zipFilePath),
                               contentType: "application/zip")
            } catch {
                print("Exception Getting LogsInZip \(error)")
            }
            return nil

        case "logging_setPlainTextStatus":
            if let sink = LogSinks.file {
                LogSinks.file = FileLogSink(
                    level: sink.level,
                    directory: sink.directory,
                    usePlainText: plain_text,
                    maxKeptFiles: sink.maxKeptFiles,
                    maxFileSize: sink.maxFileSize
                )
                return serializeConfig(LogSinks.file!)
            }
            return nil

        case "logging_setMaxRotateCount":
            if let sink = LogSinks.file {
                LogSinks.file = FileLogSink(
                    level: sink.level,
                    directory: sink.directory,
                    usePlainText: sink.usePlaintext,
                    maxKeptFiles: max_rotate_count + 1,
                    maxFileSize: sink.maxFileSize
                )
                return serializeConfig(LogSinks.file!)
            }
            return nil

        case "logging_setMaxSize":
            if let sink = LogSinks.file {
                LogSinks.file = FileLogSink(
                    level: sink.level,
                    directory: sink.directory,
                    usePlainText: sink.usePlaintext,
                    maxKeptFiles: sink.maxKeptFiles,
                    maxFileSize: max_file_size
                )
                return serializeConfig(LogSinks.file!)
            }
            return nil

        case "logging_setConfig":
            if let sink = LogSinks.file {
                LogSinks.file = FileLogSink(
                    level: sink.level,
                    directory: directory,
                    usePlainText: sink.usePlaintext,
                    maxKeptFiles: sink.maxKeptFiles,
                    maxFileSize: sink.maxFileSize
                )
                return serializeConfig(LogSinks.file!)
            }
            return nil

        case "logging_setLogLevel":
            let level = parseLogLevel(log_level)
            if let sink = LogSinks.file {
                LogSinks.file = FileLogSink(
                    level: level,
                    directory: sink.directory,
                    usePlainText: sink.usePlaintext,
                    maxKeptFiles: sink.maxKeptFiles,
                    maxFileSize: sink.maxFileSize
                )
                return serializeConfig(LogSinks.file!)
            }
            return nil

        default:
            throw RequestHandlerError.MethodNotFound(method)
        }
    }
    
    func parseLogLevel(_ s: String) -> LogLevel {
        switch s.lowercased() {
        case "debug":   return .debug
        case "verbose": return .verbose
        case "error":   return .error
        case "info":    return .info
        case "warning": return .warning
        default:        return .none
        }
    }
    
    func serializeConfig(_ sink: FileLogSink) -> [String: Any] {
        return [
            "usePlaintext": sink.usePlaintext,
            "maxFileSize": sink.maxFileSize,
            "maxKeptFiles": sink.maxKeptFiles,
            "level": sink.level.rawValue,
            "directory": sink.directory
        ]
    }
}
