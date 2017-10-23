package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.http.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScopedRequest implements Request {
    private HttpHeaders headers;
    Request delegate;

    public ScopedRequest(Request delegate) {
        this.delegate = delegate;
        this.headers = delegate.getHeaders();
    }
    public void putHeader(String key, List<String> values){
        headers=headers.plus(new HttpHeader(key,values));
    }

    @Override
    public String getUrl() {
        return delegate.getUrl();
    }

    @Override
    public String getAbsoluteUrl() {
        return delegate.getAbsoluteUrl();
    }

    @Override
    public RequestMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getClientIp() {
        return delegate.getClientIp();
    }

    @Override
    public String getHeader(String key) {
        HttpHeader header = headers.getHeader(key);
        if(header.isPresent()){
            return header.firstValue();
        }else{
            return null;
        }
    }

    @Override
    public HttpHeader header(String key) {
        return headers.getHeader(key);
    }

    @Override
    public ContentTypeHeader contentTypeHeader() {
        return headers.getContentTypeHeader();
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Override
    public boolean containsHeader(String key) {
        return headers.getHeader(key).isPresent();
    }

    @Override
    public Set<String> getAllHeaderKeys() {
        return headers.keys();
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public QueryParameter queryParameter(String key) {
        return delegate.queryParameter(key);
    }

    @Override
    public byte[] getBody() {
        return delegate.getBody();
    }

    @Override
    public String getBodyAsString() {
        return delegate.getBodyAsString();
    }

    @Override
    public String getBodyAsBase64() {
        return delegate.getBodyAsBase64();
    }

    @Override
    public boolean isBrowserProxyRequest() {
        return delegate.isBrowserProxyRequest();
    }
}
