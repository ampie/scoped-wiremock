package com.sbg.bdd.resource.http;

import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.WritableResource;

public class WritableHttpResource extends HttpResource implements WritableResource {
    public WritableHttpResource(HttpResourceContainer parent, String name) {
        super(parent, name);
    }

    @Override
    public void write(byte[] data) {

    }

    @Override
    public boolean canRead() {
        return false;
    }

    @Override
    public ReadableResource asReadable() {
        return null;
    }
}
