package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;


public class InvalidHeadersLoggingTransformer extends ResponseDefinitionTransformer {

    public InvalidHeadersLoggingTransformer() {
    }

    @Override
    public String getName() {
        return "InvalidHeadersLoggingTransformer";
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource fileSource, Parameters parameters) {
        if (request.getHeader(HeaderName.ofTheCorrelationKey()) == null && request.getHeader(HeaderName.toProxyUnmappedEndpoints()) == null) {
            String message = "Request " + request.getUrl() + " has neither a Correlation Key header nor a Proxy Unmapped Endpoints header";
            LocalNotifier.notifier().error(message);
//            responseDefinition= ResponseDefinitionBuilder.like(responseDefinition).withStatusMessage(message).build();
        }
        return responseDefinition;
    }

    public boolean applyGlobally() {
        return true;
    }
}
