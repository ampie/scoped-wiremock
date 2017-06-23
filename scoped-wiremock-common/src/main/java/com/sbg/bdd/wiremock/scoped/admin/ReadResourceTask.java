package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.model.ResourceState;
import com.sbg.bdd.wiremock.scoped.common.MimeTypeHelper;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class ReadResourceTask extends ScopeAdminTask {
    public ReadResourceTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        ResourceState resourceState = Json.read(request.getBodyAsString(), ResourceState.class);
        ResourceContainer resourceRoot = super.admin.getResourceRoot(resourceState.getResourceRoot());
        ReadableResource resource = (ReadableResource) resourceRoot.resolveExisting(resourceState.getPath());
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(resource.read())
                .withHeader("Content-Type", MimeTypeHelper.determineContentType(resource.getName()))
                .build();
    }
}
