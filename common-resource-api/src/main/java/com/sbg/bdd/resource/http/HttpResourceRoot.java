package com.sbg.bdd.resource.http;

import com.sbg.bdd.resource.ResourceRoot;
import okhttp3.OkHttpClient;

public class HttpResourceRoot extends HttpResourceContainer implements ResourceRoot{
    private String rootName;
    private String baseUrl;
    private OkHttpClient client = new OkHttpClient();

    public HttpResourceRoot(String rootName, String baseUrl) {
        super(null, rootName);
        this.rootName = rootName;

        this.baseUrl = baseUrl;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public HttpResourceRoot getRoot() {
        return this;
    }

    @Override
    public String getRootName() {
        return rootName;
    }
}
