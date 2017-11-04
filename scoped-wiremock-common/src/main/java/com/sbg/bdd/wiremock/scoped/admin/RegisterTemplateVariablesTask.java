package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

public class RegisterTemplateVariablesTask extends ScopeAdminTask{
    public RegisterTemplateVariablesTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        CorrelationState correlationState = Json.read(request.getBodyAsString(), CorrelationState.class);
        admin.registerTemplateVariables(correlationState);
        return responseDefinition()
                .withStatus(HTTP_NO_CONTENT)
                .build();
    }
}
