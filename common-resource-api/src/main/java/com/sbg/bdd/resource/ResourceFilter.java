package com.sbg.bdd.resource;

public interface ResourceFilter {
    boolean accept(ResourceContainer container, String name);
}
