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
package com.couchbase.lite.android.mobiletestkit;

import android.content.Context;
import android.util.Base64;

import java.io.File;

import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.mobiletestkit.TestKitApp;


public class AndroidTestKitApp extends TestKitApp {
    private final Context context;

    public AndroidTestKitApp(Context context) { this.context = context.getApplicationContext(); }

    @Override
    protected void initCBL() { CouchbaseLite.init(context, true); }

    @Override
    public String getPlatform() { return "android"; }

    @Override
    public File getFilesDir() { return context.getFilesDir(); }

    @Override
    public String encodeBase64(byte[] hashBytes) { return Base64.encodeToString(hashBytes, Base64.NO_WRAP); }

    @Override
    public byte[] decodeBase64(String encodedBytes) { return Base64.decode(encodedBytes, Base64.NO_WRAP); }
}
