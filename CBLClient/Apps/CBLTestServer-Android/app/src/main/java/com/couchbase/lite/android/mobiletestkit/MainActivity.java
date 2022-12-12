package com.couchbase.lite.android.mobiletestkit;


import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import com.couchbase.lite.mobiletestkit.Server;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.Log;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "UI";

    private Server server;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final String id = TestKitApp.getApp().getAppId();
        server = new Server(id);

        status = findViewById(R.id.status);

        final int port = server.myPort;
        Log.i(TAG, "Server launched at " + id + ":" + port);
        status.setText(getString(R.string.running, id, port));
        try { server.start(); }
        catch (IOException e) {
            Log.e(TAG, "Failed starting server", e);
            status.setText(getString(R.string.fail));
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        status.setText(R.string.stopped);
        if (server != null) { server.stop(); }
    }
}
