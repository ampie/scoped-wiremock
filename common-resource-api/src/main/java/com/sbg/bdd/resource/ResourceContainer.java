package com.sbg.bdd.resource;

public interface ResourceContainer extends Resource{
    Resource[] list();
    Resource[] list(ResourceFilter filter);
    Resource resolveExisting(String ... segments);
    WritableResource resolvePotential(String ... segments);
    ResourceContainer resolvePotentialContainer(String ... segments);
    boolean fallsWithin(String path);

    Resource getChild(String segment);
}
