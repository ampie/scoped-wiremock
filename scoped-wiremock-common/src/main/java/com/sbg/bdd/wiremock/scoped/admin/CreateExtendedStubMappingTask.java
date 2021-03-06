package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping;
import org.apache.commons.lang3.time.StopWatch;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;

public class CreateExtendedStubMappingTask extends ScopeAdminTask {
    public CreateExtendedStubMappingTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        StopWatch sw = new StopWatch();
        sw.start();

        String bodyAsString = request.getBodyAsString();
        ExtendedStubMapping newMapping = Json.read(bodyAsString, ExtendedStubMapping.class);
        try {
            super.admin.register(newMapping);
            return ResponseDefinitionBuilder.jsonResponse(newMapping, HTTP_CREATED);
        } catch (BadMappingException e) {
            return ResponseDefinitionBuilder.responseDefinition().
                    withStatus(HTTP_BAD_REQUEST).
                    withBody(e.getMessage()).build();
        } finally {
            sw.stop();
            System.out.println(request.getAbsoluteUrl() + " took " + sw.getTime());
        }
    }
}
