package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceState;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceType;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class ListResourcesTask extends ScopeAdminTask {
    public ListResourcesTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        ResourceState resourceState = Json.read(request.getBodyAsString(), ResourceState.class);
        ResourceContainer resourceRoot = super.admin.getResourceRoot(resourceState.getResourceRoot());
        ResourceContainer resourceContainer = (ResourceContainer) resourceRoot.resolveExisting(resourceState.getPath());
        List<ResourceState> children = new ArrayList<>();
        if (resourceContainer != null) {
            Resource[] resources = resourceContainer.list();
            for (Resource resource : resources) {
                ResourceState child = new ResourceState(resourceRoot.getName(), resource.getPath());
                if (resource instanceof ResourceContainer) {
                    child.setType(ResourceType.CONTAINER);
                } else if (((ReadableResource) resource).canWrite()) {
                    child.setType(ResourceType.READ_WRITE);
                } else {
                    child.setType(ResourceType.READ_ONlY);

                }
                children.add(child);
            }
        }
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(children))
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
