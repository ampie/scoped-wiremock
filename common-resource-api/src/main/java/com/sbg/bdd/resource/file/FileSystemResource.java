package com.sbg.bdd.resource.file;

import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;

import java.io.File;

public abstract class FileSystemResource implements Resource {
    private DirectoryResource parent;
    private File file;

    public FileSystemResource(DirectoryResource parent, File file) {
        this.parent = parent;
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public ResourceContainer getContainer() {
        return getParent();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public DirectoryResource getRoot() {
        return getParent().getRoot();
    }

    @Override
    public String getPath() {
        return getParent().getPath() + "/" + getName();
    }

    public DirectoryResource getParent() {
        return parent;
    }
}
