package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class GetCorrelatedScopeTask extends ScopeAdminTask{
    public GetCorrelatedScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        CorrelationState correlationState = Json.read(request.getBodyAsString(), CorrelationState.class);
        CorrelationState scope= admin.getCorrelatedScope(correlationState.getCorrelationPath());
        if(scope==null){
            return ResponseDefinition.notFound();
        }
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(scope))
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
