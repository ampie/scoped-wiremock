package com.sbg.bdd.resource.file;

import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceRoot;
import com.sbg.bdd.resource.ResourceSupport;

import java.io.File;

public class DirectoryResourceRoot extends DirectoryResource implements ResourceRoot {
    private final String rootName;

    public DirectoryResourceRoot(String rootName, File file) {
        super(null, file);
        this.rootName = rootName;
    }

    @Override
    public DirectoryResourceRoot getRoot() {
        return this;
    }

    @Override
    public String getPath() {
        return "";
    }

    public String getName() {
        return getRootName();
    }

    @Override
    public String getRootName() {
        return rootName;
    }
}
