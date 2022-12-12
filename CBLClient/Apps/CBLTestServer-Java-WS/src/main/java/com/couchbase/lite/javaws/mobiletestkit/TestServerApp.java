package com.couchbase.lite.javaws.mobiletestkit;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.couchbase.lite.internal.utils.StringUtils;
import com.couchbase.lite.mobiletestkit.Dispatcher;
import com.couchbase.lite.mobiletestkit.Memory;
import com.couchbase.lite.mobiletestkit.Reply;
import com.couchbase.lite.mobiletestkit.TestKitApp;
import com.couchbase.lite.mobiletestkit.util.Log;


@WebServlet(name = "TestServerApp", urlPatterns = {"/"}, loadOnStartup = 1)
public class TestServerApp extends HttpServlet {
    public static final String TAG = "WS-REQUEST";
    public static Memory MEMORY;


    private Dispatcher dispatcher;

    @Override
    public void init() {
        TestKitApp.init(new JavaWSTestKitApp(getServletContext().getServerInfo().replaceAll("\\s+", "_")));
        MEMORY = Memory.create(TestKitApp.getApp().getAppId());
        dispatcher = TestKitApp.getApp().getDispatcher();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Reply reply = dispatchRequest(request.getRequestURI(), getPostData(request.getReader()));
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Type", reply.getContentType());
            response.getOutputStream().write(reply.getData());
            response.getOutputStream().flush();
            response.getOutputStream().close();
        }
        catch (Exception e) {
            Log.w(TAG, "Request failed", e);

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader("Content-Type", "text/plain");

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            response.getWriter().println(sw);
        }
    }

    /**
     * GET method, for testing
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String resp_msg = "CouchbaseLite Java WebService - OK";

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Type", "text/plain");
        response.getOutputStream().write(resp_msg.getBytes());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private Reply dispatchRequest(String req, String body) throws Exception {

        Log.i(TAG, "Request: " + req);

        String[] path = req.split("/");
        int pathLen = path.length;

        String method = (pathLen <= 0) ? null : path[pathLen - 1];
        if (StringUtils.isEmpty(method)) { throw new IllegalArgumentException("Empty request"); }

        // Find and invoke the method on the RequestHandler.
        return dispatcher.run(req, body, MEMORY);
    }

    private String getPostData(Reader in) throws IOException {
        StringWriter out = new StringWriter();
        char[] buffer = new char[1024];
        int read;
        while ((read = in.read(buffer)) != -1) { out.write(buffer, 0, read); }
        return out.toString();
    }
}
