package com.sbg.bdd.resource;

public interface Resource {
    String getPath();
    String getName();
    ResourceRoot getRoot();

    ResourceContainer getContainer();
}
