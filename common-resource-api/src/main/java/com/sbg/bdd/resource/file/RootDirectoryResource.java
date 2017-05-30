package com.sbg.bdd.resource.file;

import java.io.File;

public class RootDirectoryResource extends DirectoryResource {
    public RootDirectoryResource(File file) {
        super(null, file);
    }

    @Override
    public DirectoryResource getRoot() {
        return this;
    }

    @Override
    public String getPath() {
        return "";
    }
    public String getName(){
        return "";
    }
}
