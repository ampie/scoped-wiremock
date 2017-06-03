package com.sbg.bdd.wiremock.scoped.recording.endpointconfig;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import static com.sbg.bdd.wiremock.scoped.HttpAdminClient.getEntityAsStringAndCloseStream;


public abstract class BaseHttpClient {
    protected static Map<String, Integer> statusMap = new HashMap<>();
    protected OkHttpClient client = new OkHttpClient();


    static {
        statusMap.put("POST", HttpURLConnection.HTTP_CREATED);
        statusMap.put("DELETE", HttpURLConnection.HTTP_NO_CONTENT);
        statusMap.put("GET", HttpURLConnection.HTTP_OK);
        statusMap.put("PUT", HttpURLConnection.HTTP_OK);
    }

    public BaseHttpClient(OkHttpClient httpClient) {
        this.client = httpClient;
    }

    public ObjectNode execute(Request request) throws IOException {
        Response response = client.newCall(request).execute();
        if (response == null) {
            throw new IllegalStateException(request.url() + " " + request.method() + " not mocked!");
        }
        String resultString = getResponseString(response);
        Integer status = statusMap.get(request.method());
        if (response.code() == status || response.code() == 200) {
            if (resultString == null || resultString.length() == 0) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            return (ObjectNode) mapper.readTree(resultString);
        } else {
            throw new IllegalArgumentException(response.message() + ": " + resultString);
        }

    }

    private String getResponseString(Response response) throws IOException {
        String string = getEntityAsStringAndCloseStream(response);
        return string;
    }


}
