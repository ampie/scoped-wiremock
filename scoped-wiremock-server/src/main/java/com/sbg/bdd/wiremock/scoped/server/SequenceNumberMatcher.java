package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.*;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.ServiceInvocationCount;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.URLHelper;

import java.net.MalformedURLException;
import java.net.URL;

import static com.sbg.bdd.wiremock.scoped.server.decorated.InMemoryStubMappingsDecorator.serviceIdentifierOf;

public class SequenceNumberMatcher implements ValueMatcher<Request> {
    private CorrelatedScopeAdmin admin;
    private int threadContextId;
    private int sequenceNumber;
    private StringValuePattern correlationPattern;
    private UrlPathPattern urlPattern;

    public void setThreadContextId(int threadContextId) {
        this.threadContextId = threadContextId;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setCorrelationPattern(StringValuePattern correlationPattern) {
        this.correlationPattern = correlationPattern;
    }

    public StringValuePattern getCorrelationPattern() {
        return correlationPattern;
    }

    public void setAdmin(CorrelatedScopeAdmin admin) {
        this.admin = admin;
    }

    @Override
    public MatchResult match(Request request) {
        if (urlPattern.match(request.getUrl()).isExactMatch()) {
            HttpHeader correlationPathHeader = request.getHeaders().getHeader(HeaderName.ofTheCorrelationKey());
            HttpHeader threadContextIdHeader = request.getHeaders().getHeader(HeaderName.ofTheThreadContextId());
            if (isContextMatch(correlationPathHeader, threadContextIdHeader)) {
                CorrelationState correlationState = admin.getCorrelatedScope(correlationPathHeader.firstValue());
                String serviceIdentifier = serviceIdentifierOf(request);
                int threadContextId = Integer.parseInt(threadContextIdHeader.firstValue());
                ServiceInvocationCount previousCount = correlationState.findOrCreateServiceInvocationCount(threadContextId, serviceIdentifier);
                if (previousCount.getCount() == sequenceNumber) {
                    return MatchResult.exactMatch();
                } else {
                    return MatchResult.partialMatch(1.0);
                }
            }
        }
        return MatchResult.noMatch();
    }

    private boolean isContextMatch(HttpHeader correlationPathHeader, HttpHeader threadContextIdHeader) {
        boolean headersPresent = correlationPathHeader.isPresent() && threadContextIdHeader.isPresent();
        if (headersPresent) {
            boolean headersMatchParameters = correlationPattern.match(correlationPathHeader.firstValue()).isExactMatch() &&
                    threadContextIdHeader.firstValue().equals(String.valueOf(threadContextId));
            return headersMatchParameters;
        } else {
            return false;
        }
    }


    public void setUrlPattern(UrlPathPattern urlPattern) {
        this.urlPattern = urlPattern;
    }
}
