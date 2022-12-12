package com.couchbase.lite.javadesktop.mobiletestkit;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;

import com.couchbase.lite.mobiletestkit.Server;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.Log;


public class TestServerApp implements Daemon {
    private static final String TAG = "MAIN";

    private static final AtomicReference<TestServerApp> APP = new AtomicReference<>();
    private static final AtomicReference<Server> SERVER = new AtomicReference<>();

    private static void startApp() {
        TestServerApp app = new TestServerApp();

        if (APP.compareAndSet(null, app)) { throw new IllegalStateException("Attempt to restart app"); }

        app.initApp();
        app.start();
    }

    private static void stopApp() {
        Log.i(TAG, "Stopping TestServer service.");
        Server server = SERVER.getAndSet(null);
        if (server != null) {
            server.stop();
            server.notifyAll();
        }
        APP.set(null);
    }

    /**
     * Main method runs as non-service mode for debugging use
     *
     * @param args cli args
     */
    public static void main(String[] args) {
        startApp();

        System.out.println("Hit Enter to stop >>>");
        try { System.in.read(); }
        catch (IOException err) { Log.w(TAG, "Exception waiting for CLI input", err); }

        stopApp();
    }

    /**
     * Static methods called by prunsrv to start/stop the Windows service.
     * Pass the argument "start" to start the service and any other argument to stop it.
     *
     * @param args Arguments from prunsrv command line
     **/
    public static void windowsService(String[] args) {
        switch (args[0].trim().toLowerCase()) {
            case "":
            case "start":
                startApp();
                Server server;
                while ((server = SERVER.get()) != null) {
                    try { server.wait(); }
                    catch (InterruptedException ignore) { }
                }
                return;

            default:
                stopApp();
        }
    }

    @Override
    public void init(DaemonContext context) { initApp(); }

    @Override
    public void start() {
        final String ip = TestKitApp.getApp().getLocalIpAddress();

        Server server = new Server(ip);
        if (SERVER.compareAndSet(null, server)) { throw new IllegalStateException("Attempt to restart server"); }

        Log.i(TAG, "Server launched at " + ip + ":" + server.myPort);
    }

    @Override
    public void stop() { stopApp(); }

    @Override
    public void destroy() { Log.i(TAG, "TestServer service is destroyed."); }

    private void initApp() {
        String tmpDirPath = System.getProperty("java.io.tmpdir");
        TestKitApp.init(new JavaDesktopTestkitApp(new File(tmpDirPath, "TestServerTemp")));
        Log.i(TAG, "TestKit App initialized.");
    }
}
