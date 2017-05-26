package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class FindExchangesAgainstStepTask extends ScopeAdminTask {
    public FindExchangesAgainstStepTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        CorrelationState state = Json.read(request.getBodyAsString(), CorrelationState.class);
        List<RecordedExchange> exchanges= admin.findExchangesAgainstStep(state.getCorrelationPath(), state.getCurrentStep());
        return responseDefinition()
                .withStatus(HTTP_OK)
                .withBody(Json.write(exchanges))
                .withHeader("Content-Type", "application/json")
                .build();
    }
}
