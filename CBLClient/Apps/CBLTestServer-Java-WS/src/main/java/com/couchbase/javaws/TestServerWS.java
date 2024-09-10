package com.couchbase.javaws;

import com.couchbase.mobiletestkit.javacommon.*;
import com.couchbase.mobiletestkit.javacommon.util.Log;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "TestServerWS", urlPatterns = {"/"}, loadOnStartup = 1)
public class TestServerWS extends HttpServlet {
    public static final String TAG = "WS-REQUEST";
    public static final Memory memory = new Memory();

    @Override
    public void init() throws ServletException {
        CouchbaseLite.init();
         try {
            CouchbaseLite.enableVectorSearch();
        }
        catch (CouchbaseLiteException e) {
            Log.e(TAG, "Warning: vector search was not loaded, the vector serach tests are expected to fail");
        }
        Database.log.getConsole().setLevel(LogLevel.DEBUG);
        Database.log.getConsole().setDomains(LogDomain.ALL_DOMAINS);

        Log.init(new TestServerLogger());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Context context = new TestServerContext(request);
        memory.setIpAddress(context.getLocalIpAddress());
        RequestHandlerDispatcher.setDispatcherProperties(context, memory);

        /*
        request URI comes as full path after the domain name
        such as /TestServer-Java-WS/database_create
        get the path after the last slash
        */
        String path = request.getRequestURI();
        Log.i(TAG, path);
        String[] pathLevels = path.split("/");
        if(pathLevels.length == 0){
            Log.e(TAG, "The request URI has incorrect format.");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        /*
        according to the design of mobile test framework across to all platforms
        the path has pattern of handlerType_methodName
        split the underscore to extract the request handler name and its method
        */
        String handlerType = "";
        String method_to_call = "";

        if(pathLevels[pathLevels.length - 1].equalsIgnoreCase("copy_files")){
            method_to_call = pathLevels[pathLevels.length - 1];
        } else {
            String[] methodArgs = pathLevels[pathLevels.length - 1].split("_");
            handlerType = methodArgs.length > 1 ? methodArgs[0] : "nohandler";
            method_to_call = methodArgs.length > 1 ? methodArgs[1] : methodArgs[0];
        }

        /*
        process request body
        the body sent by mobile test framework is gauranteed in json format
        the body not only contains postData, but also contains other settings, such as flushMemory etc.
        */
        Args args = new Args();
        BufferedReader reader = request.getReader();
        Gson gson = new GsonBuilder().serializeNulls().create();

        Map<String, Object> query = new Gson().fromJson(reader, Map.class);
        if (query != null) {
            for (String key : query.keySet()) {
                String value = (String) query.get(key);
                /*
                while receiving a release method in request
                no deserialization is needed,
                the original object memory address is required
                */
                if("release".equals(method_to_call)){
                    args.put(key, value);
                }
                else {
                    args.put(key, ValueSerializer.deserialize(value, RequestHandlerDispatcher.memory));
                }
            }
        }
        if(!args.contain("config") && !args.contain("directory")){
            args.put("directory", context.getFilesDir().getAbsolutePath());
        }

        try{
            Object body = RequestHandlerDispatcher.handle(handlerType, method_to_call, args);

            if (body == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Content-Type", "text/plain");
                response.getWriter().write("I-1");
                response.getWriter().flush();
                response.getWriter().close();

                return;
            }

            if (body instanceof String) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/plain; charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
                response.getOutputStream().flush();
                response.getOutputStream().close();

                return;
            }
            else if (body instanceof RawData) {
                RawData dataObj = (RawData) body;

                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Content-Type", dataObj.contentType);
                response.setCharacterEncoding("UTF-8");
                response.getOutputStream().write(dataObj.data);
                response.getOutputStream().flush();
                response.getOutputStream().close();

                return;
            }
            else {
                throw new IllegalArgumentException("unrecognized body type: " + body.getClass());
            }
        }catch (Exception e){
            Log.w(TAG, "Request failed", ((InvocationTargetException) e).getTargetException());

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader("Content-Type", "text/plain");
            if(((InvocationTargetException) e).getCause() != null){
                response.getWriter().println(((InvocationTargetException) e).getCause());
            }
            else {
                response.getWriter().println(((InvocationTargetException) e).getMessage());
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        /*
        this serves as GET method for testing
        */
        String resp_msg = "CouchbaseLite Java WebService - Respond OK";

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Content-Type", "text/plain");
        response.getOutputStream().write(resp_msg.getBytes());
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }
}
