package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.junit.WireMockRuleConfiguration;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WhenReceivinScopedRequests {
    @Test
    public void itShouldUpdateTheCurrentScopedStateIfReceivedFromADownstreamResponse() throws Exception {
        //Given:

        ScopedWireMockServer server = new ScopedWireMockServer(WireMockRuleConfiguration.DYNAMIC_PORT);
        CorrelationState path123 = new CorrelationState("path123");
        path123.getServiceInvocationCounts().put("http://test.com:8080/this?queryPartm=123", 3);
        server.joinKnownCorrelatedScope(path123);
        server.syncCorrelatedScope(path123);
        HttpHeaders headers = new HttpHeaders()
                .plus(new HttpHeader(HeaderName.ofTheCorrelationKey(), path123.getCorrelationPath()))
                .plus(new HttpHeader(HeaderName.ofTheServiceInvocationCount(), "http://test.com:8080/this?queryPartm=123|4"));

        Response response = Mockito.mock(Response.class);
        Mockito.when(response.getHeaders()).thenReturn(headers);
        //When:
        new ScopeUpdatingResponseTransformer().transform(Mockito.mock(Request.class), response, Mockito.mock(FileSource.class), new Parameters());
        //Then:
        Integer actualCount = server.getCorrelatedScope(path123.getCorrelationPath()).getServiceInvocationCounts().get("http://test.com:8080/this?queryPartm=123");
        assertThat(actualCount, is(4));
    }
    @Test
    public void itShouldUpdateTheCurrentScopedStateIfReceivedFromAnUpstreamRequestOnlyIfNoneWasReceivedFromTheResponse() throws Exception {
        //Given:

        ScopedWireMockServer server = new ScopedWireMockServer(WireMockRuleConfiguration.DYNAMIC_PORT);
        CorrelationState path123 = new CorrelationState("path123");
        path123.getServiceInvocationCounts().put("http://test.com:8080/this?queryPartm=123", 3);
        server.joinKnownCorrelatedScope(path123);
        server.syncCorrelatedScope(path123);
        HttpHeaders headers = new HttpHeaders()
                .plus(new HttpHeader(HeaderName.ofTheCorrelationKey(), path123.getCorrelationPath()))
                .plus(new HttpHeader(HeaderName.ofTheServiceInvocationCount(), "http://test.com:8080/this?queryPartm=123|4"));

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.getHeaders()).thenReturn(headers);
        Response response = Mockito.mock(Response.class);
        Mockito.when(response.getHeaders()).thenReturn(new HttpHeaders());
        //When:
        new ScopeUpdatingResponseTransformer().transform(request, response, Mockito.mock(FileSource.class), new Parameters());
        //Then:
        Integer actualCount = server.getCorrelatedScope(path123.getCorrelationPath()).getServiceInvocationCounts().get("http://test.com:8080/this?queryPartm=123");
        assertThat(actualCount, is(4));
    }
}
