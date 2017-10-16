package com.sbg.bdd.wiremock.scoped.resources;

import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.wiremock.scoped.admin.ReadResourceTask;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceState;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.io.IOException;

public class ReadableWireMockResource extends WireMockResource implements ReadableResource {
    private final ResourceType type;

    public ReadableWireMockResource(WireMockResourceContainer wireMockResourceContainer, String name, ResourceType type) {
        super(wireMockResourceContainer, name);
        this.type = type;
    }

    @Override
    public byte[] read() {
        try {
            Request.Builder builder = new Request.Builder().url(getRoot().getBaseUrlFor(ReadResourceTask.class));
            builder = builder.post(RequestBody.create(JSON, Json.write(new ResourceState(getRoot().getRootName(), getPath()))));
            ResponseBody body = getRoot().getClient().newCall(builder.build()).execute().body();
            return body.bytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public WritableWireMockResource asWritable() {
        return new WritableWireMockResource(this.getContainer(), getName(), true);
    }

    @Override
    public boolean canWrite() {
        return type == ResourceType.READ_WRITE;
    }
}
