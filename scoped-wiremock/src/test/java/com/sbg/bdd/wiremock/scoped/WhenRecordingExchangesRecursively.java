package com.sbg.bdd.wiremock.scoped;


import com.github.tomakehurst.wiremock.common.HttpClientUtils;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class WhenRecordingExchangesRecursively extends ScopedWireMockTest {

    @Test
    public void shouldOnlyRetrieveRootExchangesAgainstStep() throws IOException {
        //given
        CorrelationState scope = getWireMock().joinCorrelatedScope("scope");
        getWireMock().startStep(scope.getCorrelationPath(), "step1");
        getWireMock().register(matching("scope.*"), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(getWireMock().baseUrl()+"/proxied")).atPriority(1));
        getWireMock().register(matching("scope.*"), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1));
        assertThat(sendGet("/entry_point", "scope"),is(equalTo("hello")));
        getWireMock().stopStep("scope", "step1");
        assertThat(sendGet("/entry_point", "scope"),is(equalTo("hello")));
        //When
        List<RecordedExchange> exchangesAgainstStep = getWireMock().findExchangesAgainstStep(scope.getCorrelationPath(), "step1");
        List<RecordedExchange> exchangesAgainstScope = getWireMock().findMatchingExchanges(matching("scope.*"),new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build());
        //Then
        assertThat(exchangesAgainstScope.size(), is(equalTo(4)));
        assertThat(exchangesAgainstStep.size(), is(equalTo(1)));
        assertThat(exchangesAgainstStep.get(0).getNestedExchanges().size(), is(equalTo(1)));
    }

    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + getWireMockPort() + path);
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath);
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get);
        return HttpClientUtils.getEntityAsStringAndCloseStream(response);
    }
}
