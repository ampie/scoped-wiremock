package com.sbg.bdd.resource.http;

import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;

public class HttpResource implements Resource{
    private final HttpResourceContainer parent;
    private String name;

    public HttpResource(HttpResourceContainer parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String getPath() {
        if(parent instanceof HttpResourceRoot){
            return getName();
        }
        return parent.getPath() + "/" + getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HttpResourceRoot getRoot() {
        return parent.getRoot();
    }

    @Override
    public ResourceContainer getContainer() {
        return parent;
    }
}
