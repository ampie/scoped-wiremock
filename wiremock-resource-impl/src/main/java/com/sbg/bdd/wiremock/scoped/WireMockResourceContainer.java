package com.sbg.bdd.wiremock.scoped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.resource.ResourceSupport;
import com.sbg.bdd.wiremock.scoped.admin.ListResourcesTask;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceState;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WireMockResourceContainer extends WireMockResource implements ResourceContainer {
    private Map<String, WireMockResource> children;

    public WireMockResourceContainer(WireMockResourceContainer container, String name) {
        super(container, name);
    }

    @Override
    public WireMockResource[] list() {
        return getChildren().values().toArray(new WireMockResource[getChildren().size()]);
    }

    private Map<String, WireMockResource> getChildren() {
        if (children == null) {
            try {
                children = new TreeMap<>();
                Request.Builder builder = new Request.Builder().url(getRoot().getBaseUrlFor(ListResourcesTask.class));
                builder = builder.post(RequestBody.create(JSON, Json.write(new ResourceState(getRoot().getRootName(), getPath()))));
                ResponseBody body = getRoot().getClient().newCall(builder.build()).execute().body();
                ObjectMapper mapper = Json.getObjectMapper();
                CollectionLikeType type = Json.getObjectMapper().getTypeFactory().constructCollectionType(List.class, ResourceState.class);
                List<ResourceState> list = mapper.readValue(body.string(), type);
                for (ResourceState s : list) {
                    switch (s.getType()) {
                        case CONTAINER:
                            children.put(s.getName(), new WireMockResourceContainer(this, s.getName()));
                            break;
                        default:
                            children.put(s.getName(), new ReadableWireMockResource(this, s.getName(), s.getType()));
                            break;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return children;
    }

    @Override
    public WireMockResource[] list(ResourceFilter filter) {
        return (WireMockResource[]) ResourceSupport.list(filter, getChildren(), this);
    }

    @Override
    public WireMockResource resolveOrFail(String... segments) throws IllegalArgumentException {
        return ResourceSupport.resolveExisting(this, segments, true);
    }

    @Override
    public WireMockResource resolveExisting(String... segments) {
        return ResourceSupport.resolveExisting(this, segments, false);
    }

    @Override
    public WireMockResourceContainer resolvePotentialContainer(String... segments) {
        String[] flattened = ResourceSupport.flatten(segments);
        WireMockResource previous = resolvePotential(flattened, flattened.length);
        if (previous instanceof WireMockResourceContainer) {
            return (WireMockResourceContainer) previous;
        } else {
            return null;
        }
    }

    @Override
    public WritableWireMockResource resolvePotential(String... segments) {
        String[] flattened = ResourceSupport.flatten(segments);
        WireMockResource previous = resolvePotential(flattened, flattened.length - 1);
        if (previous instanceof WritableWireMockResource) {
            return (WritableWireMockResource) previous;
        } else if (previous instanceof ReadableWireMockResource) {
            return ((ReadableWireMockResource) previous).asWritable();
        } else {
            return null;
        }
    }

    private WireMockResource resolvePotential(String[] flattened, int lastDirectoryIndex) {
        WireMockResource previous = this;
        for (int i = 0; i < flattened.length; i++) {
            if (previous instanceof WireMockResourceContainer) {
                WireMockResource child = ((WireMockResourceContainer) previous).getChild(flattened[i]);
                if (child == null) {
                    if (i < lastDirectoryIndex) {
                        child = new WireMockResourceContainer((WireMockResourceContainer) previous, flattened[i]);
                    } else {
                        child = new WritableWireMockResource((WireMockResourceContainer) previous, flattened[i], false);
                    }
                }
                previous = child;
            }
        }
        return previous;
    }

    @Override
    public boolean fallsWithin(String path) {
        return path.startsWith(getPath());
    }

    @Override
    public WireMockResource getChild(String segment) {
        return getChildren().get(segment);
    }
}
