package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState;

import java.net.MalformedURLException;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;

public class StopGlobalScopeTask extends ScopeAdminTask {
    public StopGlobalScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        GlobalCorrelationState correlationState = Json.read(request.getBodyAsString(), GlobalCorrelationState.class);
        GlobalCorrelationState scope = admin.stopGlobalScope(correlationState);
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(scope))
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
