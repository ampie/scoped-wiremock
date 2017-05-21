package com.github.ampie.wiremock;


import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.servlet.WireMockHttpServletRequestAdapter;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class WhenTransformingProxiedResponses {
    @Test
    public void shouldOnlyUseLast3SegmentsAndBeBackwardCompatible() throws Exception {
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/ignore/ignore/here/test/uri");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("useLeadingSegments", false);
        parameters.put("segmentsToUse", 3);
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        class ProxyResponseRenderer{
            String getUrl(){
                return request.getUrl();
            }
        }
        assertThat(new ProxyResponseRenderer().getUrl(),is(equalTo("/here/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/ignore/ignore/here/test/uri")));
    }
    @Test
    public void shouldOnlyUseLast3Segments() throws Exception {
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/ignore/ignore/here/test/uri");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("useLeadingSegments", false);
        parameters.put("numberOfSegments", 3);
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        class ProxyResponseRenderer{
            String getUrl(){
                return request.getUrl();
            }
        }
        assertThat(new ProxyResponseRenderer().getUrl(),is(equalTo("/here/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/ignore/ignore/here/test/uri")));
    }
    @Test
    public void shouldOnlyUseFirst2Segments() throws Exception {
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/test/uri/ignore/ignore/ignore");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("useLeadingSegments", true);
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        class ProxyResponseRenderer{
            String getUrl(){
                return request.getUrl();
            }
        }
        assertThat(new ProxyResponseRenderer().getUrl(),is(equalTo("/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/test/uri/ignore/ignore/ignore")));
    }
    @Test
    public void shouldIgnoreLast3Segments() throws Exception {
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/here/test/uri/ignore/ignore/ignore");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("ignoreTrailingSegments", true);
        parameters.put("numberOfSegments", 3);
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        class ProxyResponseRenderer{
            String getUrl(){
                return request.getUrl();
            }
        }
        assertThat(new ProxyResponseRenderer().getUrl(),is(equalTo("/here/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/here/test/uri/ignore/ignore/ignore")));
    }
    @Test
    public void shouldIgnoreFirst2Segments() throws Exception {
        ProxyUrlTransformer proxyUrlTransformer = new ProxyUrlTransformer();
        HttpServletRequest mock = Mockito.mock(HttpServletRequest.class);
        when(mock.getRequestURI()).thenReturn("/ignore/ignore/test/uri");
        final WireMockHttpServletRequestAdapter request = new WireMockHttpServletRequestAdapter(mock);
        Parameters parameters = new Parameters();
        parameters.put("ignoreLeadingSegments", true);
        ResponseDefinition responseDefinition = aResponse().proxiedFrom("http://proxy.url:9090/asdf/asdf/asfd").build();
        responseDefinition.setOriginalRequest(request);
        ResponseDefinition result = proxyUrlTransformer.transform(request, responseDefinition, null, parameters);
        class ProxyResponseRenderer{
            String getUrl(){
                return request.getUrl();
            }
        }
        assertThat(new ProxyResponseRenderer().getUrl(),is(equalTo("/test/uri")));
        assertThat(request.getUrl(),is(equalTo("/ignore/ignore/test/uri")));
    }
}
