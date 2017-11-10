package com.sbg.bdd.wiremock.scoped.spring;

import com.sbg.bdd.wiremock.scoped.integration.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutboundCorrelationKeyInterceptor implements ClientHttpRequestInterceptor {
    Logger LOGGER = Logger.getLogger(OutboundCorrelationKeyInterceptor.class.getName());


    @Override
    public ClientHttpResponse intercept(HttpRequest ctx, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        RuntimeCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            URL originalHost = null;
            if(ctx.getHeaders().containsKey(HeaderName.ofTheOriginalUrl())) {
                originalHost =new URL(ctx.getHeaders().getFirst(HeaderName.ofTheOriginalUrl()));
                ctx.getHeaders().remove(HeaderName.ofTheOriginalUrl());
            }else{
                originalHost=URLHelper.hostOnly(ctx.getURI().toURL());
            }
            URL originalUrl = URLHelper.calculateOriginalUrl(ctx.getURI().toURL(),originalHost);
            String key = URLHelper.identifier(originalUrl,ctx.getMethod().name());
            ctx.getHeaders().add(HeaderName.ofTheOriginalUrl(), originalUrl.toExternalForm());
            ctx.getHeaders().add(HeaderName.ofTheThreadContextId(), currentCorrelationState.getCurrentThreadContextId() + "");
            ctx.getHeaders().add(HeaderName.ofTheCorrelationKey(), currentCorrelationState.getCorrelationPath());
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                ctx.getHeaders().add(HeaderName.toProxyUnmappedEndpoints(), "true");
            }
            if(RuntimeCorrelationState.ON) {
                String sequenceNumber = currentCorrelationState.getNextSequenceNumberFor(key).toString();
                ctx.getHeaders().add(HeaderName.ofTheSequenceNumber(), sequenceNumber);
                for (ServiceInvocationCount entry : currentCorrelationState.getServiceInvocationCounts()) {
                    ctx.getHeaders().add(HeaderName.ofTheServiceInvocationCount(), entry.toString());
                }
            }
        }
        //TODO experiment and see if we can change the URL here to point to WireMock. THen we can include the original URL in the header
        ClientHttpResponse response = clientHttpRequestExecution.execute(ctx, bytes);
        try {
            if (currentCorrelationState.isSet()) {
                HttpHeaders headers = response.getHeaders();
                if (headers != null && headers.containsKey(HeaderName.ofTheServiceInvocationCount())) {
                    Iterable<String> o = headers.get(HeaderName.ofTheServiceInvocationCount());
                    for (String s : o) {
                        currentCorrelationState.initSequenceNumberFor(new ServiceInvocationCount(s));
                    }
                }
            }
        } catch (Exception e) {
            //Make no assumptions
            LOGGER.log(Level.WARNING, "Could not process response", e);
        }
        return response;
    }
}