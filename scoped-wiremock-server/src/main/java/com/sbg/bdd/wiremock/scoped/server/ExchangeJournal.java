package com.sbg.bdd.wiremock.scoped.server;


import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.server.ScopePathMatcher.matches;
import static com.sbg.bdd.wiremock.scoped.server.decorated.InMemoryStubMappingsDecorator.serviceIdentifierOf;

// WAS necessary because the normal InMemoryRequestJournal only stored the request with the responseDefinition, not the actual response
//TODO merge with InMemoryRequestJournalDecorator. Would have to make RecordedExchange extend ServeEvent
public class ExchangeJournal {
    private ConcurrentLinkedQueue<RecordedExchange> recordings = new ConcurrentLinkedQueue<>();
    private Map<String, Collection<RecordedExchange>> exchangesByStep = new ConcurrentHashMap<>();

    public RecordedExchange requestReceived(CorrelationState scope, String step, Request request) {
        try {
            RecordedExchange exchange = new RecordedExchange(buildRecordedRequest(request), scope.getCorrelationPath(), step);
            String threadContextIdString = request.getHeader(HeaderName.ofTheThreadContextId());
            int threadContextId = 0;
            if (threadContextIdString != null) {
                threadContextId = Integer.parseInt(threadContextIdString);
                exchange.setThreadContextId(threadContextId);
            }
            ServiceInvocationCount sic = scope.findOrCreateServiceInvocationCount(threadContextId, serviceIdentifierOf(request));
            exchange.setSequenceNumber(sic.getCount());
            Collection<RecordedExchange> exchangesInStep = findActiveExchangesInStep(scope.getCorrelationPath(), step);
            synchronized (exchangesInStep) {
                if (exchangesInStep.isEmpty()) {
                    exchange.setRootExchange(true);
                } else {
                    addToMostRecentParentExchange(exchange, exchangesInStep);
                }
                exchangesInStep.add(exchange);
            }
            this.recordings.add(exchange);
            return exchange;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void addToMostRecentParentExchange(RecordedExchange exchange, Collection<RecordedExchange> exchangesInStep) {
        RecordedExchange mostRecentParentExchange = null;
        String currentThreadContextId = String.valueOf(exchange.getThreadContextId());
        int offset = currentThreadContextId.length();
        for (RecordedExchange potentialParentExchange : exchangesInStep) {
            String potentialParentThreadContextId = String.valueOf(potentialParentExchange.getThreadContextId());
            if (currentThreadContextId.startsWith(potentialParentThreadContextId) && offset > potentialParentThreadContextId.length() - currentThreadContextId.length()) {
                mostRecentParentExchange = potentialParentExchange;
                offset = potentialParentThreadContextId.length() - currentThreadContextId.length();
            }
        }
        //We may want to verify the parentExchange has not yet received a response, but with async calls this may not always be true
        mostRecentParentExchange.recordNestedExchange(exchange);
    }

    public Collection<RecordedExchange> findActiveExchangesInStep(String scopePath, String step) {
        String stepKey = contextKey(ParentPath.ofUserScope(scopePath), step);
        Collection<RecordedExchange> activeExchanges = exchangesByStep.get(stepKey);
        if (activeExchanges == null) {
            activeExchanges = new ArrayList<>();
            exchangesByStep.put(stepKey, activeExchanges);
        }
        return activeExchanges;
    }

    private String contextKey(String scopePath, String step) {
        return scopePath + "/" + step;
    }


    public int count(List<RequestPattern> patterns) {
        int result = 0;
        for (RequestPattern pattern : patterns) {
            for (RecordedExchange recording : this.recordings) {
                if (pattern.match(recording.getRequest()).isExactMatch()) {
                    result++;
                }
            }
        }
        return result;
    }

    public List<RecordedExchange> findMatchingExchanges(List<RequestPattern> patterns) {
        List<RecordedExchange> result = new ArrayList<>();
        for (RequestPattern requestPattern : patterns) {
            for (RecordedExchange recording : this.recordings) {
                if (requestPattern.match(recording.getRequest()).isExactMatch()) {
                    result.add(new RecordedExchange(recording));
                }
            }
        }
        return result;
    }

    public RecordedResponse recordedResponseOf(Response value) {
        RecordedResponse response = new RecordedResponse();
        response.setDate(new Date());
        response.setStatus(value.getStatus());
        response.setBase64Body(base64Body(value.getBody(), value.getHeaders()));
        response.setHeaders(value.getHeaders());
        response.getHeaders().plus(HttpHeader.httpHeader(HeaderName.ofTheResponseCode(), value.getStatus() + ""));
        return response;
    }


    private String base64Body(byte[] body, HttpHeaders contentEncoding) {
        body = body == null ? new byte[0] : body;
        if ("gzip".equals(contentEncoding.getHeader("Content-Encoding")) || Gzip.isGzipped(body)) {
            body = Gzip.unGzip(body);
        }
        return Base64.getEncoder().encodeToString(body);
    }

    private RecordedRequest buildRecordedRequest(Request inputRequest) {
        RecordedRequest request = new RecordedRequest();
        request.setDate(new Date());
        request.setHeaders(inputRequest.getHeaders());
        request.setBase64Body(base64Body(inputRequest.getBody(), inputRequest.getHeaders()));
        request.setMethod(inputRequest.getMethod());
        request.setClientIp(inputRequest.getClientIp());
        request.setCookies(inputRequest.getCookies());
        request.setPath(inputRequest.getUrl());
        request.setAbsoluteUrl(inputRequest.getAbsoluteUrl());
        return request;
    }


    public void clearScope(Pattern scopePathPattern) {
        Iterator<RecordedExchange> iterator1 = recordings.iterator();
        while (iterator1.hasNext()) {
            if (matches(scopePathPattern, iterator1.next().getRequest())) {
                iterator1.remove();
            }
        }
        Iterator<Map.Entry<String, Collection<RecordedExchange>>> iterator2 = this.exchangesByStep.entrySet().iterator();
        while (iterator2.hasNext()) {
            if (scopePathPattern.matcher(iterator2.next().getKey()).find()) {
                iterator2.remove();
            }
        }
    }

    public void reset() {
        this.recordings.clear();
        this.exchangesByStep.clear();
    }

    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        List<RecordedExchange> result = new ArrayList<>();
        for (RecordedExchange recording : findActiveExchangesInStep(scopePath, stepName)) {
            if (recording.isRootExchange()) {
                result.add(recording);
            }
        }
        return result;
    }

    public void responseReceived(RecordedExchange exchange, Response response) {
        exchange.recordResponse(recordedResponseOf(response));
    }
}
