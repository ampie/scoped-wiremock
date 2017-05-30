package com.sbg.bdd.resource;

public interface Resource {
    String getPath();
    String getName();
    ResourceContainer getRoot();

    ResourceContainer getContainer();
}
