package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.WritableResource;
import com.sbg.bdd.resource.file.ReadableFileResource;
import com.sbg.bdd.wiremock.scoped.admin.WriteResourceTask;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceState;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class WritableWireMockResource extends WireMockResource implements WritableResource {
    private boolean canRead;

    public WritableWireMockResource(WireMockResourceContainer container, String name, boolean canRead) {
        super(container, name);
        this.canRead = canRead;
    }

    @Override
    public void write(byte[] data) {
        try {
            Request.Builder builder = new Request.Builder().url(getRoot().getBaseUrlFor(WriteResourceTask.class));
            ResourceState resourceState = new ResourceState(getRoot().getRootName(), getPath());
            resourceState.setData(data);
            builder = builder.post(RequestBody.create(JSON, Json.write(resourceState)));
            Response response = getRoot().getClient().newCall(builder.build()).execute();
            int code = response.code();
            if (code != 200) {
                throw new IllegalStateException(response.message() + ": " + getPath());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean canRead() {
        return canRead;
    }

    @Override
    public ReadableWireMockResource asReadable() {
        if (canRead()) {
            return (ReadableWireMockResource) getContainer().getChild(getName());
        } else {
            throw new IllegalStateException("File " + getPath() + " does not exist yet or cannot be read");
        }
    }
}
