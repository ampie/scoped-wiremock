package com.sbg.bdd.wiremock.scoped.server

import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.servlet.WireMockHttpServletRequestAdapter
import org.mockito.Mockito
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.is
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.when
//TODO may want to refactor the WireMockApp to override the ProxyResponseRenderer
class WhenTransformingProxiedResponses extends Specification{
    //Dummy class that meets the conditions for the ProxyUrlTransformer to think it is being called from the WireMock ProxyResponseRenderer
    class ProxyResponseRenderer{
        WireMockHttpServletRequestAdapter request

        ProxyResponseRenderer(WireMockHttpServletRequestAdapter request) {
            this.request = request
        }

        String getUrl(){
            return request.getUrl();
        }
    }

    def 'shouldOnlyUseLast3Segments'(){
        given:
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/ignore/ignore/here/test/uri");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("numberOfSegments", 3);
        parameters.put("action", "use");
        parameters.put("which", "trailing");
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        when:
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        then:

        assertThat(new ProxyResponseRenderer(request).getUrl(),is(equalTo("/here/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/ignore/ignore/here/test/uri")));
    }
    def 'shouldOnlyUseFirst2Segments'(){
        given:
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/test/uri/ignore/ignore/ignore");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("action", "use");
        parameters.put("which", "leading");
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        when:
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        then:
        assertThat(new ProxyResponseRenderer(request).getUrl(),is(equalTo("/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/test/uri/ignore/ignore/ignore")));
    }
    def 'shouldIgnoreLast3Segments'(){
        given:
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/here/test/uri/ignore/ignore/ignore");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("numberOfSegments", 3);
        parameters.put("action", "ignore");
        parameters.put("which", "trailing");

        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        when:
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        then:
        assertThat(new ProxyResponseRenderer(request).getUrl(),is(equalTo("/here/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/here/test/uri/ignore/ignore/ignore")));
    }
    def 'shouldIgnoreFirst2Segments'(){
        given:
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/ignore/ignore/test/uri");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("action", "ignore");
        parameters.put("which", "leading");
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        when:
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        then:
        assertThat(new ProxyResponseRenderer(request).getUrl(),is(equalTo("/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/ignore/ignore/test/uri")));
    }

}
