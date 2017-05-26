package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class GetMappingsInScopeTask extends ScopeAdminTask {
    public GetMappingsInScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        CorrelationState requestPattern = Json.read(request.getBodyAsString(), CorrelationState.class);
        List<StubMapping> mappings= admin.getMappingsInScope(requestPattern.getCorrelationPath());
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(mappings))
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
