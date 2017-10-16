package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import org.apache.commons.lang3.time.StopWatch;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static java.net.HttpURLConnection.HTTP_OK;

public class JoinKnownCorrelatedScopeTask extends ScopeAdminTask{
    public JoinKnownCorrelatedScopeTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin a, Request request, PathParams pathParams) {
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            CorrelationState correlationState = Json.read(request.getBodyAsString(), CorrelationState.class);
            CorrelationState scope = admin.startNestedScope(correlationState);
            return responseDefinition()
                    .withStatus(HTTP_OK)
                    .withBody(Json.write(scope))
                    .withHeader("Content-Type", "application/json")
                    .build();
        }finally{
            sw.stop();
            System.out.println(request.getAbsoluteUrl() + " took " + sw.getTime());
        }
    }
}
