package com.couchbase.lite.mobiletestkit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import com.couchbase.lite.internal.utils.StringUtils;
import com.couchbase.lite.mobiletestkit.util.Log;


public class Server extends NanoHTTPD {
    private static final String TAG = "SERVER";

    private static final int PORT = 8080;
    private static final String KEY_POST_DATA = "postData";

    private final Memory memory;
    private final Dispatcher dispatcher;

    public Server(@NonNull String address) {
        super(PORT);
        this.memory = Memory.create(address);
        this.dispatcher = TestKitApp.getApp().getDispatcher();
    }

    @Override
    public Response handle(IHTTPSession session) {
        try {
            Reply reply = dispatchRequest(session.getUri(), getPostData(session));
            return Response.newFixedLengthResponse(Status.OK, reply.getContentType(), reply.getData());
        }
        catch (Exception e) {
            Log.w(TAG, "Request failed", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return Response.newFixedLengthResponse(Status.BAD_REQUEST, "text/plain", sw.toString());
        }
    }

    private Reply dispatchRequest(@Nullable String req, @Nullable String body) throws Exception {
        Log.i(TAG, "Request: " + req);

        if (StringUtils.isEmpty(req)) { throw new IllegalArgumentException("Empty request"); }

        if (!req.startsWith("/")) { req = req.substring(1); }

        // Find and invoke the method on the RequestHandler.
        return dispatcher.run(req, body, memory);
    }

    @Nullable
    private String getPostData(@NonNull IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> data = new HashMap<>();
        session.parseBody(data);
        return data.get(KEY_POST_DATA);
    }
}



