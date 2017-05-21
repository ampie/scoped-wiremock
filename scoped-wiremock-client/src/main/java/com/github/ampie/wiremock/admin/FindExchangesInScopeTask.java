package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.ampie.wiremock.RecordedExchange;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class FindExchangesInScopeTask extends ScopeAdminTask {
    public FindExchangesInScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        RequestPattern requestPattern = Json.read(request.getBodyAsString(), RequestPattern.class);
        List<RecordedExchange> exchanges= admin.findMatchingExchanges(requestPattern);
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(exchanges))
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
