package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.ampie.wiremock.extended.BadMappingException;
import org.apache.commons.lang3.time.StopWatch;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;

public class CreateStubMappingTask implements AdminTask {

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        StopWatch sw = new StopWatch();
        sw.start();

        StubMapping newMapping = StubMapping.buildFrom(request.getBodyAsString());
        try {
            admin.addStubMapping(newMapping);
            return ResponseDefinitionBuilder.jsonResponse(newMapping, HTTP_CREATED);
        } catch (BadMappingException e) {
            return ResponseDefinitionBuilder.responseDefinition().
                    withStatus(HTTP_BAD_REQUEST).
                    withBody(e.getMessage()).build();
        }finally{
            sw.stop();
            System.out.println(request.getAbsoluteUrl() + " took " + sw.getTime());
        }
    }
}
