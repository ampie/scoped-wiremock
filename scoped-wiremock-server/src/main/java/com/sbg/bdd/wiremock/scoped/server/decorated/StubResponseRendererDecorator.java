package com.sbg.bdd.wiremock.scoped.server.decorated;

import com.github.tomakehurst.wiremock.http.*;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.ExchangeJournal;

import static com.sbg.bdd.wiremock.scoped.server.decorated.InMemoryStubMappingsDecorator.rectifyRequestHeaders;

public class StubResponseRendererDecorator implements ResponseRenderer {
    private final StubResponseRenderer delegate;
    private final ScopedAdmin scopedAdmin;
    private final ExchangeJournal exchangeJournal;

    public StubResponseRendererDecorator(StubResponseRenderer delegate, ScopedAdmin scopedAdmin, ExchangeJournal exchangeJournal) {
        this.delegate = delegate;
        this.scopedAdmin = scopedAdmin;
        this.exchangeJournal = exchangeJournal;
    }

    @Override
    public Response render(ResponseDefinition responseDefinition) {
        Request request = responseDefinition.getOriginalRequest();
        String scopePath = request.getHeader(HeaderName.ofTheCorrelationKey());
        if (scopePath != null) {
            String stepName = determineStep(scopePath);
            //NB! the normal RequestListener or ResponseTransformer won't work because we want to support recursive requests
            //to build a stack of exchanges
            RecordedExchange exchange = exchangeJournal.requestReceived(scopePath, stepName, rectifyRequestHeaders(request));
            try {
                Response response = delegate.render(responseDefinition);
                exchangeJournal.responseReceived(exchange, response);
                return response;
            } catch (RuntimeException e) {
                e.printStackTrace();
                exchangeJournal.responseReceived(exchange, Response.notConfigured());
                throw e;
            }
        } else {
            return delegate.render(responseDefinition);
        }
    }

    private String determineStep(String scopePath) {
        String stepContainerPath = ParentPath.ofUserScope(scopePath);
        CorrelationState correlationState = scopedAdmin.getCorrelatedScope(stepContainerPath);
        //CorrelationState could be null if we are not using the scoped client
        return correlationState == null ? null : correlationState.getCurrentStep();
    }
}
