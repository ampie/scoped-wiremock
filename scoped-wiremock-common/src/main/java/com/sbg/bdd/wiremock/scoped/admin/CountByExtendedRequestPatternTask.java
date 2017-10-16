package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern;
import org.apache.commons.lang3.time.StopWatch;

import java.util.HashMap;
import java.util.Map;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;

public class CountByExtendedRequestPatternTask extends ScopeAdminTask {
    public CountByExtendedRequestPatternTask(ScopedAdmin scopedAdmin) {
        super(scopedAdmin);
    }

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        StopWatch sw = new StopWatch();
        sw.start();

        String bodyAsString = request.getBodyAsString();
        ExtendedRequestPattern newMapping = Json.read(bodyAsString, ExtendedRequestPattern.class);
        try {
            Map<String,Object> oj = new HashMap<>();
            int count = super.admin.count(newMapping);
            oj.put("count", count);
            return ResponseDefinitionBuilder.jsonResponse(oj, HTTP_OK);
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
