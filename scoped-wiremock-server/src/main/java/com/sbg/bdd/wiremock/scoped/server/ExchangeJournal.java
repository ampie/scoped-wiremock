package com.sbg.bdd.wiremock.scoped.server;



import com.github.tomakehurst.wiremock.common.Gzip;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedRequest;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedResponse;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.server.ScopePathMatcher.matches;

//NB!! necessary because the normal InMemoryRequestJournal only stores the request with the responseDefinition, not the actual response
public class ExchangeJournal  {
    private ConcurrentLinkedQueue<RecordedExchange> recordings = new ConcurrentLinkedQueue<>();
    private Map<String, RecordedExchange> exchangesInProgress = new HashMap<>();

    public RecordedExchange requestReceived(String scopePath, String step, Request request) {
        RecordedExchange exchange=new RecordedExchange(buildRecordedRequest(request), scopePath, step);
        //Scopes are single threaded:
        RecordedExchange activeExchange = exchangesInProgress.get(contextKey(exchange.getStepContainerPath(), step));
        if(activeExchange ==null) {
            exchangesInProgress.put(contextKey(exchange.getStepContainerPath(), step), exchange);
            exchange.setRootExchange(true);
        }else{
            activeExchange.getInnerMostActiveExchange().recordNestedExchange(exchange);
        }
        this.recordings.add(exchange);
        return exchange;
    }

    private String contextKey(String scopePath, String step) {
        return scopePath + "/" + step;
    }

    public void responseReceived(RecordedExchange exchange, Response response) {
        String key = contextKey(exchange.getStepContainerPath(), exchange.getStep());
        RecordedExchange activeExchange = exchangesInProgress.get(key);
        activeExchange.getInnerMostActiveExchange().recordResponse(recordResponse(response));
        if(activeExchange.getResponse()!=null){
            exchangesInProgress.remove(key);
        }
    }

    public List<RecordedExchange> findMatchingExchanges(RequestPattern pattern) {
        List<RecordedExchange> result = new ArrayList<>();
        for (RecordedExchange recording : this.recordings) {
            if(pattern.match(recording.getRequest()).isExactMatch()){
                result.add(new RecordedExchange(recording));
            }
        }
        return result;
    }

    private RecordedResponse recordResponse(Response value) {
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
        request.setRequestedUrl(inputRequest.getUrl());
        request.setAbsoluteUrl(inputRequest.getAbsoluteUrl());
        String sequenceNumber = inputRequest.getHeader(HeaderName.ofTheSequenceNumber());
        if (sequenceNumber != null) {
            request.setSequenceNumber(Integer.parseInt(sequenceNumber));
        }
        return request;
    }


    public void clearScope(Pattern scopePathPattern) {
        Iterator<RecordedExchange> iterator = recordings.iterator();
        while(iterator.hasNext()){
            if(matches(scopePathPattern,iterator.next().getRequest())){
                iterator.remove();
            }
        }
    }

    public void reset() {
        this.recordings.clear();
    }

    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        List<RecordedExchange> result = new ArrayList<>();
        for (RecordedExchange recording : this.recordings) {
            if(include(scopePath, stepName, recording)){
                result.add(recording);
            }
        }
        return result;

    }

    public boolean include(String scopePath, String stepName, RecordedExchange entry) {
        return entry.isRootExchange() &&
                scopePath.equals(entry.getStepContainerPath()) &&
                entry.getStep() != null &&
                entry.getStep().equals(stepName);
    }

}
