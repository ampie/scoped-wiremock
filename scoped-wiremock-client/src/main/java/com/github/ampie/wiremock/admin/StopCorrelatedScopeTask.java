package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class StopCorrelatedScopeTask extends ScopeAdminTask{
    public StopCorrelatedScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        CorrelationState correlationState = Json.read(request.getBodyAsString(), CorrelationState.class);
        List<String> removedScopePaths= admin.stopCorrelatedScope(correlationState.getCorrelationPath());
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(removedScopePaths))
                .withHeader("Content-Type", "application/json")
                .build();
    }

}
