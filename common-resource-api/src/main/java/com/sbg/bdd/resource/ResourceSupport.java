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
            if (segment.equals("..")) {
                previous = previous.getContainer();
            } else if (segment.equals(".")) {
                previous = previous;// you get the point
            } else if (previous instanceof ResourceContainer) {
                ResourceContainer previousDir = (ResourceContainer) previous;
                previous = previousDir.getChild(segment);
                if(previous == null && failWhenNotFound){
                    throw new IllegalArgumentException("Could not find the resource " + String.join("/" + segments) + ". The offending segment is '" + segment +"'");
                }
            }
        }
        return (T) previous;
    }

    public static String[] flatten(String[] segments) {
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
}
