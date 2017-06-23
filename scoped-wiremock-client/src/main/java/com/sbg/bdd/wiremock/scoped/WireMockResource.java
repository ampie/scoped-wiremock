package com.sbg.bdd.wiremock.scoped;

import com.sbg.bdd.resource.Resource;
import okhttp3.MediaType;

public class WireMockResource implements Resource {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private WireMockResourceContainer container;
    private String name;

    public WireMockResource(WireMockResourceContainer container, String name) {
        this.container = container;
        this.name = name;
    }

    @Override
    public String getPath() {
        if (getContainer() instanceof WireMockResourceRoot) {
            return getName();
        } else {
            return getContainer().getPath() + "/" + getName();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public WireMockResourceRoot getRoot() {
        return getContainer().getRoot();
    }

    @Override
    public WireMockResourceContainer getContainer() {
        return container;
    }
}
