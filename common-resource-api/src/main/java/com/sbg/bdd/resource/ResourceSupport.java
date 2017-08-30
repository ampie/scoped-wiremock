package com.sbg.bdd.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceSupport {
    public static Resource[] list(ResourceFilter filter, Map<String, ? extends Resource> children, ResourceContainer container) {
        List<Resource> result = new ArrayList<>();
        for (Map.Entry<String, ? extends Resource> entry : children.entrySet()) {
            if (filter.accept(container, entry.getKey())) {
                result.add(entry.getValue());
            }
        }
        return result.toArray(new Resource[result.size()]);
    }

    public static <T extends Resource> T resolveExisting(Resource previous, String[] segments, boolean failWhenNotFound) {
        for (String segment : flatten(segments)) {
            if (previous instanceof ResourceContainer) {
                ResourceContainer previousDir = (ResourceContainer) previous;
                previous = previousDir.getChild(segment);
                if (previous == null && failWhenNotFound) {
                    throw new IllegalArgumentException("Could not find the resource " + previous.getRoot().getRootName() +":/" + previous.getPath()+ "/" +join(segments) + ". The offending segment is '" + segment + "'");
                }
            }
        }
        return (T) previous;
    }

    private static String join(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            sb.append("/");
            sb.append(segment);
        }
        return sb.toString();
    }

    public static String[] flatten(String[] segments) {
        List<String> result = new ArrayList<>();
        for (String s : segments) {
            if (s != null) {
                String[] split = s.split("\\/");
                for (String atomicSegment : split) {
                    if (atomicSegment.trim().length() > 0) {
                        if (atomicSegment.equals("..")) {
                            result.remove(result.size() - 1);
                        } else if (atomicSegment.equals(".")) {
                            //nothing
                        } else {
                            result.add(atomicSegment);
                        }
                    }
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }
}
