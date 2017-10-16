package com.sbg.bdd.wiremock.scoped.resources;

import com.github.tomakehurst.wiremock.admin.AdminRoutes;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.sbg.bdd.resource.ResourceRoot;
import com.sbg.bdd.wiremock.scoped.admin.ScopeAdminTask;
import com.sbg.bdd.wiremock.scoped.admin.ScopeExtensionsOnClient;
import okhttp3.OkHttpClient;

import java.util.Arrays;

public class WireMockResourceRoot extends WireMockResourceContainer implements ResourceRoot {
    private final AdminRoutes scopedAdminRoutes;
    private String baseUrl;
    private String rootName;
    private OkHttpClient client = new OkHttpClient();
    public WireMockResourceRoot(String baseUrl, String name) {
        super(null, name);
        this.baseUrl = baseUrl;
        rootName=name;
        this.scopedAdminRoutes = AdminRoutes.defaultsPlus(Arrays.<AdminApiExtension>asList(new ScopeExtensionsOnClient()));

    }

    @Override
    public WireMockResourceRoot getRoot() {
        return this;
    }

    @Override
    public String getPath() {
        return "";
    }

    @Override
    public String getRootName() {
        return rootName;
    }

    public OkHttpClient getClient() {
        return client;
    }
    public String getBaseUrlFor(Class<? extends ScopeAdminTask> cls){
        return baseUrl + "/__admin" + this.scopedAdminRoutes.requestSpecForTask(cls).path();
    }

}
