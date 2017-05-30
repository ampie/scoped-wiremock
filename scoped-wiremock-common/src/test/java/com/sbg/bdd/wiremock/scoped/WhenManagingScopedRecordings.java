package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.common.HttpClientUtils;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
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

public abstract class WhenManagingScopedRecordings extends ScopedWireMockTest {

    @Test
    public void shouldRecordAnExchangeAgainstTheScopeItOccurredIn() throws IOException {
        //given
        CorrelationState scope1 = getWireMock().joinCorrelatedScope("/scope1");
        CorrelationState scope2 = getWireMock().joinCorrelatedScope("/scope2");
        getWireMock().register(matching("/scope1.*"), get(urlEqualTo("/test/uri1")).willReturn(aResponse().withBody("hello1")).atPriority(1));
        getWireMock().register(matching("/scope2.*"), get(urlEqualTo("/test/uri2")).willReturn(aResponse().withBody("hello2")).atPriority(1));
        assertThat(sendGet("/test/uri1", "/scope1"), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri1", "/scope1"), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri2", "/scope2"), is(equalTo("hello2")));
        //When
        List<RecordedExchange> exchangesAgainstScope1 = getWireMock().findMatchingExchanges(matching("/scope1"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        List<RecordedExchange> exchangesAgainstScope2 = getWireMock().findMatchingExchanges(matching("/scope2"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri2")).build());
        //Then
        assertThat(exchangesAgainstScope1.size(), is(equalTo(2)));
        assertThat(exchangesAgainstScope2.size(), is(equalTo(1)));
    }

    @Test
    public void shouldDiscardAnExchangeWhenTheScopeItOccurredInStops() throws IOException {
        //Given
        CorrelationState scope1 = getWireMock().joinCorrelatedScope("/scope1");
        CorrelationState scope2 = getWireMock().joinCorrelatedScope("/scope2");
        getWireMock().register(matching("/scope1.*"), get(urlEqualTo("/test/uri1")).willReturn(aResponse().withBody("hello1")).atPriority(1));
        getWireMock().register(matching("/scope2.*"), get(urlEqualTo("/test/uri2")).willReturn(aResponse().withBody("hello2")).atPriority(1));
        assertThat(sendGet("/test/uri1", "/scope1"), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri1", "/scope1"), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri2", "/scope2"), is(equalTo("hello2")));
        List<RecordedExchange> exchangesAgainstScope1 = getWireMock().findMatchingExchanges(matching("/scope1.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstScope1.size(), is(equalTo(2)));
        List<RecordedExchange> exchangesAgainstScope2 = getWireMock().findMatchingExchanges(matching("/scope2.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri2")).build());
        assertThat(exchangesAgainstScope2.size(), is(equalTo(1)));
        //When
        getWireMock().stopCorrelatedScope("/scope1");
        //Then
        exchangesAgainstScope1 = getWireMock().findMatchingExchanges( matching("/scope1.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstScope1.size(), is(equalTo(0)));
        exchangesAgainstScope2 = getWireMock().findMatchingExchanges(matching("/scope2.*"),new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri2")).build());
        assertThat(exchangesAgainstScope2.size(), is(equalTo(1)));
    }

    @Test
    public void shouldDiscardAnExchangeWhenTheParentOfTheScopeItOccurredInStops() throws IOException {
        //Given
        CorrelationState parentScope = getWireMock().joinCorrelatedScope("/root_scope");
        CorrelationState nestedScope = getWireMock().startNewCorrelatedScope(parentScope.getCorrelationPath());
        String nestedCorrelationPathPattern = nestedScope.getCorrelationPath() + ".*";
        getWireMock().register(get(urlEqualTo("/test/uri1")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/root_scope.*")).willReturn(aResponse().withBody("hello1")).atPriority(1));
        assertThat(sendGet("/test/uri1",nestedScope.getCorrelationPath()), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri1", nestedScope.getCorrelationPath()), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri1", parentScope.getCorrelationPath()), is(equalTo("hello1")));
        List<RecordedExchange> exchangesAgainstNestedScope = getWireMock().findMatchingExchanges(matching(nestedCorrelationPathPattern), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstNestedScope.size(), is(equalTo(2)));
        List<RecordedExchange> exchangesAgainstParentScope = getWireMock().findMatchingExchanges(matching("/root_scope.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstParentScope.size(), is(equalTo(3)));
        //When
        getWireMock().stopCorrelatedScope(parentScope.getCorrelationPath());
        //Then
        exchangesAgainstNestedScope = getWireMock().findMatchingExchanges(matching(nestedCorrelationPathPattern), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstNestedScope.size(), is(equalTo(0)));
        exchangesAgainstParentScope = getWireMock().findMatchingExchanges(matching("/root_scope.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstParentScope.size(), is(equalTo(0)));
    }

    @Test
    public void shouldDiscardExchangesFromNestedScopeWhenItStops() throws IOException {
        //Given
        CorrelationState parentScope = getWireMock().joinCorrelatedScope("/root_scope");
        CorrelationState nestedScope = getWireMock().startNewCorrelatedScope(parentScope.getCorrelationPath());
        String nestedCorrelationPathPattern = nestedScope.getCorrelationPath() + ".*";
        getWireMock().register(get(urlEqualTo("/test/uri1")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/root_scope.*")).willReturn(aResponse().withBody("hello1")).atPriority(1));
        assertThat(sendGet("/test/uri1",nestedScope.getCorrelationPath()), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri1", nestedScope.getCorrelationPath()), is(equalTo("hello1")));
        assertThat(sendGet("/test/uri1", parentScope.getCorrelationPath()), is(equalTo("hello1")));
        List<RecordedExchange> exchangesAgainstNestedScope = getWireMock().findMatchingExchanges(matching(nestedCorrelationPathPattern), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstNestedScope.size(), is(equalTo(2)));
        List<RecordedExchange> exchangesAgainstParentScope = getWireMock().findMatchingExchanges(matching("/root_scope.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstParentScope.size(), is(equalTo(3)));
        //When
        getWireMock().stopCorrelatedScope(nestedScope.getCorrelationPath());
        exchangesAgainstNestedScope = getWireMock().findMatchingExchanges(matching(nestedCorrelationPathPattern), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstNestedScope.size(), is(equalTo(0)));
        exchangesAgainstParentScope = getWireMock().findMatchingExchanges(matching("/root_scope.*"), new RequestPatternBuilder(RequestMethod.GET, urlEqualTo("/test/uri1")).build());
        assertThat(exchangesAgainstParentScope.size(), is(equalTo(1)));
    }

    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + getWireMockPort() + path);
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath);
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get);
        return HttpClientUtils.getEntityAsStringAndCloseStream(response);
    }


}
