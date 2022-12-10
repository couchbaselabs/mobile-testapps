//
// Copyright (c) 2019 Couchbase, Inc All rights reserved.
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
package com.couchbase.lite.android.mobiletestkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.couchbase.lite.Database;
import com.couchbase.lite.FileLogger;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.mobiletestkit.Args;
import com.couchbase.lite.mobiletestkit.Memory;
import com.couchbase.lite.mobiletestkit.Reply;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.legacy.LoggingRequestHandler;


public class LogTest {

    @Test
    public void testLogZipper() throws Exception {
        LoggingRequestHandler logRequestHandler = new LoggingRequestHandler();

        final Map<String, Object> args = new HashMap<>();
        args.put("directory", "");
        args.put("log_level", "verbose");
        args.put("plain_text", false);
        args.put("max_rotate_count", 1);
        args.put("max_size", 16 * 1024L);
        logRequestHandler.configure(new Args(args));

        final String message = "The quick brown fox jumped over the lazy dog";
        FileLogger logger = Database.log.getFile();
        logger.log(LogLevel.DEBUG, LogDomain.DATABASE, message);
        logger.log(LogLevel.VERBOSE, LogDomain.DATABASE, message);
        logger.log(LogLevel.INFO, LogDomain.DATABASE, message);
        logger.log(LogLevel.WARNING, LogDomain.DATABASE, message);
        logger.log(LogLevel.ERROR, LogDomain.DATABASE, message);

        Assert.assertFalse(new File(TestKitApp.getApp().getFilesDir(), "zip").exists());

        final Reply r = TestKitApp.getApp().getDispatcher().run("logging_getLogsInZip", null, Memory.create("test"));

        Assert.assertNotNull(r);
        Assert.assertEquals("application/zip", r.getContentType());
        // I don't know a good way to test the contents of the zip data.  Pick a random number...
        Assert.assertTrue(r.getData().length > 20);
    }
}

