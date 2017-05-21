package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

public class SyncCorrelatedScopeTask extends ScopeAdminTask{
    public SyncCorrelatedScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        CorrelationState correlationState = Json.read(request.getBodyAsString(), CorrelationState.class);
        admin.syncCorrelatedScope(correlationState);
        return responseDefinition()
                .withStatus(HTTP_NO_CONTENT)
                .build();
    }
}
