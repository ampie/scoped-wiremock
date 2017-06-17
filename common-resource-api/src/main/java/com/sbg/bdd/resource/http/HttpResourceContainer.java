package com.sbg.bdd.resource.http;

import com.sbg.bdd.resource.*;

public class HttpResourceContainer extends HttpResource implements ResourceContainer {
    public HttpResourceContainer(HttpResourceContainer parent, String name) {
        super(parent, name);
    }

    @Override
    public Resource[] list() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public Resource[] list(ResourceFilter filter) {
//        Request.Builder builder = new Request.Builder().url(getRoot().getBaseUrl() + getPath());
//        Response response = getRoot().getClient().newCall(builder.build()).execute()
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public Resource resolveExisting(String... segments) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public WritableResource resolvePotential(String... segments) {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public HttpResourceContainer resolvePotentialContainer(String... segments) {
        String[] flatten = ResourceSupport.flatten(segments);
        return (HttpResourceContainer) resolvePotential(flatten, flatten.length);
    }

    @Override
    public boolean fallsWithin(String path) {
        return true;
    }

    @Override
    public HttpResource getChild(String segment) {
        return null;
    }

    private HttpResource resolvePotential(String[] flattened, int lastDirectoryIndex) {
        HttpResource previous = this;
        for (int i = 0; i < flattened.length; i++) {
            if (previous instanceof HttpResourceContainer) {
                HttpResource child = ((HttpResourceContainer) previous).getChild(flattened[i]);
                if (child == null) {
                    if (i < lastDirectoryIndex) {
                        child = new HttpResourceContainer((HttpResourceContainer) previous, flattened[i]);
                    } else {
                        child = new WritableHttpResource((HttpResourceContainer) previous, flattened[i]);
                    }
                }
                previous = child;
            }
        }
        return previous;
    }
}
