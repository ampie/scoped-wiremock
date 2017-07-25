package com.sbg.bdd.resource.file;

import com.sbg.bdd.resource.*;

import java.io.File;
import java.util.*;

public class DirectoryResource extends FileSystemResource implements ResourceContainer {

    private Map<String, FileSystemResource> children;
    public DirectoryResource(DirectoryResource parent, File file) {
        super(parent, file);
    }

    @Override
    public Resource resolveOrFail(String... segments) throws IllegalArgumentException {
        return ResourceSupport.resolveExisting(this, segments,true);
    }
    @Override
    public Resource[] list() {
        Map<String, FileSystemResource> children = getChildren();
        return children.values().toArray(new FileSystemResource[children.size()]);
    }
    public void clearCache(){
        children=null;
    }
    private Map<String, FileSystemResource> getChildren() {
        String[] actualChildrenNames = getFile().list();
        if (needsRefresh(actualChildrenNames)) {
            children = new TreeMap<>();
            if (actualChildrenNames != null) {
                for (int i = 0; i < actualChildrenNames.length; i++) {
                    children.put(actualChildrenNames[i], newChild(actualChildrenNames[i]));
                }
            }
        }
        return children;
    }

    private boolean needsRefresh(String[] actualChildrenNames) {
        return children == null || actualChildrenNames == null || actualChildrenNames.length != children.size();
    }

    private FileSystemResource newChild(String name) {
        File file = new File(getFile(), name);
        if (file.isDirectory()) {
            return new DirectoryResource(this, file);
        } else {
            return new ReadableFileResource(this, file);
        }
    }

    @Override
    public Resource[] list(ResourceFilter filter) {
        return ResourceSupport.list(filter, getChildren(), this);
    }

    @Override
    public Resource resolveExisting(String... segments) {
        FileSystemResource previous = this;
        return ResourceSupport.resolveExisting(previous, segments, false);
    }

    @Override
    public boolean fallsWithin(String path) {
        return !new File(path).isAbsolute();
    }

    @Override
    public ResourceContainer resolvePotentialContainer(String... segments) {
        String[] flattened = ResourceSupport.flatten(segments);
        FileSystemResource previous = resolvePotential(flattened, flattened.length);
        if (previous instanceof ResourceContainer) {
            return (ResourceContainer) previous;
        } else {
            return null;
        }
    }

    @Override
    public WritableResource resolvePotential(String... segments) {
        String[] flattened = ResourceSupport.flatten(segments);
        FileSystemResource previous = resolvePotential(flattened, flattened.length - 1);
        if (previous instanceof WritableResource) {
            return (WritableResource) previous;
        } else if (previous instanceof ReadableFileResource) {
            return ((ReadableFileResource) previous).asWritable();
        } else {
            return null;
        }
    }

    private FileSystemResource resolvePotential(String[] flattened, int lastDirectoryIndex) {
        FileSystemResource previous = this;
        for (int i = 0; i < flattened.length; i++) {
            if (previous instanceof DirectoryResource) {
                FileSystemResource child = ((DirectoryResource) previous).getChild(flattened[i]);
                if (child == null) {
                    if (i < lastDirectoryIndex) {
                        child = new DirectoryResource((DirectoryResource) previous, new File(previous.getFile(), flattened[i]));
                    } else {
                        child = new WritableFileResource((DirectoryResource) previous, new File(previous.getFile(), flattened[i]));
                    }
                }
                previous = child;
            }
        }
        return previous;
    }

    public FileSystemResource getChild(String segment) {
        return getChildren().get(segment);
    }
}
