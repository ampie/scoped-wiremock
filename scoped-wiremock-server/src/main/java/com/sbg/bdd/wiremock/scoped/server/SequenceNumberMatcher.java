package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.ServiceInvocationCount;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.URLHelper;

import java.net.MalformedURLException;
import java.net.URL;

public class SequenceNumberMatcher extends RequestMatcherExtension {
    public static final String NAME = "SequenceNumberMatcher";
    public static final String SEQUENCE_NUMBER = "sequenceNumber";
    public static final String CORRELATION_PATH = "correlationPath";
    public static final String THREAD_CONTEXT_ID = "threadContextId";
    private CorrelatedScopeAdmin admin;

    public void setAdmin(CorrelatedScopeAdmin admin) {
        this.admin = admin;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getExpected() {
        return "Sequence Number Matching";
    }

    @Override
    public MatchResult match(Request request, Parameters parameters) {
        HttpHeader correlationPathHeader = request.getHeaders().getHeader(HeaderName.ofTheCorrelationKey());
        HttpHeader threadContextIdHeader = request.getHeaders().getHeader(HeaderName.ofTheThreadContextId());
        if (isContextMatch(parameters, correlationPathHeader, threadContextIdHeader)) {
            CorrelationState correlationState = admin.getCorrelatedScope(correlationPathHeader.firstValue());
            String serviceIdentifier = serviceIdentifierOf(request);
            int threadContextId = Integer.parseInt(threadContextIdHeader.firstValue());
            ServiceInvocationCount previousCount = correlationState.findOrCreateServiceInvocationCount(threadContextId, serviceIdentifier);
            int sequenceNumber = parameters.getInt(SEQUENCE_NUMBER);//Null not an option
            if (previousCount.getCount() + 1 == sequenceNumber) {
                previousCount.increment();
                return MatchResult.exactMatch();
            }else{
                return MatchResult.partialMatch(1.0);
            }
        }
        return MatchResult.noMatch();
    }

    private boolean isContextMatch(Parameters parameters, HttpHeader correlationPathHeader, HttpHeader threadContextIdHeader) {
        boolean headersPresent = correlationPathHeader.isPresent() && threadContextIdHeader.isPresent();
        if(headersPresent) {
            boolean headersMatchParameters = parameters.getString(CORRELATION_PATH).equals(correlationPathHeader.firstValue()) &&
                    parameters.getString(THREAD_CONTEXT_ID).equals(threadContextIdHeader.firstValue());
            return headersMatchParameters;
        }else{
            return false;
        }
    }

    private String serviceIdentifierOf(Request request) {
        try {
            return URLHelper.identifier(new URL(request.getAbsoluteUrl()), request.getMethod().getName());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
