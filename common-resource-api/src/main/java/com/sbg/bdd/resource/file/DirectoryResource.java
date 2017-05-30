package com.sbg.bdd.resource.file;

import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.resource.WritableResource;

import java.io.File;
import java.util.*;

public class DirectoryResource extends FileSystemResource implements ResourceContainer {

    private Map<String, FileSystemResource> children;

    public DirectoryResource(DirectoryResource parent, File file) {
        super(parent, file);
    }


    @Override
    public Resource[] list() {
        Map<String, FileSystemResource> children = getChildren();
        return children.values().toArray(new FileSystemResource[children.size()]);
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
        return list(filter, getChildren(), this);
    }

    public static Resource[] list(ResourceFilter filter, Map<String, ? extends Resource> children, ResourceContainer container) {
        List<Resource> result = new ArrayList<>();
        for (Map.Entry<String, ? extends Resource> entry : children.entrySet()) {
            if (filter.accept(container, entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result.toArray(new Resource[result.size()]);
    }

    @Override
    public Resource resolveExisting(String... segments) {
        FileSystemResource previous = this;
        return resolveExisting(previous, segments);
    }

    public static <T extends Resource> T resolveExisting(Resource previous, String[] segments) {
        for (String segment : flatten(segments)) {
            if (previous instanceof ResourceContainer) {
                ResourceContainer previousDir = (ResourceContainer) previous;
                previous = previousDir.getChild(segment);
            }
        }
        return (T)previous;
    }

    private static String[] flatten(String[] segments) {
        List<String> result = new ArrayList<>();
        for (String s : segments) {
            if (s != null) {
                String[] split = s.split("\\/");
                for (String atomicSegment : split) {
                    if (atomicSegment.trim().length() > 0) {
                        result.add(atomicSegment);
                    }
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public boolean fallsWithin(String path) {
        return !new File(path).isAbsolute();
    }

    @Override
    public ResourceContainer resolvePotentialContainer(String... segments) {
        String[] flattened = flatten(segments);
        FileSystemResource previous = resolvePotential(flattened, flattened.length);
        if (previous instanceof ResourceContainer) {
            return (ResourceContainer) previous;
        } else {
            return null;
        }
    }

    @Override
    public WritableResource resolvePotential(String... segments) {
        String[] flattened = flatten(segments);
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
