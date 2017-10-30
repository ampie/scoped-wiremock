package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;
//NB!!! Use with care. Just for testing really
public class ResetAllScopesTask extends  ScopeAdminTask{
    public ResetAllScopesTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        super.admin.resetAll();
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody("{}")
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
