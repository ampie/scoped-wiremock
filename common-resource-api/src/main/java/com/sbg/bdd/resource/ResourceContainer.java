package com.sbg.bdd.resource;
//TODO migrate and align to WireMock's FileSource
public interface ResourceContainer extends Resource{
    Resource[] list();
    Resource[] list(ResourceFilter filter);
    Resource resolveOrFail(String ... segments) throws IllegalArgumentException;
    Resource resolveExisting(String ... segments);
    WritableResource resolvePotential(String ... segments);
    ResourceContainer resolvePotentialContainer(String ... segments);
    boolean fallsWithin(String path);

    Resource getChild(String segment);
}
